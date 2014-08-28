package zzz.akka.avionics

import akka.actor.{Actor, Props, ActorRef}
import zzz.akka.MessageTransformer

object Airport {

  case class DirectFlyerToAirport(flyingBehaviour: ActorRef)

  case class StopDirectingFlyer(flyingBehaviour: ActorRef)

  def toronto(): Props = Props(new Airport with BeaconProvider with AirportSpecifics {
    override lazy val headingTo: Float = 314.3f
    override lazy val altitude: Double = 26000
  })

}

class Airport extends Actor {
  this: AirportSpecifics with BeaconProvider =>

  import Airport._
  import Beacon._
  import FlyingBehaviour._
  import zzz.akka.GenericPublisher._

  val beacon = context.actorOf(Props(newBeacon(headingTo)), "Beacon")

  def receive = {
    case DirectFlyerToAirport(flyingBehaviour) =>
      val oneHourFromNow = System.currentTimeMillis + 60 * 60 * 1000
      val when = oneHourFromNow

      context.actorOf(Props(
        new MessageTransformer(from = beacon, to = flyingBehaviour, {
          case BeaconHeading(heading) => Fly(CourseTarget(altitude, heading, when))
        })))

    case StopDirectingFlyer(_) =>
      context.children.foreach(context.stop)
  }

}

trait AirportSpecifics {

  lazy val headingTo: Float = 0.0f
  lazy val altitude: Double = 0d

}