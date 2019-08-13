import administration.{Money, Name, Row, Seat, Section, TimeSlot}
import administration.ShowCreation.ShowCreationFinished
import play.api.libs.json.{Json, Reads}

package object integration {

  implicit val g: Reads[Seat] = Json.reads[Seat]
  implicit val f: Reads[Row] = Json.reads[Row]
  implicit val e: Reads[Name] = Json.reads[Name]
  implicit val d: Reads[TimeSlot] = Json.reads[TimeSlot]
  implicit val c: Reads[Section] = Json.reads[Section]
  implicit val b: Reads[Money] = Json.reads[Money]
  implicit val a: Reads[ShowCreationFinished] = Json.reads[ShowCreationFinished]


}
