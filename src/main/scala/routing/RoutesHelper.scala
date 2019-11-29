package routing

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import entities.Schedule
import service.FreeRoomActor

import scala.concurrent.{ExecutionContextExecutor, Future}

object RoutesHelper {
  case class Accepted(status: Int, message: String)
}
trait RoutesHelper {
  import RoutesHelper._

  implicit val requestTimeout: Timeout
  implicit def executionContext: ExecutionContextExecutor

  def createFreeRoomActor(): ActorRef

  lazy val freeRoomActor: ActorRef = createFreeRoomActor()

  def getSchedule(day: Int): Future[Option[Schedule]] = {
    (freeRoomActor ? FreeRoomActor.GetSchedule(day)).mapTo[Option[Schedule]]
  }

  def emptyDatabase(): Future[Accepted] = {
    freeRoomActor ! FreeRoomActor.EmptyDatabase
    Future{Accepted(StatusCodes.OK.intValue, "OK")}
  }

  def updateDatabase(): Future[Accepted] = {
    freeRoomActor ! FreeRoomActor.UpdateDatabase
    Future{Accepted(StatusCodes.OK.intValue, "OK")}
  }

}
