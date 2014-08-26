package zzz.akka.avionics.main

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import zzz.akka.avionics.{TelnetServer, ControlSurfaces, Plane}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Avionics {

  import zzz.akka.avionics.Plane._

  def main(args: Array[String]): Unit ={
    implicit val timeout = Timeout(5.seconds)

    val system = ActorSystem("PlaneSimulation")
    val plane = system.actorOf(Props(Plane()), "Plane")

    val server = system.actorOf(Props(new TelnetServer(plane)), "Telnet")

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
//    system.scheduler.scheduleOnce(5.seconds){
//      system.shutdown()
//    }
  }
}
