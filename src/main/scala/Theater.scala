
import Arguments._
import Calendar.{ReserveTimeSlots, TimeSlotsReserved}
import akka.Done
import akka.actor.ActorRef
import akka.persistence.PersistentActor
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object Theater {

  case class CreateTheater(name: Name, address: Address)
  case class TheaterCreated(name: Name, address: Address)

  case class StartShowCreation(showName: Name, timeSlots: List[TimeSlot])
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

    case StartShowCreation(showName, timeSlots) =>
      (calendar ? ReserveTimeSlots(timeSlots)).mapTo[TimeSlotsReserved].map { _ =>
        showCreation ! TheaterShowCreation.StartShowCreation(state.name, showName, timeSlots)
        sender() ! Done
      }.recover { case exception: InvalidArgumentException =>
        sender() ! akka.actor.Status.Failure(exception)
      }
  }

  override def receiveRecover: Receive = {
    case TheaterCreated(name, address) => state = TheaterState(name, address)
  }
}

case class TheaterState(name: Name, address: Address)
