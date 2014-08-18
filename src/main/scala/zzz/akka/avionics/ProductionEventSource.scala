package zzz.akka.avionics

import akka.actor.{Actor, ActorRef}

object ProductionEventSource {

  case class RegisterListener(listener: ActorRef)

  case class UnregisterListener(listener: ActorRef)

}

trait ProductionEventSource extends EventSource {
  this: Actor =>

  import ProductionEventSource._

  var listeners = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = listeners.foreach {
    _ ! event
  }

  def eventSourceReceive: Receive = {
    case RegisterListener(listener) => listeners = listeners :+ listener
    case UnregisterListener(listener) => listeners = listeners.filter(_ != listener)
  }
}
