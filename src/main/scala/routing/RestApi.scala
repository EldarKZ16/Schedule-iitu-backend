package routing

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.Config
import reactivemongo.api.DefaultDB
import service.FreeRoomActor

import scala.concurrent.{ExecutionContextExecutor, Future}

class RestApi(timeout: Timeout,
              mongoDBManager: ActorRef,
              mongoDatabase: Future[DefaultDB])
             (implicit system: ActorSystem,
              config: Config) extends Routes {

  implicit val requestTimeout: Timeout = timeout

  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  def createFreeRoomActor(): ActorRef = system.actorOf(FreeRoomActor.props(mongoDBManager, mongoDatabase))

}