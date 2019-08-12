package administration

import util.Arguments.InvalidArgumentException
import administration.Calendar.{ReserveTimeSlots, TimeSlotsReserved}
import akka.{ActorStateTransition, Done}
import akka.actor.ActorRef
import akka.pattern._
import akka.persistence.PersistentActor
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Success, Try}

object Theater {

  case class CreateTheater(name: Name, address: Address)
  case class TheaterCreated(name: Name, address: Address)

  case class StartShowCreation(showName: Name, timeSlots: List[TimeSlot])
}

class Theater(id: String, showCreation: ActorRef, calendar: ActorRef) extends PersistentActor with ActorStateTransition {

  import Theater._

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = 20 seconds

  type S = TheaterState

  override def persistenceId: String = id

  val createTheater: TheaterCreated => Try[S] = event => Success(TheaterState(event.name, event.address))

  override def receiveCommand: Receive = {
    case CreateTheater(name, address) =>
      applyPersisting(createTheater)(TheaterCreated(name, address))

    case StartShowCreation(showName, timeSlots) =>
      (calendar ? ReserveTimeSlots(timeSlots)).mapTo[TimeSlotsReserved].map { _ =>
        showCreation ! ShowCreation.StartShowCreation(state.name, showName, timeSlots)
        sender() ! Done
      }.recover { case exception: InvalidArgumentException =>
        sender() ! akka.actor.Status.Failure(exception)
      }
  }

  override def receiveRecover: Receive = {
    case evt:TheaterCreated => applySuccess(createTheater)(evt)
  }
}

case class TheaterState(name: Name, address: Address)
