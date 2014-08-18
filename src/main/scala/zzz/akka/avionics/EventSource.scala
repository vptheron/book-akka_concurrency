package zzz.akka.avionics

import akka.actor.Actor

trait EventSource {

  def sendEvent[T](event: T): Unit

  def eventSourceReceive: Actor.Receive

}
