package service

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.config.Config
import entities.Schedule
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import scala.concurrent.duration._

object FreeRoomActor {

  case class GetSchedule(day: Int)

  case object EmptyDatabase

  case object UpdateDatabase

  def props(mongoDBManager: ActorRef,
            mongoDatabase: Future[DefaultDB])
           (implicit system: ActorSystem, config: Config): Props =
    Props(new FreeRoomActor(mongoDBManager, mongoDatabase))

}

class FreeRoomActor(mongoDBManager: ActorRef,
                    mongoDatabase: Future[DefaultDB])
                   (implicit system: ActorSystem,
                    config: Config)
  extends Actor
    with ActorLogging {

  import FreeRoomActor._
  implicit val timeout: Timeout = Timeout(30.seconds)
  import context.dispatcher

  val collectionName: String = config.getString("mongo.rooms-collection")
  val bsonCollection: Future[BSONCollection] = mongoDatabase.map(_.collection(s"$collectionName"))

  override def receive: Receive = {
    case GetSchedule(day) =>
      (mongoDBManager ? MongoDBManager.GetFreeRooms(day.toString, bsonCollection))
        .mapTo[Option[Schedule]]
        .pipeTo(sender())

    case EmptyDatabase =>
      (1 to 5).foreach{day =>
        mongoDBManager ! MongoDBManager.DeleteDay(day.toString, bsonCollection)
      }

    case UpdateDatabase =>
      val scheduleStreamer = context.actorOf(ScheduleStreamer.props(mongoDBManager, mongoDatabase))
      scheduleStreamer ! ScheduleStreamer.RetrieveSchedule

  }
}
