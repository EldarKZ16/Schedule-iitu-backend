package service

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.Future
import scala.concurrent.duration._

object ScheduleStreamer {

  case object RetrieveSchedule

  def props(bsonCollection: Future[BSONCollection], mongoDBManager: ActorRef)
           (implicit system: ActorSystem): Props =
    Props(new ScheduleStreamer(bsonCollection, mongoDBManager))

}

class ScheduleStreamer(bsonCollection: Future[BSONCollection],
                       mongoDBManager: ActorRef)
                      (implicit system: ActorSystem)
  extends Actor
    with ActorLogging {

  import context.dispatcher

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
