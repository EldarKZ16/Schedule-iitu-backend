package service

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, document}
import serialization.{BSONSerialization, Json4sSerialization}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MongoDBManager {

  trait MongoRequest {
    val bsonCollection: Future[BSONCollection]
  }

  final case class AddFreeRoom(day: String, timetable: String, room: String, bsonCollection: Future[BSONCollection]) extends MongoRequest

  final case class DeleteDay(day: String, bsonCollection: Future[BSONCollection]) extends MongoRequest

  final case class GetFreeRooms(day: String, bsonCollection: Future[BSONCollection])

}

class MongoDBManager
  extends Actor
    with ActorLogging
    with Json4sSerialization
    with BSONSerialization {

  import MongoDBManager._
  import context.dispatcher

  override def receive: Receive = {
    case AddFreeRoom(day, timetable, room, bsonCollection) =>
      val selector = BSONDocument("day" -> day)

      val modifier = BSONDocument(
        "$addToSet" -> BSONDocument(
          s"timetable.$timetable" -> room)
      )

      val updateResult =
        bsonCollection
          .flatMap(
            _.update
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

    case DeleteDay(day, bsonCollection) =>
      val selector = BSONDocument("day" -> day)

      val futureRemove = bsonCollection.flatMap(_.delete.one(selector))

      futureRemove.onComplete {
        case Failure(e) =>
          log.warning(s"Failed: $e")
        case Success(_) =>
          log.info(s"Successfully removed day: $day")
      }

    case GetFreeRooms(day, bsonCollection) =>
      bsonCollection.flatMap(_.find(document("day" -> day), None).one).pipeTo(sender())
  }
}