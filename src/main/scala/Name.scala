import Arguments._

case class Name(string: String) {

  string mustBe(notBlank, "name is required")
  string mustBe(alphabetic, "name must contain alphabetic characters only")
}
