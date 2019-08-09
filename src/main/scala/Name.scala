import Arguments._

case class Name(name: String) {
  name mustBe(notBlank, "name is required")
  name mustBe(alphabetic, "name must contain alphabetic characters only")
}
