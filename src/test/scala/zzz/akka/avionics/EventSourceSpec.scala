package zzz.akka.avionics

import akka.actor.{ActorSystem, Actor}
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}
import zzz.akka.avionics.ProductionEventSource.{RegisterListener, UnregisterListener}

object EventSourceSpec {

  class TestEventSource extends Actor with ProductionEventSource {
    def receive = eventSourceReceive
  }

}

class EventSourceSpec
  extends TestKit(ActorSystem("EventSourceSpec"))
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  import EventSourceSpec._

  "EventSource" should {

    "allow us to register a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain(testActor)
    }

    "allow us to unregister a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners should contain(testActor)
      real.receive(UnregisterListener(testActor))
      real.listeners.size should be(0)
    }

    "send the event to our test actor" in {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibonacci")
      expectMsg("Fibonacci")
    }

  }

  override def afterAll(): Unit = {
    system.shutdown()
  }

}