package zzz.akka.avionics

import akka.actor.Actor

import scala.concurrent.duration._

object Beacon {

  case class BeaconHeading(heading: Float)

  def apply(heading: Float) = new Beacon(heading) with BeaconResolution

  private case object Tick
}

class Beacon(heading: Float) extends Actor {
  this: BeaconResolution =>

  import Beacon._
  import zzz.akka.GenericPublisher._

  private implicit val ec = context.dispatcher

  val bus = new EventBusForActors[BeaconHeading, Boolean]({
    _: BeaconHeading => true
  })

  val ticker = context.system.scheduler.schedule(beaconInterval, beaconInterval, self, Tick)

  def receive = {
    case RegisterListener(actor) => bus.subscribe(actor, true)
    case UnregisterListener(actor) => bus.unsubscribe(actor)
    case Tick => bus.publish(BeaconHeading(heading))
  }
}

trait BeaconResolution {
  lazy val beaconInterval = 1.second
}

trait BeaconProvider {

  def newBeacon(heading: Float) = Beacon(heading)

}