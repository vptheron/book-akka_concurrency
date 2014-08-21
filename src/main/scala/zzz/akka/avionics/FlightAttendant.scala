package zzz.akka.avionics

import akka.actor.{Cancellable, ActorRef, Actor}

object FlightAttendant {

  case class GetDrink(drinkName: String)

  case class Drink(drinkName: String)

  case class Assist(passenger: ActorRef)

  case object Busy_?

  case object Yes

  case object No

  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }

  private case class DeliverDrink(drink: Drink)

}

class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>

  import FlightAttendant._

  private implicit val ec = context.dispatcher

  private var pendingDelivery: Option[Cancellable] = None

  private def scheduleDelivery(drinkName: String): Cancellable =
    context.system.scheduler.scheduleOnce(responseDuration, self, DeliverDrink(Drink(drinkName)))

  private def assistInjuredPassenger: Receive = {
    case Assist(passenger) =>
      pendingDelivery.foreach(_.cancel())
      pendingDelivery = None
      passenger ! Drink("Magic Healing Potion")
  }

  private def handleDrinkRequests: Receive = {
    case GetDrink(drinkName) =>
      val passenger = sender()
      pendingDelivery = Some(scheduleDelivery(drinkName))
      context.become(assistInjuredPassenger orElse handleSpecificPerson(passenger))

    case Busy_? => sender ! No
  }

  private def handleSpecificPerson(person: ActorRef): Receive = {
    case GetDrink(drinkName) if sender == person =>
      pendingDelivery.foreach(_.cancel())
      pendingDelivery = Some(scheduleDelivery(drinkName))

    case DeliverDrink(drink) =>
      person ! drink
      pendingDelivery = None
      context.become(assistInjuredPassenger orElse handleDrinkRequests)

    case m: GetDrink => context.parent forward m

    case Busy_? => sender ! Yes
  }

  def receive = assistInjuredPassenger orElse handleDrinkRequests
}

trait AttendantResponsiveness {
  import scala.concurrent.duration._

  val maxResponseTimeMS: Int

  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis

}

