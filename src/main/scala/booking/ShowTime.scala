package booking

import administration.{Money, SeatLocation, Section, TimeSlot}
import akka.Done
import akka.actor.Status
import akka.persistence.PersistentActor
import booking.ShowTimeCommands.Reserve
import booking.ShowTimeEvents.ReserveConfirmed

import scala.util.{Failure, Success, Try}

object ShowTimeCommands {

  sealed trait Commad {
    val showTimeId: String
  }

  case class Create(
                     showTimeId: String,
                     theaterName: String,
                     showName: String,
                     timeSlot: TimeSlot,
                     sections: Set[Section],
                     pricesBySection: Map[String, Money]
                   ) extends Commad

  case class Reserve(showTimeId: String, requestedSeats: Set[SeatLocation]) extends Commad

}

object ShowTimeEvents {

  case class ShowTimeCreated(
                              theaterName: String,
                              showName: String,
                              timeSlot: TimeSlot,
                              sections: Set[Section],
                              pricesBySection: Map[String, Money]
                            )

  case class ReserveConfirmed(seats: Set[SeatLocation])

}

object ShowTime {


}

class ShowTime extends PersistentActor {
  var showTimeState: ShowTimeState = _

  override def persistenceId: String = self.path.name //ShowTime-{id}

  override def receiveCommand: Receive = {
    // case Create(...) => ...

    case Reserve(_, requestedSeats) => showTimeState.reserve(requestedSeats) match {
      case Success(newState) => persist(ReserveConfirmed(requestedSeats)) { _ =>
        showTimeState = newState
        sender() ! Done
      }
      case Failure(exception) => sender() ! Status.Failure(exception)
    }
  }

  override def receiveRecover: Receive = {
    // case Create(...) => ...
    case ReserveConfirmed(seats) => showTimeState = showTimeState.copy(reservedSeats = seats)
  }

  case class ShowTimeState(timeSlot: TimeSlot, sections: Set[Section], reservedSeats: Set[SeatLocation]) {
    lazy val allLocations: Set[SeatLocation] = sections.flatten(_.locations)

    def reserve(requestedSeats: Set[SeatLocation]): Try[ShowTimeState] = {
      val notBelongedLocations = requestedSeats.diff(showTimeState.allLocations).toList
      val alreadyReserverd = showTimeState.reservedSeats.intersect(requestedSeats).toList
      notBelongedLocations -> alreadyReserverd match {
        case (Nil, Nil) => Success(copy(reservedSeats = reservedSeats ++ requestedSeats))
        case (_ :: _, Nil) => Failure(new Exception("SeatsNotBelongToTheater"))
        case (_, _) => Failure(new Exception("SeatsAlreadyReserved"))
      }
    }
  }

}

