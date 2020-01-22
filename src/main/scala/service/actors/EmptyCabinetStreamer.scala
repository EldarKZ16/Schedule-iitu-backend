package service.actors

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import reactivemongo.api.DefaultDB
import utils.Utils

import scala.concurrent.duration._

object EmptyCabinetStreamer {

  case object RetrieveCabinet

  def props(mongoDatabase: DefaultDB): Props =
    Props(new EmptyCabinetStreamer(mongoDatabase))

}

class EmptyCabinetStreamer(mongoDatabase: DefaultDB)
  extends Actor
    with ActorLogging {

  import context.dispatcher

  context.system.scheduler.scheduleOnce(30.seconds, self, PoisonPill)

  import EmptyCabinetStreamer._

  override def receive: Receive = {
    case RetrieveCabinet =>
      Utils.CABINETS.foreach { room =>
        context.actorOf(EmptyCabinetRetriever.props(room, mongoDatabase))
      }
  }

}
