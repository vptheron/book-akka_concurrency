package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorRef}

object DrinkingBehaviour {

  case class LevelChanged(level: Float)

  case object FeelingSober

  case object FeelingTipsy

  case object FeelingLikeZaphod

  def apply(drinker: ActorRef) =
    new DrinkingBehaviour(drinker) with DrinkingResolution

}

class DrinkingBehaviour(drinker: ActorRef) extends Actor {

  this: DrinkingResolution =>

  import DrinkingBehaviour._

  private implicit val ec = context.dispatcher

  private var currentLevel = 0f

  private val scheduler = context.system.scheduler

  val sobering = scheduler.schedule(initialSobering, soberingInterval, self, LevelChanged(-0.0001f))

  override def postStop(): Unit ={
    sobering.cancel()
  }

  override def preStart(): Unit ={
    drink()
  }

  private def drink() = scheduler.scheduleOnce(drinkInterval(), self, LevelChanged(0.005f))

  def receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel + amount).max(0f)
      val msg = currentLevel match {
        case _ if currentLevel <= 0.01 => drink(); FeelingSober
        case _ if currentLevel <= 0.03 => drink(); FeelingTipsy
        case _ => FeelingLikeZaphod
      }
      drinker ! msg
  }
}

trait DrinkingResolution {
  import scala.util.Random
  import scala.concurrent.duration._

  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration = Random.nextInt(300).seconds
}

trait DrinkingProvider {
  def newDrinkingBehaviour(drinker: ActorRef): Props =
    Props(DrinkingBehaviour(drinker))
}