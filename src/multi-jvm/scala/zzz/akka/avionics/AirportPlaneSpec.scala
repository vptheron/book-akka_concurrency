package zzz.akka.avionics

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe, ImplicitSender}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}
import org.scalatest.Matchers
import scala.concurrent.duration._

class AirportPlaneMultiJvmNode1
  extends TestKit(ActorSystem("AirportSpec", RemoteConfig.airportConfig))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  val receiver = system.actorOf(Props(new Actor {
    def receive: Receive = {
      case m => testActor forward m
    }
  }), "testReceiver")

  println("THE PATH:  " +receiver.path)

  override def afterAll(): Unit = {
    system.shutdown()
  }

  "AirportPlaneSpec" should {
    "start up" in {
      val toronto = system.actorOf(Airport.toronto, "toronto")
//      expectMsg("stopAirport")
    }
  }

}

class AirportPlaneMultiJvmNode2
  extends TestKit(ActorSystem("PlaneSpec", RemoteConfig.planeConfig))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  import Airport._
  import FlyingBehaviour._
  import RemoteConfig._

  override def afterAll(): Unit ={
    system.shutdown()
  }

  def remoteTestReceiver(): ActorRef =
    system.actorFor("akka.tcp://AirportSpec@" + s"$airportHost:$airportPort/user/testReceiver")

  def toronto(): ActorRef =
    system.actorFor("akka.tcp://AirportSpec@" + s"$airportHost:$airportPort/user/toronto")

  def actorForAirport: Boolean = !toronto().isTerminated

  def actorForReceiver: Boolean = !remoteTestReceiver().isTerminated

  "AirportPlaneSpec" should {
    "get flying instructions from toronto" in {
      awaitCond(actorForAirport && actorForReceiver, 3.seconds)
//      val to = toronto()

//      to ! DirectFlyerToAirport(testActor)

//      expectMsgPF(){
//        case Fly(CourseTarget(altitude, heading, when)) =>
//          altitude should be > (1000.0)
//          heading should be (314.3f)
//          when should be > (0L)
//      }

      remoteTestReceiver() ! "stopAirport"
    }
  }

}
