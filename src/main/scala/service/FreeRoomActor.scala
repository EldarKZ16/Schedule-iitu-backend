package service

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import entities.Schedule
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import scala.concurrent.duration._

object FreeRoomActor {

  case class GetSchedule(day: Int)

  case object EmptyDatabase

  case object UpdateDatabase

  def props(mongoDBManager: ActorRef, bsonCollection: Future[BSONCollection])(implicit system: ActorSystem): Props = Props(new FreeRoomActor(mongoDBManager, bsonCollection))

}

class FreeRoomActor(mongoDBManager: ActorRef, bsonCollection: Future[BSONCollection])(implicit system: ActorSystem) extends Actor with ActorLogging {
  import FreeRoomActor._
  implicit val timeout: Timeout = Timeout(30.seconds)
  import context.dispatcher

  override def receive: Receive = {
    case GetSchedule(day) =>
      (mongoDBManager ? MongoDBManager.GetFreeRooms(day.toString)).mapTo[Option[Schedule]].pipeTo(sender())

    case EmptyDatabase =>
      (1 to 5).foreach{day =>
        mongoDBManager ! MongoDBManager.DeleteDay(day.toString)
      }

    case UpdateDatabase =>
      val scheduleStreamer = context.actorOf(ScheduleStreamer.props(bsonCollection, mongoDBManager))
      scheduleStreamer ! ScheduleStreamer.RetrieveSchedule

  }
}
