package zzz.akka

import akka.actor.{Actor, ActorRef}

class MessageTransformer(from: ActorRef,
                          to: ActorRef,
                          transformer: PartialFunction[Any, Any]) extends Actor {

  import GenericPublisher._

  override def preStart(): Unit ={
    from ! RegisterListener(self)
  }

  override def postStop(): Unit ={
    from ! UnregisterListener(self)
  }

  def receive = {
    case m => to.forward(transformer(m))
  }

}
