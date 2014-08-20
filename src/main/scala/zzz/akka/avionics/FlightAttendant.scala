package zzz.akka.avionics

import akka.actor.Actor

object FlightAttendant {

  case class GetDrink(drinkName: String)
  case class Drink(drinkName: String)

  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }

}

class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>

  import FlightAttendant._

  private implicit val ec = context.dispatcher

  def receive = {
    case GetDrink(drinkName) =>
      context.system.scheduler.scheduleOnce(responseDuration, sender(), Drink(drinkName))
  }
}

trait AttendantResponsiveness {
  import scala.concurrent.duration._

  val maxResponseTimeMS: Int

  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis

}

