package zzz.akka.avionics

import akka.actor.{Actor, ActorLogging}
import scala.concurrent.duration._

object HeadingIndicator {

  case class BankChange(amount: Float)

  case class HeadingUpdate(heading: Float)

  case object GetCurrentHeading

  case class CurrentHeading(heading: Float)

  private case object Tick

  private val maxDegPerSec = 5

  def apply(): HeadingIndicator = new HeadingIndicator with ProductionEventSource

}

class HeadingIndicator
  extends Actor
  with ActorLogging
  with StatusReporter {

  this: EventSource =>

  import HeadingIndicator._
  import StatusReporter._
  import context._

  private val ticker = system.scheduler.schedule(100.millis, 100.millis, self, Tick)

  private var lastTick: Long = System.currentTimeMillis
  private var rateOfBank = 0f
  private var heading = 0f

  def currentStatus = StatusOK

  def headingIndicatorReceive: Receive = {
    case BankChange(amount) =>
      rateOfBank = amount.min(1.0f).max(-1.0f)

    case Tick =>
      val tick = System.currentTimeMillis
      val timeDelta = (tick - lastTick) / 1000f
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick

      sendEvent(HeadingUpdate(heading))

    case GetCurrentHeading => sender ! CurrentHeading(heading)
  }

  def receive = statusReceive orElse
    eventSourceReceive orElse
    headingIndicatorReceive

  override def postStop(): Unit = {
    ticker.cancel()
  }

}

trait HeadingIndicatorProvider {

  def newHeadingIndicator = HeadingIndicator()

}
