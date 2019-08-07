
import Arguments._
import Calendar.{ReserveTimeSlots, TimeSlotsReserved}
import akka.Done
import akka.actor.ActorRef
import akka.persistence.PersistentActor
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object Theater {

  case class CreateTheater(name: Name, address: Address)
  case class TheaterCreated(name: Name, address: Address)

  case class StartShowCreation(showName: Name, timeFrames: List[TimeSlot])
}

class Theater(name: String, showCreation: ActorRef, calendar: ActorRef) extends PersistentActor {

  import Theater._

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = 20 seconds

  var state: TheaterState = _

  override def persistenceId: String = name

  override def receiveCommand: Receive = {
    case CreateTheater(name, address) =>
      persist(TheaterCreated(name, address)) { evt =>
        state = TheaterState(name, address)
        sender() ! evt
      }

    case StartShowCreation(showName, timeFrames) =>
      (calendar ? ReserveTimeSlots(timeFrames)).mapTo[TimeSlotsReserved].map { _ =>
        showCreation ! TheaterShowCreation.StartShowCreation(state.name, showName, timeFrames)
        sender() ! Done
      }.recover { case exception: InvalidArgumentException =>
        sender() ! akka.actor.Status.Failure(exception)
        unstashAll
        context.unbecome
      }
  }

  override def receiveRecover: Receive = ???
}

case class TheaterState(name: Name, address: Address)
