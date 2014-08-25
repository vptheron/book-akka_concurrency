package zzz.akka.avionics

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{Matchers, WordSpecLike}
import zzz.akka.avionics.Passenger.FastenSeatbelts

import scala.concurrent.duration._

object PassengerSpec {

  trait TestDrinkRequestProbability extends DrinkRequestProbability {
    override val askThreshold = 0f
    override val requestMin = 0.millis
    override val requestUpper = 2.millis
  }

}

class PassengerSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with Matchers {

  import PassengerSpec._
  import akka.event.Logging.Info
  import akka.testkit.TestProbe

  var seatNumber = 9

  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(
      new Passenger(testActor) with TestDrinkRequestProbability),
      s"Pat_Metheny-$seatNumber-B")
  }

  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF(){
        case Info(_, _, m) => m.toString should include(" fastening seatbelt")
      }
    }
  }

}
