package zzz.akka

import akka.actor.{AllForOneStrategy, OneForOneStrategy, SupervisorStrategy}
import akka.actor.SupervisorStrategy.Decider

import scala.concurrent.duration.Duration

trait SupervisionStrategyFactory {

  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy

}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {

  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy =
    OneForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {

  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy =
    AllForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}