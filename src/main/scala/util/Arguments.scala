package util

object Arguments extends {

  implicit final class ArgumentAssert[A](private val self: A) extends AnyVal {

    def mustBe(cond: A => Boolean, reason: => String): A = mustBe(cond)(InvalidArgumentException(reason))

    def mustBe(cond: A => Boolean)(exception: Throwable): A = if (cond(self)) self else throw exception
  }

  val notBlank: String => Boolean = string => Option(string).exists(_.trim.nonEmpty)
  val alphabetic: String => Boolean = string => string.matches("^[a-zA-Z ]*$")
  val alphanumeric: String => Boolean = string => string.matches("^[a-zA-Z0-9. ]*$")

  case class InvalidArgumentException(reason: String) extends Exception(reason)
}