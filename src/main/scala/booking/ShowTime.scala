package booking

import administration.{Money, SeatLocation, Section, TimeSlot}
import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props, Status}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.persistence.PersistentActor
import booking.ShowTimeCommands.Reserve
import booking.ShowTimeEvents.ReserveConfirmed

object ShowTimeCommands {

  sealed trait Command {
    val showTimeId: String
  }

  case class Create(
                     showTimeId: String,
                     theaterName: String,
                     showName: String,
                     timeSlot: TimeSlot,
                     sections: Set[Section],
                     pricesBySection: Map[String, Money]
                   ) extends Command

  case class Reserve(
                      showTimeId: String,
                      requestedSeats: Set[SeatLocation]
                    ) extends Command

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

  def createRef()(implicit actorSystem: ActorSystem): ActorRef = {
    val hashCodeMessageExtractor = new HashCodeMessageExtractor(3 * 10) {
      override def entityId(message: Any): String = message match {
        case command: ShowTimeCommands.Command => command.showTimeId
      }
    }
    ClusterSharding(actorSystem)
      .start("ShowTime", Props(new ShowTime), ClusterShardingSettings(actorSystem), hashCodeMessageExtractor)
  }
}

class ShowTime extends PersistentActor {
  var showTimeState: ShowTimeState = _

  override def persistenceId: String = "ShowTime" + self.path.name

  override def receiveCommand: Receive = {
    // case Create(...) => ...
    case Reserve(_, requestedSeats) =>
      val notBelongedLocations = requestedSeats.diff(showTimeState.allLocations).toList
      val alreadyReserved = showTimeState.reservedSeats.intersect(requestedSeats).toList
      notBelongedLocations -> alreadyReserved match {
        case (Nil, Nil) => persist(ReserveConfirmed(requestedSeats)) { event =>
          updateState(event)
          sender() ! Done
        }
        case (_ :: _, Nil) => sender() ! Status.Failure(new Exception("SeatsNotBelongToTheater"))
        case (_, _) => sender() ! Status.Failure(new Exception("SeatsAlreadyReserved"))
      }
  }

  override def receiveRecover: Receive = {
    // case event: Create => ...
    case event: ReserveConfirmed => updateState(event)
  }

  def updateState(reserveConfirmed: ReserveConfirmed): Unit = {
    showTimeState = showTimeState.copy(reservedSeats = showTimeState.reservedSeats ++ reserveConfirmed.seats)
  }

  case class ShowTimeState(timeSlot: TimeSlot, sections: Set[Section], reservedSeats: Set[SeatLocation]) {
    lazy val allLocations: Set[SeatLocation] = sections.flatten(_.locations)
  }

}
