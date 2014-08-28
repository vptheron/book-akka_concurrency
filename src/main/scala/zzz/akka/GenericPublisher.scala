package zzz.akka

import akka.actor.ActorRef

object GenericPublisher {

  case class RegisterListener(actor: ActorRef)
  case class UnregisterListener(actor: ActorRef)

}
