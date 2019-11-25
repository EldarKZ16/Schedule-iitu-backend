package routing

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import reactivemongo.api.collections.bson.BSONCollection
import service.FreeRoomActor

import scala.concurrent.{ExecutionContextExecutor, Future}

class RestApi(timeout: Timeout, mongoDBManager: ActorRef, bsonCollection: Future[BSONCollection])(implicit system: ActorSystem) extends Routes {
  implicit val requestTimeout: Timeout = timeout
  implicit def executionContext: ExecutionContextExecutor = system.dispatcher
  def createFreeRoomActor(): ActorRef = system.actorOf(FreeRoomActor.props(mongoDBManager, bsonCollection))
}