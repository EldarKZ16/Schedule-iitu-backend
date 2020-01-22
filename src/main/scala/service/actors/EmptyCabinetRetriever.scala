package service.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import entities.http.Room
import org.json4s
import org.json4s.native.JsonMethods.parse
import reactivemongo.api.DefaultDB
import serialization.Json4sSerialization
import service.dao.EmptyCabinetsDAO
import service.dao.mongo.EmptyCabinetsDAOImpl

object EmptyCabinetRetriever {

  def props(bundleId: String,
            mongoDatabase: DefaultDB): Props =
    Props(new EmptyCabinetRetriever(bundleId, mongoDatabase))

}

class EmptyCabinetRetriever(bundleId: String,
                            mongoDatabase: DefaultDB)
  extends Actor
    with ActorLogging
    with Json4sSerialization {

  import context.dispatcher
  import utils.Utils._

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val http: HttpExt = Http(context.system)

  private val emptyCabinetsDAO: EmptyCabinetsDAO = new EmptyCabinetsDAOImpl(mongoDatabase)

  override def preStart(): Unit = {
    http.singleRequest(HttpRequest(uri = s"$SCHEDULE_REST_TIMETABLE_ROOM_URL?bundle_id=$bundleId"))
      .pipeTo(self)
  }

  override def receive: Receive = {
    case resp @ HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { response =>
        val cabinetNumber = (parse(response.utf8String) \ "bundles" \ bundleId \ "0").extract[Room].name_en

        cabinetNumber match {
          case Some(cabinet) =>
            log.info(s"Cabinet is $cabinet")
            DAYS.foreach { day =>
              TIMES.foreach { time =>
                val parsedScheduleTime = parse(response.utf8String) \ "timetable" \ day \ time
                val result: Option[json4s.JValue] = parsedScheduleTime.toOption
                result match {
                  case None =>
                    emptyCabinetsDAO.add(day, time, cabinet)
                  case Some(_) =>
                }
              }
            }
          case None =>
            log.info("Cabinet doesn't exist")
            resp.discardEntityBytes()
        }
      }

    case response @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      response.discardEntityBytes()

    case msg =>
      log.warning(s"Received unexpected message. Room: $bundleId, message: $msg")
  }
}
