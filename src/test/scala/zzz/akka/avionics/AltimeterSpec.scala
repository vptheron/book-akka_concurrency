package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestLatch, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class AltimeterSpec
  extends TestKit(ActorSystem("AltimeterSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  import Altimeter._

  "Altimeter" should {

    "record rate of climb changes" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(1f))
      real.rateOfClimb should be(maxRateOfClimb)
    }

    "record negative rate of climb changes" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(-0.5f))
      real.rateOfClimb should be(-0.5 * maxRateOfClimb)
    }

    "keep rate of climb changes within bounds" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(2f))
      real.rateOfClimb should be(maxRateOfClimb)
    }

    "calculate altitude changes" in new Helper {
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)
      fishForMessage() {
        case AltitudeUpdate(altitude) if altitude == 0f => false
        case AltitudeUpdate(_) => true
      }
      system.stop(ref)
    }

    "not go above ceiling" in new Helper {
      val ref = TestActorRef[Altimeter](Props(Altimeter()))
      ref.underlyingActor.altitude = ceiling - 1
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)
      fishForMessage() {
        case AltitudeUpdate(altitude) if altitude == ceiling => true
        case AltitudeUpdate(altitude) => false
      }

      expectMsg(AltitudeUpdate(ceiling))
    }

    "send events" in new Helper {
      val (ref, _) = actor()
      Await.ready(EventSourceSpy.latch, 1.second)
      EventSourceSpy.latch.isOpen should be(true)
    }

  }

  class Helper {

    object EventSourceSpy {
      val latch = TestLatch(1)
    }

    trait EventSourceSpy extends EventSource {
      def sendEvent[T](event: T): Unit = {
        EventSourceSpy.latch.countDown()
      }

      def eventSourceReceive = Actor.emptyBehavior
    }

    def slicedAltimeter = new Altimeter with EventSourceSpy

    def actor(): (TestActorRef[Altimeter], Altimeter) = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }

  override def afterAll(): Unit = {
    system.shutdown()
  }
}
