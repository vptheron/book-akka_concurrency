package zzz.akka.avionics

import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}
import akka.actor._
import akka.routing.BroadcastGroup
import akka.util.Timeout
import akka.pattern.{ask, pipe}

import scala.concurrent.duration._

object PassengerSupervisor {

  case object GetPassengerBroadcaster

  case class PassengerBroadcaster(broadcaster: ActorRef)

  def apply(callButton: ActorRef) =
    new PassengerSupervisor(callButton) with PassengerProvider

}

class PassengerSupervisor(callButton: ActorRef) extends Actor {

  this: PassengerProvider =>

  import zzz.akka.avionics.PassengerSupervisor._

  private implicit val ec = context.dispatcher

  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  case object GetChildren

  override def preStart(): Unit = {
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config
      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => Escalate
        case _: ActorInitializationException => Escalate
        case _ => Stop
      }

      override def preStart(): Unit = {
        import com.typesafe.config.ConfigList

        import scala.collection.JavaConverters._

        val passengers = config.getList("zzz.akka.avionics.passengers")

        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList]
            .unwrapped().asScala.mkString("-")
            .replaceAllLiterally(" ", "-")
          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }

      def receive = {
        case GetChildren => sender ! context.children.toList
      }
    }), "PassengersSupervisor")
  }

  private implicit val askTimeout = Timeout(5.seconds)

  def noRouter: Receive = {

    case GetPassengerBroadcaster =>
      val destinedFor = sender()
      val actor = context.actorFor("PassengersSupervisor")
      (actor ? GetChildren).mapTo[Seq[ActorRef]] map { passengers =>
        val childrenPaths = passengers.toList.map(_.path.toStringWithoutAddress)
        (BroadcastGroup(childrenPaths).props(), destinedFor)
      } pipeTo self

    case (props: Props, destinedFor: ActorRef) =>
      val router = context.actorOf(props, "Passengers")
      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster => sender ! PassengerBroadcaster(router)
  }

  def receive = noRouter
}