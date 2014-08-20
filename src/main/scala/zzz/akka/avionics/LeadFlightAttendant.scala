package zzz.akka.avionics

import akka.actor.{Props, ActorRef, Actor}

object LeadFlightAttendant {

  case object GetFlightAttendant

  case class Attendant(a: ActorRef)

  def apply() = new LeadFlightAttendant with AttendantCreationPolicy

}

class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>

  import LeadFlightAttendant._

  override def preStart(): Unit = {
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList("zzz.akka.avionics.flightcrew.attendantNames").asScala
    attendantNames
      .take(numberOfAttendants)
      .foreach(name => context.actorOf(Props(createAttendant), name))
  }

  private def randomAttendant(): ActorRef =
    context.children.take(scala.util.Random.nextInt(numberOfAttendants) + 1).last

  def receive = {
    case GetFlightAttendant => sender ! Attendant(randomAttendant())

    case m => randomAttendant() forward m
  }
}

trait AttendantCreationPolicy {

  val numberOfAttendants: Int = 8

  def createAttendant: Actor = FlightAttendant()

}

trait LeadFlightAttendantProvider {

  def newLeadFlightAttendant: Actor = LeadFlightAttendant()

}