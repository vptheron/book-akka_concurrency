package zzz.akka.avionics

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, Props, Actor, ActorLogging}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Altimeter {

  case class RateChange(amount: Float)

  case class AltitudeUpdate(altitude: Double)

  val ceiling = 43000
  val maxRateOfClimb = 5000

  private case object Tick

  def apply(): Altimeter = new Altimeter with ProductionEventSource

  private case class CalculateAltitude(lastTick: Long,
                                       tick: Long,
                                       roc: Double)

  private case class AltitudeCalculated(newTick: Long,
                                        altitude: Double)

  case object GetCurrentAltitude

  case class CurrentAltitude(altitude: Double)
}

class Altimeter extends Actor with ActorLogging {
  this: EventSource =>

  import Altimeter._

  implicit val ec: ExecutionContext = context.dispatcher

  private[avionics] var rateOfClimb = 0f
  private[avionics] var altitude = 0d
  private var lastTick = System.currentTimeMillis

  private val ticker = context.system.scheduler.schedule(100.millis, 100.millis, self, Tick)

  private val altitudeCalculator = context.actorOf(Props(
    new Actor {
      def receive = {
        case CalculateAltitude(lastTick, tick, roc) =>
          val alt = ((tick - lastTick) / 60000.0) *  roc
          sender ! AltitudeCalculated(tick, alt)
      }
    }
  ), "AltitudeCalculator")

  override val supervisorStrategy =
    OneForOneStrategy(-1, Duration.Inf){
      case _ => Restart
    }

  private def altimeterReceive: Receive = {
    case RateChange(amount) =>
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log.info(s"Altimeter changed rate of climb to $rateOfClimb")

    case Tick =>
      val tick = System.currentTimeMillis
      altitudeCalculator ! CalculateAltitude(lastTick, tick, rateOfClimb)
      lastTick = tick

    case AltitudeCalculated(tick, altDelta) =>
      altitude += altDelta
      altitude = altitude.min(ceiling)
      sendEvent(AltitudeUpdate(altitude))

    case GetCurrentAltitude => sender ! CurrentAltitude(altitude)
  }

  def receive = eventSourceReceive orElse altimeterReceive

  override def postStop(): Unit = {
    ticker.cancel()
  }

}

trait AltimeterProvider {

  def newAltimeter: Actor = Altimeter()

}
