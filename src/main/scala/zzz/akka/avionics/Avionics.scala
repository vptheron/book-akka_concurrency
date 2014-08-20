package zzz.akka.avionics

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Avionics {

  import Plane._

  def main(args: Array[String]): Unit ={
    implicit val timeout = Timeout(5.seconds)

    val system = ActorSystem("PlaneSimulation")
    val plane = system.actorOf(Props[Plane], "Plane")

    val control = Await.result((plane ? GiveMeControl).mapTo[Controls], 5.seconds).controls

    system.scheduler.scheduleOnce(200.millis){
      control ! ControlSurfaces.StickBack(1f)
    }
    system.scheduler.scheduleOnce(1.second){
      control ! ControlSurfaces.StickBack(0f)
    }
    system.scheduler.scheduleOnce(3.seconds){
      control ! ControlSurfaces.StickBack(0.5f)
    }
    system.scheduler.scheduleOnce(4.seconds){
      control ! ControlSurfaces.StickBack(0f)
    }
    system.scheduler.scheduleOnce(5.seconds){
      system.shutdown()
    }
  }
}
