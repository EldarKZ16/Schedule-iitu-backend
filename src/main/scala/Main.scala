import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import routing.RestApi
import service.{MongoDBManager, Scheduler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

object Main extends App {

  implicit val config: Config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val log = LoggerFactory.getLogger(this.getClass)

  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val hostname = config.getString("http.hostname")
  val timeout: Timeout = Timeout.durationToTimeout(config.getInt("http.request-timeout").seconds)

  // MongoDB configuration
  val mongoHost = config.getString("mongo.host")
  val database = config.getString("mongo.database")
  val user = config.getString("mongo.user")
  val password = config.getString("mongo.password")

  val mongoUri = s"mongodb://$user:$password@$mongoHost/$database"
  val driver = MongoDriver()
  val parsedURI = MongoConnection.parseURI(mongoUri)
  val connection = parsedURI.flatMap(driver.connection(_, strictUri = true))
  val futureConnection = Future.fromTry(connection)

  def mongoDatabase: Future[DefaultDB] = futureConnection.flatMap(_.database(s"$database"))

  val mongoDBManager = system.actorOf(Props[MongoDBManager])
  val scheduler = system.actorOf(Scheduler.props(system, hostname))

  val api = new RestApi(timeout, mongoDBManager, mongoDatabase)

  val routes = concat(
    path("healthcheck") {
      get {
        complete("OK")
      }
    },
    api.routes
  )

  Http().bindAndHandle(routes, host, port)

  log.info(s"Schedule server API server running at $host:$port")
  Await.result(system.whenTerminated, Duration.Inf)
}

