package zzz.akka.avionics

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.{Tcp, IO}
import akka.util.{ByteString, Timeout}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TelnetServer(plane: ActorRef) extends Actor with ActorLogging {

  import TelnetServer._

  def receive = {
    case _ =>
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

  case class NewMessage(msg: String)

  class SubServer(socket: ???, plane: ActorRef) extends Actor {
    import HeadingIndicator._
    import Altimeter._

    def headStr(head: Float): ByteString = ByteString(f"Current heading is: $head%3.2f degrees\n\n> ")

    def altStr(alt: Double): ByteString = ByteString(f"Current altitude is: $alt%5.2f feet\n\n> ")

    def unknown(str: String): ByteString = ByteString(f"Current $str is: unknown\n\n> ")

    def handleHeading() = {
      (plane ? GetCurrentHeading).mapTo[CurrentHeading].onComplete {
        case Success(CurrentHeading(heading)) => socket.write(headStr(heading))
        case Failure(_) => socket.write(unknown("heading"))
      }
    }

    def handleAltitude() = {
      (plane ? GetCurrentAltitude).mapTo[CurrentAltitude].onComplete {
        case Success(CurrentAltitude(altitude)) => socket.write(altStr(altitude))
        case Failure(_) => socket.write(unknown("altitude"))
      }
    }

    def receive = {
      case NewMessage(msg) => msg match {
        case "heading" => handleHeading()
        case "altitude" => handleAltitude()
        case m => socket.write(ByteString("What?\n\n"))
      }
    }
  }

}
