package zzz.akka.avionics

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import Pilots._
import zzz.akka.IsolatedLifeCycleSupervisor.WaitForStart
import zzz.akka.{IsolatedStopSupervisor, OneForOneStrategyFactory, IsolatedResumeSupervisor}

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

  private val config = context.system.settings.config
  private val pilotName = config.getString(s"$configPrefix.pilotName")
  private val copilotName = config.getString(s"$configPrefix.copilotName")

  private implicit val askTimeout = Timeout(1.second)

  private def startEquipment(): Unit = {
    val equipment = context.actorOf(Props(
      new IsolatedResumeSupervisor() with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          context.actorOf(Props(newAutopilot), "Autopilot")
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
      }
    ), "Equipment")
    Await.result(equipment ? WaitForStart, 1.second)
  }

  private def actorForControls(name: String) =
    context.actorFor("Equipment/" + name)

  private def startPeople(): Unit = {
    val plane = self

    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")

    val people = context.actorOf(Props(
      new IsolatedStopSupervisor() with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter)), copilotName)
        }
      }
    ), "Pilots")
    context.actorOf(Props(newLeadFlightAttendant), config.getString(s"$configPrefix.leadAttendantName"))
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
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control.")
//      sender ! Controls(controls)

    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }

}
