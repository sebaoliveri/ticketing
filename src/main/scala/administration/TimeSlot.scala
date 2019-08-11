package administration

import java.time.LocalDateTime

import util.Arguments.InvalidArgumentException

case class TimeSlot(startingDateTime: LocalDateTime, endingDateTime: LocalDateTime) {

  if (isAfterOrEqualTo(startingDateTime, endingDateTime))
    throw InvalidArgumentException("StartingDateTime must be greater than EndingDateTime")

  private def isAfterOrEqualTo(startingDateTime: LocalDateTime, endingDateTime: LocalDateTime) =
    startingDateTime.isAfter(endingDateTime) || startingDateTime.isEqual(endingDateTime)

  def overlaps(anotherTimeSlot: TimeSlot): Boolean = ???
}
