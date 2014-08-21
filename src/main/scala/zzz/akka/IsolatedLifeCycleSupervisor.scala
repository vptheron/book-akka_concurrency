package zzz.akka

import akka.actor.Actor

object IsolatedLifeCycleSupervisor {

  case object WaitForStart

  case object Started

}

trait IsolatedLifeCycleSupervisor extends Actor {

  import IsolatedLifeCycleSupervisor._

  def receive = {
    case WaitForStart => sender ! Started

    case m => throw new Exception(s"Don't call ${self.path.name} directly ($m).")
  }

  def childStarter(): Unit

  final override def preStart(): Unit ={
    childStarter()
  }

  final override def preRestart(reason: Throwable, message: Option[Any]){}

  final override def postRestart(reason: Throwable){}

}
