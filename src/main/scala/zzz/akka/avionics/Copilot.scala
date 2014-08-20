package zzz.akka.avionics

import akka.actor.{ActorRef, Actor}

class Copilot extends Actor {

  import Pilot._

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
