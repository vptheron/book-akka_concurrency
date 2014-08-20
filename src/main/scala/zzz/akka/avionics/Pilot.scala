package zzz.akka.avionics

import akka.actor.{ActorSelection, ActorRef, Actor}

object Pilot {

  case object ReadyToGo

  case object RelinquishControl

}

class Pilot extends Actor {

  import Pilot._
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
