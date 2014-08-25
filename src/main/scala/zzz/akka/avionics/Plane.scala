package zzz.akka.avionics

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import Pilots._
import zzz.akka.IsolatedLifeCycleSupervisor.WaitForStart
import zzz.akka.{IsolatedStopSupervisor, OneForOneStrategyFactory, IsolatedResumeSupervisor}

object Plane {

  case object GiveMeControl

  case class Controls(controls: ActorRef)

  case object TakeControl

  case object RequestCopilot

  case class CopilotReference(copilot: ActorRef)

  case object LostControl

  private val configPrefix = "zzz.akka.avionics.flightcrew"

  def apply() = new Plane with AltimeterProvider
    with HeadingIndicatorProvider
    with PilotProvider
    with FlightAttendantProvider
}

class Plane extends Actor with ActorLogging {

  this: AltimeterProvider
    with HeadingIndicatorProvider
    with PilotProvider
    with FlightAttendantProvider =>

  import Altimeter._
  import Plane._
  import EventSource._

  private val config = context.system.settings.config
  private val pilotName = config.getString(s"$configPrefix.pilotName")
  private val copilotName = config.getString(s"$configPrefix.copilotName")

  private implicit val askTimeout = Timeout(1.second)

  private def startEquipment(): Unit = {
    val plane = self

    val equipment = context.actorOf(Props(
      new IsolatedResumeSupervisor() with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          context.actorOf(Props(newAutopilot(plane)), "Autopilot")
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          val heading = context.actorOf(Props(newHeadingIndicator), "HeadingIndicator")
          context.actorOf(Props(new ControlSurfaces(plane, alt, heading)), "ControlSurfaces")
        }
      }
    ), "Equipment")
    Await.result(equipment ? WaitForStart, 1.second)
  }

  private def actorForControls(name: String) =
    context.actorFor("Equipment/" + name)

  private def startPeople(): Unit = {
    val plane = self

    val heading = actorForControls("HeadingIndicator")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")

    val leadAttendant = context.actorOf(
      Props(newFlightAttendant()).withRouter(FromConfig),
      "FlightAttendantRouter")

    val people = context.actorOf(Props(
      new IsolatedStopSupervisor() with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          context.actorOf(Props(newPilot(plane, autopilot, heading, altimeter)), pilotName)
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter)), copilotName)
          context.actorOf(Props(PassengerSupervisor(leadAttendant)), "Passengers")
        }
      }
    ), "Pilots")
    Await.result(people ? WaitForStart, 1.second)
  }

  private def actorForPilots(name: String) =
    context.actorFor("Pilots/" + name)

  override def preStart(): Unit = {
    startEquipment()
    startPeople()
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
    actorForControls("Autopilot") ! ReadyToGo
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control.")
      sender ! Controls(actorForControls("ControlSurfaces"))

    case LostControl => actorForControls("Autopilot") ! TakeControl

    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")

    case RequestCopilot =>
      sender ! CopilotReference(actorForPilots(copilotName))
  }

}
