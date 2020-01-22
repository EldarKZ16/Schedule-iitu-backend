package service.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import entities.http.{Response, ScheduleTime, Time}
import org.json4s.native.JsonMethods.parse
import reactivemongo.api.DefaultDB
import serialization.Json4sSerialization
import service.{Repository, ScheduleRepository}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object EmptyCabinetUpdater {

  def props(mongoDatabase: DefaultDB): Props =
    Props(new EmptyCabinetUpdater(mongoDatabase))

  case object Update

}

class EmptyCabinetUpdater(mongoDatabase: DefaultDB)
  extends Actor
    with ActorLogging
    with Json4sSerialization {

  import EmptyCabinetUpdater._
  import context.dispatcher
  import utils.Utils._

  context.system.scheduler.schedule(Duration.Zero, UPDATE_INTERVAL_IN_DAYS, self, Update)

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val http: HttpExt = Http(context.system)

  private val repository: Repository = ScheduleRepository(context.system, mongoDatabase)

  override def receive: Receive = {
    case Update =>
      http.singleRequest(HttpRequest(uri = s"$SCHEDULE_REST_TIMETABLE_ROOM_URL?bundle_id=$TEST_BUNDLE_ID"))
        .pipeTo(self)

    case HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { response =>
        val scheduleTime = (parse(response.utf8String)).extract[Option[ScheduleTime]]

        scheduleTime match {
          case Some(_) =>
            log.info("Auto Update...")
            repository.deleteAllEmptyCabinets() onComplete {
              case Success(response: Response) if response.status == 200 =>
                log.info("Successfully sent update request...")
                repository.updateAllEmptyCabinets()
              case Success(response) =>
                log.info(s"Clear database request failed with: $response, it seems database is already empty. Sending update request...")
                repository.updateAllEmptyCabinets()
              case Failure(exception) =>
                log.error(s"Error occurred, exception: $exception")
            }
          case None =>
            log.warning("Schedule.iitu.kz doesn't contain timetable. Auto update is failed")
        }
      }

    case response @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      response.discardEntityBytes()

    case msg =>
      log.warning(s"Received unexpected message, $msg")
  }
}

