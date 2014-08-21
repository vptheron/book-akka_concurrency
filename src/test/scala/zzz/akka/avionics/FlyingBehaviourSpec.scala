package zzz.akka.avionics

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestFSMRef, TestProbe, TestKit, ImplicitSender}
import org.scalatest.{Matchers, WordSpecLike}

class FlyingBehaviourSpec
  extends TestKit(ActorSystem("FlyingBehaviourSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers {

  import FlyingBehaviour._
  import HeadingIndicator._
  import Altimeter._
  import Plane._

  def nilActor: ActorRef = TestProbe().ref

  def fsm(plane: ActorRef = nilActor,
          heading: ActorRef = nilActor,
          altimeter: ActorRef = nilActor) =
    TestFSMRef(new FlyingBehaviour(plane, heading, altimeter))

  "FlyingBehaviour" should {

    "start in idle state and with uninitialized data" in {
      val a = fsm()
      a.stateName should be(Idle)
      a.stateData should be(Uninitialized)
    }

    "stay in PreparingToFly state when only a HeadingUpdate is received" in {
      val a = fsm()
      a ! Fly(CourseTarget(1, 1, 1))
      a ! HeadingUpdate(20)
      a.stateName should be(PreparingToFly)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.status.altitude should be(-1)
      sd.status.heading should be(20)
    }

    "move to Flying state when all parts are received" in {
      val a = fsm()
      a ! Fly(CourseTarget(1, 1, 1))
      a ! HeadingUpdate(20)
      a ! AltitudeUpdate(20)
      a ! Controls(testActor)
      a.stateName should be(Flying)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.controls should be(testActor)
      sd.status.altitude should be(20)
      sd.status.heading should be(20)
    }

    "create the Adjustment timer when transitioning to Flying state" in {
      val a = fsm()
      a.setState(PreparingToFly)
      a.setState(Flying)
      a.isTimerActive("Adjustment") should be(true)
    }
  }
}
