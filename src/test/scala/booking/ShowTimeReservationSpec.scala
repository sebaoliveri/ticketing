package booking

import administration.SeatLocation
import akka.Done
import akka.actor.{ActorSystem, Props, Status}
import akka.testkit.{ImplicitSender, TestKit}
import booking.ShowTimeCommands.Reserve
import org.scalatest.FlatSpecLike

class ShowTimeReservationSpec extends TestKit(ActorSystem("Booking")) with FlatSpecLike with ImplicitSender {

  "Existing ShowTime" should "reserver Seats" in {

    val showTimeId = "ShowTime-GranRex_LaBandaElastica_20190817_2030"
    val aShowTime = system.actorOf(Props(new ShowTime()), showTimeId)
    //aShowTime ! Create(...)
    aShowTime ! Reserve(showTimeId, Set(
      SeatLocation("Platea", 12, 23),
      SeatLocation("Platea", 12, 24),
      SeatLocation("Platea", 12, 25)
    ))
    expectMsg(Done)

    aShowTime ! Reserve(showTimeId, Set(SeatLocation("Platea", 12, 24)))
    expectMsg(Status.Failure(new Exception("SeatsAlreadyReserved")))

  }

}
