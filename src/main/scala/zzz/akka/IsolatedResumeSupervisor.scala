package zzz.akka

import akka.actor.{ActorKilledException, ActorInitializationException}
import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}

import scala.concurrent.duration.Duration

abstract class IsolatedResumeSupervisor(maxNrRetries: Int = -1,
                                        withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {

  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange){
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }

}
