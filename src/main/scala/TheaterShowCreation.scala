
import akka.actor.ActorRef
import akka.persistence.PersistentActor
import scala.concurrent.duration._
import Arguments._

import scala.util.{Failure, Success, Try}

object TheaterShowCreation {
  val SectionAlreadyExist = "The section already exist"
  val SectionNotDefined = "The section does not exist"
  val MissingPrices = "Prices must be specified for every defined section"

  case class StartShowCreation(theaterName: Name, showName: Name, timeSlots: List[TimeSlot])
  case class ShowCreationStarted(id: String, theaterName: Name, showName: Name, timeSlots: List[TimeSlot])

  case class AddSection(section: Section)
  case class SectionAdded(id: String, section: Section)

  case class RemoveSection(name: String)
  case class SectionRemoved(id: String, name: String)

  case class SetSectionPrice(name: String, price: Money)
  case class SectionPriceSet(id: String, name: String, price: Money)

  case class FinishShowCreation()
  case class ShowCreationFinished()

  case class CancelShowCreation()
  case class ShowCreationCancelled()

  trait State
  object InProgress extends State
  object Finished extends State
  object Cancelled extends State
}

// ID must be Theater name + show name
class TheaterShowCreation(id: String, calendar: ActorRef) extends PersistentActor {

  import TheaterShowCreation._
  import Calendar._

  implicit val timeout: FiniteDuration = 20 seconds

  override def persistenceId: String = id

  var state: TheaterShowCreationState = _

  override def receiveCommand: Receive = creationHandler

  val sectionsHandler: Receive = {
    case AddSection(section: Section) =>
      Try(state.addSection(section)) match {
        case Success(theaterShowCreation) =>
          persist(SectionAdded(id, section)) { evt =>
            state = theaterShowCreation
            sender() ! evt
          }
        case Failure(exception) =>
          sender() ! akka.actor.Status.Failure(exception)
      }
    case RemoveSection(name) =>
      persist(SectionRemoved(id, name)) { evt =>
        state = state.removeSection(name)
        sender() ! evt
      }
  }

  val pricesHandler: Receive = {
    case SetSectionPrice(sectionName, price) =>
      Try(state.setPrice(sectionName, price)) match {
        case Success(theaterShowCreation) =>
          persist(SectionPriceSet(id, sectionName, price)) { evt =>
            state = theaterShowCreation
            sender() ! evt
          }
        case Failure(exception) =>
          sender() ! akka.actor.Status.Failure(exception)
      }
  }

  val finishCreationHandler: Receive = {
    case FinishShowCreation =>
      Try(state.asFinished()) match {
        case Success(theaterShowCreation) =>
          calendar ! ConfirmTimeSlotsReservation(state.timeSlots)
          persist(ShowCreationFinished()) { evt =>
            state = theaterShowCreation
            sender() ! evt
            context.become(Map.empty)
          }
        case Failure(exception) =>
          sender() ! akka.actor.Status.Failure(exception)
      }
  }

  val cancelCreationHandler: Receive = {
    case CancelShowCreation =>
      calendar ! UnreserveTimeSlots(state.timeSlots)
      persist(ShowCreationCancelled()) { evt =>
        state = state.asCancelled()
        sender() ! evt
        context.become(Map.empty)
      }
  }

  val creationHandler: Receive = {
    case StartShowCreation(theaterName, showName, timeSlots) =>
      persist(ShowCreationStarted(id, theaterName, showName, timeSlots)) { event =>
        state = TheaterShowCreationState(theaterName, showName, timeSlots)
        sender ! event
      }
      context.become(sectionsHandler orElse pricesHandler orElse cancelCreationHandler orElse finishCreationHandler)
  }

  override def receiveRecover: Receive = ???
}

case class TheaterShowCreationState(theaterName: Name,
                                    showName: Name,
                                    timeSlots: List[TimeSlot] = Nil,
                                    sections: List[Section] = Nil,
                                    priceBySection: Map[String, Money] = Map.empty,
                                    state: TheaterShowCreation.State = TheaterShowCreation.InProgress) {

  import TheaterShowCreation._

  def addSection(section: Section): TheaterShowCreationState =
    sections.find(_.name == section.name).
      fold(copy(sections = sections :+ section))(throw InvalidArgumentException(SectionAlreadyExist))

  def removeSection(name: String): TheaterShowCreationState =
    copy(sections = sections.filterNot(_.name == name), priceBySection = priceBySection - name)

  def setPrice(sectionName: String, price: Money): TheaterShowCreationState =
    sections.find(_.name == sectionName).
      fold(throw InvalidArgumentException(SectionNotDefined))( _ =>
      copy(priceBySection = priceBySection + (sectionName -> price)))

  def asFinished(): TheaterShowCreationState = {
    sections.map(_.name).diff(priceBySection.keys.toList) match {
      case Nil => copy(state = TheaterShowCreation.Finished)
      case _ :: _ => throw InvalidArgumentException(MissingPrices)
    }
  }

  def asCancelled(): TheaterShowCreationState =
    copy(state = TheaterShowCreation.Cancelled)
}

