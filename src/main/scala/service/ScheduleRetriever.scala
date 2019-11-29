package service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.typesafe.config.Config
import entities.Room
import org.json4s
import org.json4s.native.JsonMethods.parse
import reactivemongo.api.collections.bson.BSONCollection
import serialization.Json4sSerialization

import scala.concurrent.Future

object ScheduleRetriever {

  def props(bundleId: String,
            bsonCollection: Future[BSONCollection],
            mongoDBManager: ActorRef)
           (implicit config: Config): Props =
    Props(new ScheduleRetriever(bundleId, bsonCollection, mongoDBManager))

}

class ScheduleRetriever(bundleId: String,
                        bsonCollection: Future[BSONCollection],
                        mongoDBManager: ActorRef)
                       (implicit config: Config)
  extends Actor
    with ActorLogging
    with Json4sSerialization {

  final val SCHEDULE_REST_TIMETABLE_ROOM_URL = config.getString("schedule.room-url")

  final val DAYS = for (day <- 1 to 6) yield day.toString
  final val TIMES = for (time <- 1 to 13) yield time.toString

  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val http: HttpExt = Http(context.system)

  override def preStart(): Unit = {
    http.singleRequest(HttpRequest(uri = s"$SCHEDULE_REST_TIMETABLE_ROOM_URL?bundle_id=$bundleId"))
      .pipeTo(self)
  }

  override def receive: Receive = {
    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { response =>
        val roomNumber = (parse(response.utf8String) \ "bundles" \ bundleId \ "0").extract[Room].name_en

        roomNumber match {
          case Some(room) =>
            log.info(s"Room is $room")
            DAYS.foreach { day =>
              TIMES.foreach { time =>
                val parsedScheduleTime = parse(response.utf8String) \ "timetable" \ day \ time
                val result: Option[json4s.JValue] = parsedScheduleTime.toOption
                result match {
                  case None =>
                    mongoDBManager ! MongoDBManager.AddFreeRoom(day, time, room, bsonCollection)
                  case Some(_) =>
                }
              }
            }
          case None =>
            log.info("Room doesn't exist")
        }
      }

    case response @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      response.discardEntityBytes()

    case msg =>
      log.warning(s"Received unexpected message: Room: $bundleId, $msg")

  }
}
