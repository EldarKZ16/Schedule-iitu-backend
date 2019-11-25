package service

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, document}
import serialization.{Json4s, ScheduleBSON}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MongoDBManager {

  case class AddFreeRoom(day: String, timetable: String, room: String)

  case class DeleteDay(day: String)

  case class GetFreeRooms(day: String)

  def props(bsonCollection: Future[BSONCollection]) =
    Props(new MongoDBManager(bsonCollection))

}

class MongoDBManager(bsonCollection: Future[BSONCollection])
  extends Actor
    with ActorLogging
    with Json4s
    with ScheduleBSON {

  import MongoDBManager._
  import context.dispatcher

  override def receive: Receive = {
    case AddFreeRoom(day, timetable, room) =>
      val selector = BSONDocument("day" -> day)

      val modifier = BSONDocument(
        "$addToSet" -> BSONDocument(
          s"timetable.$timetable" -> room)
      )

      val updateResult =
        bsonCollection
          .flatMap(_.update
          .one(
            q = selector,
            u = modifier,
            upsert = true,
            multi = false)
        )

      updateResult onComplete {
        case Success(_) =>
        case Failure(exception) =>
          log.warning(s"Failed to update free room: $exception")
      }

    case DeleteDay(day) =>
      val selector = BSONDocument("day" -> day)

      val futureRemove1 = bsonCollection.flatMap(_.delete.one(selector))

      futureRemove1.onComplete {
        case Failure(e) =>
          log.warning(s"Failed: $e")
        case Success(_) =>
          log.info(s"Successfully removed day: $day")
      }

    case GetFreeRooms(day) =>
      bsonCollection.flatMap(_.find(document("day" -> day), None).one).pipeTo(sender())
  }
}