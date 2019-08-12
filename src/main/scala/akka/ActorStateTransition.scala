package akka

import akka.persistence.PersistentActor

import scala.util.{Failure, Success, Try}

trait ActorStateTransition {
  this: PersistentActor =>

  type S
  var state: S = _

  def applyPersisting[E](function: E => Try[S])(event: E): Unit =
    function(event) match {
      case Success(newState: S) =>
        persist(event) { _ =>
          state = newState
          sender() ! Done
        }
      case Failure(exception) =>
        sender() ! akka.actor.Status.Failure(exception)
    }

  def applySuccess[E](function: E => Try[S])(event: E): Unit =
    function(event).foreach(state = _)
}
