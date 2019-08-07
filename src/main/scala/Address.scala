
import Arguments._

case object Address {
  val StreetRequired = "street is required"
  val AlphanumericStreetExpected = "street must contain alphanumeric characters only"
  val ZipCodeRequired = "zipcode is required"
  val AlphanumericZipcodeExpected = "zipcode must contain alphanumeric characters only"
  val CityRequired = "city is required"
  val AlphabeticCityExpected = "city must contain alphabetic characters only"
}

case class Address(street: String, zipCode: String, city: String) {

  import Address._

  street mustBe(notBlank, StreetRequired)
  street mustBe(alphanumeric, AlphanumericStreetExpected)

  zipCode mustBe(notBlank, ZipCodeRequired)
  zipCode mustBe(alphanumeric, AlphanumericZipcodeExpected)

  city mustBe(notBlank, CityRequired)
  city mustBe(alphabetic, AlphabeticCityExpected)
}
