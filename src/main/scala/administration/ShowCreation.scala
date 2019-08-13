package administration

import akka.pattern._
import akka.util.Timeout
import akka.actor.ActorRef
import akka.persistence.PersistentActor

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.{ActorStateTransition, Done}

import scala.concurrent.ExecutionContextExecutor

object ShowCreation {
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

  case class ShowCreationFinished(id: String, theaterName: Name, showName: Name, timeSlots: List[TimeSlot] = Nil,
                                  sections: List[Section] = Nil, priceBySection: Map[String, Money] = Map.empty)

  case class CancelShowCreation()

  case class ShowCreationCancelled(id: String)

  trait State

  object InProgress extends State

  object Finished extends State

  object Cancelled extends State

}

// ID must be Theater name + show name
class ShowCreation(id: String, calendar: ActorRef) extends PersistentActor with ActorStateTransition {

  import Calendar._
  import ShowCreation._

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = 20 seconds

  override def persistenceId: String = id

  type S = TheaterShowCreationState

  override def receiveCommand: Receive = creationHandler

  val start: ShowCreationStarted => Try[TheaterShowCreationState] =
    event => Success(TheaterShowCreationState(event.theaterName, event.showName, event.timeSlots))
  val addSection: SectionAdded => Try[TheaterShowCreationState] =
    event => state.addSection(event.section)
  val removeSection: SectionRemoved => Try[TheaterShowCreationState] =
    event => state.removeSection(event.name)
  val setSectionPrice: SectionPriceSet => Try[TheaterShowCreationState] =
    event => state.setPrice(event.name, event.price)
  val asFinished: ShowCreationFinished => Try[TheaterShowCreationState] =
    _ => state.asFinished()
  val asCancelled: ShowCreationCancelled => Try[TheaterShowCreationState] =
    _ => state.asFinished()

  val sectionsHandler: Receive = {
    case AddSection(section: Section) =>
      applyPersisting(addSection)(SectionAdded(id, section))
    case RemoveSection(name) =>
      applyPersisting(removeSection)(SectionRemoved(id, name))
  }

  val pricesHandler: Receive = {
    case SetSectionPrice(sectionName, price) =>
      applyPersisting(setSectionPrice)(SectionPriceSet(id, sectionName, price))
  }

  val noHandler: Receive = Map.empty

  val finishCreationHandler: Receive = {
    case FinishShowCreation =>
      (calendar ? ConfirmTimeSlotsReservation(state.timeSlots)).pipeTo(self)(sender())
    case Done =>
      applyPersisting(asFinished)(ShowCreationFinished(id, state.theaterName,
        state.showName, state.timeSlots, state.sections, state.priceBySection))
      context.become(noHandler)
  }

  val cancelCreationHandler: Receive = {
    case CancelShowCreation =>
      (calendar ? UnreserveTimeSlots(state.timeSlots)).pipeTo(self)(sender())
    case Done =>
      applyPersisting(asCancelled)(ShowCreationCancelled(id))
      context.become(noHandler)
  }

  val creationHandler: Receive = {
    case StartShowCreation(theaterName, showName, timeSlots) =>
      applyPersisting(start)(ShowCreationStarted(id, theaterName, showName, timeSlots))
      context.become(sectionsHandler orElse pricesHandler orElse cancelCreationHandler orElse finishCreationHandler)
  }

  override def receiveRecover: Receive = {
    case evt: ShowCreationStarted => applySuccess(start)(evt)
      context.become(sectionsHandler orElse pricesHandler orElse cancelCreationHandler orElse finishCreationHandler)
    case evt: SectionAdded => applySuccess(addSection)(evt)
    case evt: SectionRemoved => applySuccess(removeSection)(evt)
    case evt: SectionPriceSet => applySuccess(setSectionPrice)(evt)
    case evt: ShowCreationFinished => applySuccess(asFinished)(evt); context.become(noHandler)
    case evt: ShowCreationCancelled => applySuccess(asCancelled)(evt); context.become(noHandler)
  }
}

case class TheaterShowCreationState(theaterName: Name,
                                    showName: Name,
                                    timeSlots: List[TimeSlot] = Nil,
                                    sections: List[Section] = Nil,
                                    priceBySection: Map[String, Money] = Map.empty,
                                    state: ShowCreation.State = ShowCreation.InProgress) {

  import ShowCreation._

  def addSection(section: Section): Try[TheaterShowCreationState] =
    sections.find(_.name == section.name).
      fold[Try[TheaterShowCreationState]](
        Success(copy(sections = sections :+ section)))(_ =>
        Failure(new Exception(SectionAlreadyExist)))

  def removeSection(name: String): Try[TheaterShowCreationState] =
    Success(copy(
      sections = sections.filterNot(_.name == name),
      priceBySection = priceBySection - name))

  def setPrice(sectionName: String, price: Money): Try[TheaterShowCreationState] =
    sections.find(_.name == sectionName).
      fold[Try[TheaterShowCreationState]](
        Failure(new Exception(SectionNotDefined)))(_ =>
        Success(copy(priceBySection = priceBySection + (sectionName -> price))))

  def asFinished(): Try[TheaterShowCreationState] =
    sections.map(_.name).diff(priceBySection.keys.toList) match {
      case Nil => Success(copy(state = ShowCreation.Finished))
      case _ :: _ => Failure(new Exception(MissingPrices))
    }

  def asCancelled(): Try[TheaterShowCreationState] =
    Success(copy(state = ShowCreation.Cancelled))
}

