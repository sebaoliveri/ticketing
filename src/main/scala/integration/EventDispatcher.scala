package integration

import java.time.format.DateTimeFormatter

import administration.ShowCreation.ShowCreationFinished
import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import integration.kafka.{CommittableEventMessage, EventDispatcherStreamControl, EventMessage}
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern._
import akka.util.Timeout
import booking.ShowTimeCommands.Create
import scala.concurrent.duration._

object EventDispatcher {
  private implicit val timeout: Timeout = Timeout(15.seconds)

  def apply(showTimeRef: ActorRef, consumerSettings: ConsumerSettings[String, JsValue], committerSettings: CommitterSettings)
           (implicit mat: Materializer, ec: ExecutionContext): EventDispatcherStreamControl = {

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics("ShowAdministration"))
      .map(new CommittableEventMessage(_))
      .mapAsync(4) { eventMessage =>
        eventMessage.eventMessage match {
          case EventMessage("ShowCreationFinished", "v1", body) =>
            val showCreationFinished = body.as[ShowCreationFinished]
            val creations = toCreateCommands(showCreationFinished).map(create => showTimeRef ? create)
            Future.sequence(creations).map(_ => eventMessage.message.committableOffset)
          case _ =>
            Future.successful(eventMessage.message.committableOffset)
        }
      }
      .toMat(Committer.sink(committerSettings))(Keep.both)
      .mapMaterializedValue(EventDispatcherStreamControl(_))
      .run()

  }

  private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYYMMdd_HHMM")

  def toCreateCommands(creation: ShowCreationFinished): List[Create] = {
    creation.timeSlots.map(slot =>
      Create(
        s"${creation.theaterName}_${creation.showName}_${timeFormatter.format(slot.startingDateTime)}",
        creation.theaterName.toString,
        creation.showName.toString,
        slot,
        creation.sections.toSet,
        creation.priceBySection
      )
    )
  }
}
