package zzz.akka.avionics

import java.net.InetSocketAddress

import akka.actor.{Props, ActorSystem, Actor}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import zzz.akka.avionics.TelnetServerSpec.PlaneForTest

object TelnetServerSpec {

  class PlaneForTest extends Actor {

    import HeadingIndicator._
    import Altimeter._

    def receive = {
      case GetCurrentAltitude => sender() ! CurrentAltitude(52500f)
      case GetCurrentHeading => sender() ! CurrentHeading(233.4f)
    }
  }

}

class TelnetServerSpec
  extends TestKit(ActorSystem("TelnetServerSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers {

  "TelnetServer" should {
    "work" in {
      val p = system.actorOf(Props[PlaneForTest])
      val s = system.actorOf(Props(new TelnetServer(p)))

      IO(Tcp) ! Connect(new InetSocketAddress("localhost", 31733))
      expectMsgType[Connected]
      lastSender ! Register(testActor)
      expectMsgType[Received]

      lastSender ! Write(ByteString("heading"))
      expectMsgPF(){
        case Received(data) =>
          val result = TelnetServer.ascii(data)
          result should include("233.40 degrees")
      }

      lastSender ! Write(ByteString("altitude"))
      expectMsgPF(){
        case Received(data) =>
          val result = TelnetServer.ascii(data)
          result should include("52500.00 feet")

      }
      lastSender ! Close
    }
  }

}
