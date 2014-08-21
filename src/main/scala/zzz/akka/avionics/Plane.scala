package zzz.akka.avionics

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import Pilots._

object Plane {

  case object GiveMeControl

  case class Controls(controls: ActorRef)

  private val configPrefix = "zzz.akka.avionics.flightcrew"
}

class Plane extends Actor with ActorLogging {

  this: AltimeterProvider
  with PilotProvider
  with LeadFlightAttendantProvider =>

  import Altimeter._
  import Plane._
  import EventSource._

  private val altimeter = context.actorOf(Props(newAltimeter), "Altimeter")
  private val controls: ActorRef = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")

  private val config = context.system.settings.config
  private val pilot = context.actorOf(Props(newPilot), config.getString(s"$configPrefix.pilotName"))
  private val copilot = context.actorOf(Props(newCopilot), config.getString(s"$configPrefix.copilotName"))
  private val autopilot = context.actorOf(Props(newAutopilot), "Autopilot")
  private val flightAttendant = context.actorOf(Props(newLeadFlightAttendant), config.getString(s"$configPrefix.leadAttendantName"))

  override def preStart(): Unit = {
    altimeter ! RegisterListener(self)
    List(pilot, copilot).foreach(_ ! ReadyToGo)
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control.")
      sender ! Controls(controls)

    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }

}
