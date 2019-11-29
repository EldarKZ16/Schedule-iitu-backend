package service

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import com.typesafe.config.Config
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import scala.concurrent.duration._

object ScheduleStreamer {

  case object RetrieveSchedule

  def props(mongoDBManager: ActorRef,
            mongoDatabase: Future[DefaultDB])
           (implicit system: ActorSystem, config: Config): Props =
    Props(new ScheduleStreamer(mongoDBManager, mongoDatabase))

}

class ScheduleStreamer(mongoDBManager: ActorRef, mongoDatabase: Future[DefaultDB])
                      (implicit system: ActorSystem, config: Config)
  extends Actor
    with ActorLogging {

  import context.dispatcher

  val collectionName: String = config.getString("mongo.rooms-collection")
  val bsonCollection: Future[BSONCollection] = mongoDatabase.map(_.collection(s"$collectionName"))

  final val ROOMS = for (room <- 276 to 323) yield room.toString
  system.scheduler.scheduleOnce(30.seconds, self, PoisonPill)

  import ScheduleStreamer._

  override def receive: Receive = {
    case RetrieveSchedule =>
      ROOMS.foreach {room =>
          context.actorOf(ScheduleRetriever.props(room, bsonCollection, mongoDBManager))
      }
  }

}
