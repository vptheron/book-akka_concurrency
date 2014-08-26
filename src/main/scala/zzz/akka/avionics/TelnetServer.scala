package zzz.akka.avionics

import java.net.InetSocketAddress

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.io.{Tcp, IO}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TelnetServer(plane: ActorRef) extends Actor with ActorLogging {

  import TelnetServer._

  implicit val system = context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 31733))

  def receive = {
    case Bound(address) => log.info("Telnet server listening on port {}", address)

    case Connected(remote, local) =>
      log.info("New incoming client connection on server")
      val connection = sender()
      connection ! Register(context.actorOf(Props(new SubServer(plane))))
      connection ! Write(ByteString(welcome))
  }
}

object TelnetServer {

  implicit val askTimeout = Timeout(1.second)

  val welcome =
    """Welcome to the Airplane!
      |------------------------
      |
      |Valid commands are: 'heading' and 'altitude'
      |
      |>""".stripMargin


  def ascii(bytes: ByteString): String = bytes.utf8String.trim

  class SubServer(plane: ActorRef) extends Actor {

    import HeadingIndicator._
    import Altimeter._

    implicit val ec = context.dispatcher

    def headStr(head: Float): ByteString = ByteString(f"Current heading is: $head%3.2f degrees\n\n> ")

    def altStr(alt: Double): ByteString = ByteString(f"Current altitude is: $alt%5.2f feet\n\n> ")

    def unknown(str: String): ByteString = ByteString(f"Current $str is: unknown\n\n> ")

    def handleHeading(connection: ActorRef) = {
      (plane ? GetCurrentHeading).mapTo[CurrentHeading].onComplete {
        case Success(CurrentHeading(heading)) => connection ! Write(headStr(heading))
        case Failure(_) => connection ! Write(unknown("heading"))
      }
    }

    def handleAltitude(connection: ActorRef) = {
      (plane ? GetCurrentAltitude).mapTo[CurrentAltitude].onComplete {
        case Success(CurrentAltitude(altitude)) => connection ! Write(altStr(altitude))
        case Failure(_) => connection ! Write(unknown("altitude"))
      }
    }

    def receive = {
      case Received(data) =>
        val connection = sender()
        val write = ascii(data) match {
          case "heading" => handleHeading(connection)
          case "altitude" => handleAltitude(connection)
          case m => connection ! Write(ByteString("What?\n\n"))
        }

      case PeerClosed => context.stop(self)
    }
  }

}
