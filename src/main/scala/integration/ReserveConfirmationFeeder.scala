package integration

import administration.SeatLocation
import akka.actor.ActorSystem
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.kafka.ProducerSettings
import akka.persistence.query.{EventEnvelope, NoOffset, Offset, PersistenceQuery}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import booking.ShowTimeEvents.ReserveConfirmed
import integration.kafka.{EventMessage, TopicPublisher}
import org.apache.kafka.clients.producer.Producer
import play.api.libs.json.{JsValue, Json, Reads}

object ReserveConfirmationFeeder {
  implicit val b = Json.writes[SeatLocation]
  implicit val a = Json.writes[ReserveConfirmed]

  def apply(offset: Offset, producerSettings: ProducerSettings[String, JsValue])(implicit actorSystem: ActorSystem): Any = {

  val publisher = new TopicPublisher("Booking", producerSettings.createKafkaProducer())
  val readJournal = PersistenceQuery(actorSystem).readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)

  readJournal
    .eventsByTag("reserve-confirmed", offset)
    .collect {
      case EventEnvelope(offset, persistenceId, sequenceNr, event: ReserveConfirmed) => event
    }
    .mapAsync(4) { event =>
      publisher.publish(EventMessage("ReserveConfirmed", "v1", Json.toJson(event)))
    }
    .runWith(Sink.ignore)(ActorMaterializer())

  }
}
