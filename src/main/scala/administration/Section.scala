package administration

case class Section(name: String, rows: Set[Row]){
  lazy val locations: Set[SeatLocation] =
    rows.flatten(row => row.seats.map(seat => SeatLocation(name, row.order, seat.number)))
}

case class Row(order: Int, seats: Set[Seat])

case class Seat(number: Int, order: Int)

case class SeatLocation(sectionName: String, row: Int, seatNumber: Int)