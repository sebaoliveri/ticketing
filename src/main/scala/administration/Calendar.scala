package administration

import util.Arguments.InvalidArgumentException
import akka.persistence.PersistentActor

import scala.util.{Failure, Success, Try}

object Calendar {
  val TimeSlotOverlapped = "TimeSlot overlaps"

  sealed trait Command

  case class ReserveTimeSlots(timeSlots: List[TimeSlot])
  case class TimeSlotsReserved(id: String, timeSlots: List[TimeSlot])

  case class UnreserveTimeSlots(timeSlots: List[TimeSlot])
  case class TimeSlotsUnreserved(id: String, timeSlots: List[TimeSlot])

  case class ConfirmTimeSlotsReservation(timeSlots: List[TimeSlot])
  case class TimeSlotsReservationConfirmed(id: String, timeSlots: List[TimeSlot])
}

// ID must be the theater name
class Calendar(id: String) extends PersistentActor {

  import Calendar._

  var state: CalendarState = CalendarState()

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case ReserveTimeSlots(timeSlots) =>
      Try(state.reserve(timeSlots)) match {
        case Success(newState) =>
          persist(TimeSlotsReserved(id, timeSlots)) { event =>
            state = newState
            sender() ! event
          }
        case Failure(exception) =>
          sender() ! akka.actor.Status.Failure(exception)
      }
    case UnreserveTimeSlots(timeSlots) =>
      persist(TimeSlotsUnreserved(id, timeSlots)) { event =>
        state = state.unreserve(timeSlots)
        sender() ! event
      }
    case ConfirmTimeSlotsReservation(timeSlots) =>
      persist(TimeSlotsReservationConfirmed(id, timeSlots)) { event =>
        state = state.confirm(timeSlots)
        sender() ! event
      }
  }

  override def receiveRecover: Receive = {
    case TimeSlotsReserved(_,timeSlots) => state = state.reserve(timeSlots)
    case TimeSlotsUnreserved(_,timeSlots) => state = state.unreserve(timeSlots)
    case TimeSlotsReservationConfirmed(_,timeSlots) => state = state.confirm(timeSlots)
  }
}

case class CalendarState(occupiedTimeSlots: List[TimeSlot] = Nil, reservedTimeSlots: List[TimeSlot] = Nil) {

  import Calendar._

  def reserve(timeSlots: List[TimeSlot]): CalendarState =
    timeSlots.find(isOccupied).fold(copy(reservedTimeSlots = reservedTimeSlots ++ timeSlots))(
      throw InvalidArgumentException(TimeSlotOverlapped))

  def unreserve(timeSlots: List[TimeSlot]): CalendarState =
    copy(reservedTimeSlots = reservedTimeSlots.diff(timeSlots))

  def confirm(timeSlots: List[TimeSlot]): CalendarState =
    copy(reservedTimeSlots = reservedTimeSlots.diff(timeSlots),
      occupiedTimeSlots = occupiedTimeSlots ++ timeSlots)

  private def isOccupied(timeSlot: TimeSlot): Boolean =
    occupiedTimeSlots.union(reservedTimeSlots).exists(_.overlaps(timeSlot))
}
