package zzz.akka.avionics

import akka.actor.{ActorRef, Actor}

object EventSource {

  case class RegisterListener(listener: ActorRef)

  case class UnregisterListener(listener: ActorRef)

}

trait EventSource {

  def sendEvent[T](event: T): Unit

  def eventSourceReceive: Actor.Receive

}
