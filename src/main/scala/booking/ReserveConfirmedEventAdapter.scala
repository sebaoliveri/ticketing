package booking

import akka.actor.ExtendedActorSystem
import akka.persistence.journal.{EventAdapter, EventSeq, Tagged}

class ReserveConfirmedEventAdapter(system: ExtendedActorSystem) extends EventAdapter {
  override def manifest(event: Any): String = "ReserveConfirmed"

  override def toJournal(event: Any): Any = Tagged(event, Set("reserve-confirmed"))

  override def fromJournal(event: Any, manifest: String): EventSeq = EventSeq.single(event)
}