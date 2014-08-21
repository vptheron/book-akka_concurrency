package zzz.akka.avionics

import akka.actor.{Props, Actor, FSM, ActorRef}
import scala.concurrent.duration._

object FlyingBehaviour {

  import ControlSurfaces._

  sealed trait State

  case object Idle extends State

  case object Flying extends State

  case object PreparingToFly extends State

  case class CourseTarget(altitude: Double,
                          heading: Float,
                          byMillis: Long)

  case class CourseStatus(altitude: Double,
                          heading: Float,
                          headingSinceMS: Long,
                          altitudeSinceMS: Long)

  type Calculator = (CourseTarget, CourseStatus) => Any

  sealed trait Data

  case object Uninitialized extends Data

  case class FlightData(controls: ActorRef,
                        elevCalc: Calculator,
                        bankCalc: Calculator,
                        target: CourseTarget,
                        status: CourseStatus) extends Data

  case class Fly(target: CourseTarget)

  def currentMS = System.currentTimeMillis

  def calcElevator(target: CourseTarget, status: CourseStatus): Any = {
    val alt = (target.altitude - status.altitude).toFloat
    val dur = target.byMillis - status.altitudeSinceMS
    if (alt < 0) StickForward((alt / dur) * -1) else StickBack(alt / dur)
  }

  def calcAilerons(target: CourseTarget, status: CourseStatus): Any = {
    import scala.math.{abs, signum}
    val diff = target.heading - status.heading
    val dur = target.byMillis - status.headingSinceMS
    val amount = if (abs(diff) < 180) diff else signum(diff) * (abs(diff) - 360f)
    if (amount > 0) StickRight(amount / dur) else StickLeft((amount / dur) * -1)
  }

  private case object Adjust

  private def prepComplete(data: Data): Boolean = data match {
    case FlightData(c, _, _, _, s) =>
      !c.isTerminated && s.heading != -1f && s.altitude != -1f
    case _ => false
  }

  private def adjust(data: FlightData): FlightData = data match {
    case FlightData(c, elevCalc, bankCalc, t, s) =>
      c ! elevCalc(t, s)
      c ! bankCalc(t, s)
      data
  }

  case class NewElevatorCalculator(f: Calculator)

  case class NewBankCalculator(f: Calculator)

}

class FlyingBehaviour(plane: ActorRef,
                      heading: ActorRef,
                      altimeter: ActorRef)
  extends Actor with FSM[FlyingBehaviour.State, FlyingBehaviour.Data] {

  import FlyingBehaviour._
  import Pilots._
  import Plane._
  import Altimeter._
  import HeadingIndicator._
  import EventSource._

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Fly(target), _) =>
      goto(PreparingToFly) using FlightData(
        context.system.deadLetters,
        calcElevator,
        calcAilerons,
        target,
        CourseStatus(-1, -1, 0, 0))
  }

  onTransition {
    case Idle -> PreparingToFly =>
      plane ! GiveMeControl
      heading ! RegisterListener(self)
      altimeter ! RegisterListener(self)
  }

  when(PreparingToFly, stateTimeout = 5.seconds)(transform {
    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(
        status = d.status.copy(
          heading = head, headingSinceMS = currentMS))

    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(
        status = d.status.copy(
          altitude = alt, altitudeSinceMS = currentMS))

    case Event(Controls(ctrls), d: FlightData) =>
      stay using d.copy(controls = ctrls)

    case Event(StateTimeout, _) =>
      plane ! LostControl
      goto(Idle)
  } using {
    case s if prepComplete(s.stateData) =>
      s.copy(stateName = Flying)
  })

  onTransition {
    case PreparingToFly -> Flying =>
      setTimer("Adjustment", Adjust, 200.milliseconds, repeat = true)
  }

  when(Flying) {
    case Event(AltitudeUpdate(alt), d: FlightData) =>
      stay using d.copy(
        status = d.status.copy(altitude = alt, altitudeSinceMS = currentMS))

    case Event(HeadingUpdate(head), d: FlightData) =>
      stay using d.copy(
        status = d.status.copy(
          heading = head, headingSinceMS = currentMS))

    case Event(Adjust, d: FlightData) =>
      stay using adjust(d)

    case Event(NewBankCalculator(f), d: FlightData) =>
      stay using d.copy(bankCalc = f)

    case Event(NewElevatorCalculator(f), d: FlightData) =>
      stay using d.copy(elevCalc = f)
  }

  onTransition {
    case Flying -> _ => cancelTimer("Adjustment")
  }

  onTransition {
    case _ -> Idle =>
      heading ! UnregisterListener(self)
      altimeter ! UnregisterListener(self)
  }

  whenUnhandled {
    case Event(RelinquishControl, _) => goto(Idle)
  }

  initialize()
}

trait FlyingProvider {
  def newFlyingBehaviour(plane: ActorRef,
                         heading: ActorRef,
                         altimeter: ActorRef): Props =
    Props(new FlyingBehaviour(plane, heading, altimeter))
}
