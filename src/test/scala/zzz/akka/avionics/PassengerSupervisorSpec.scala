package zzz.akka.avionics

import akka.actor.{Props, ActorSystem, Actor, ActorRef}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import zzz.akka.avionics.PassengerSupervisorSpec.TestPassengerProvider
import scala.concurrent.duration._

object PassengerSupervisorSpec {

  val config = ConfigFactory.parseString(
    """zzz.akka.avionics.passengers = [
      |[ "Kelly Franqui", "23", "A" ],
      |[ "Tyrone Dotts", "23", "B" ],
      |[ "Malinda Class", "23", "C" ],
      |[ "Kenya Jolicoeur", "24", "A" ],
      |[ "Christian Piche", "24", "B" ]
      |]""".stripMargin)

  trait TestPassengerProvider extends PassengerProvider {
    override def newPassenger(callButton: ActorRef): Actor =
      new Actor {
        def receive = {
          case m => callButton ! m
        }
      }
  }

}

class PassengerSupervisorSpec
  extends TestKit(ActorSystem("PassengerSupervisorSpec", PassengerSupervisorSpec.config))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  import PassengerSupervisor._

  "PassengerSupervisor" should {
    "work" in {
      val a = system.actorOf(Props(
        new PassengerSupervisor(testActor) with TestPassengerProvider
      ))

      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster

      broadcaster ! "HiThere"

      expectMsg("HiThere")
      expectMsg("HiThere")
      expectMsg("HiThere")
      expectMsg("HiThere")
      expectMsg("HiThere")

      expectNoMsg(100.milliseconds)

      a ! GetPassengerBroadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))
    }
  }

  override def afterAll(): Unit = {
    system.shutdown()
  }
}
