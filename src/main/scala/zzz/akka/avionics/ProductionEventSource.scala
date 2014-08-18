package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}
import zzz.akka.avionics.EventSource.{UnregisterListener, RegisterListener}

trait ProductionEventSource extends EventSource {
  this: Actor =>

  var listeners = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = listeners.foreach {
    _ ! event
  }

  def eventSourceReceive: Receive = {
    case RegisterListener(listener) => listeners = listeners :+ listener
    case UnregisterListener(listener) => listeners = listeners.filter(_ != listener)
  }
}
