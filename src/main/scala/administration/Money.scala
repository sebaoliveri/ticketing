package administration


case class Money(amount: Double, currency: String) {
  def +(money: Money) = Money(amount + money.amount, currency)
}