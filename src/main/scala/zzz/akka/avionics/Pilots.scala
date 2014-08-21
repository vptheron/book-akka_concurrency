package zzz.akka.avionics

import akka.actor.{ActorRef, Actor}

object Pilots {

  case object ReadyToGo

  case object RelinquishControl

  class Pilot extends Actor {

    import Plane._

    private var controls: ActorRef = context.system.deadLetters
    private var copilot: ActorRef = context.system.deadLetters
    private var autopilot: ActorRef = context.system.deadLetters

    private val copilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

    def receive = {

      case ReadyToGo =>
        context.parent ! GiveMeControl
        copilot = context.actorFor("../"+copilotName)
        autopilot = context.actorFor("../Autopilot")

      case Controls(controlSurfaces) => controls = controlSurfaces

    }
  }

  class Copilot extends Actor {

    private var controls: ActorRef = context.system.deadLetters
    private var pilot: ActorRef = context.system.deadLetters
    private var autopilot: ActorRef = context.system.deadLetters

    private val pilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.pilotName")

    def receive = {
      case ReadyToGo =>
        pilot = context.actorFor("../"+pilotName)
        autopilot = context.actorFor("../Autopilot")
    }
  }

  class Autopilot extends Actor {

    def receive = {
      case _ =>
    }

  }

  trait PilotProvider {

    def newPilot: Actor = new Pilot

    def newCopilot: Actor = new Copilot

    def newAutopilot: Actor = new Autopilot
  }
}
