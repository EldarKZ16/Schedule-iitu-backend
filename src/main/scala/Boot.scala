import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import reactivemongo.api.{AsyncDriver, DefaultDB}
import routing.ScheduleRoutes
import service.actors.{EmptyCabinetUpdater, Scheduler}
import service.{Repository, ScheduleRepository}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

object Boot extends App with ScheduleRoutes {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.global

  val host = config.getString("api.host")
  val port = config.getInt("api.port")
  val hostname = config.getString("api.hostname")
  val timeout = Timeout.durationToTimeout(config.getInt("api.request-timeout").seconds)

  // MongoDB configuration
  val mongoPrefix = config.getString("mongo.prefix")
  val mongoHost = config.getString("mongo.host")
  val database = config.getString("mongo.database")
  val user = config.getString("mongo.user")
  val password = config.getString("mongo.password")

  val mongoUri = s"$mongoPrefix://$user:$password@$mongoHost/$database"
  val driver = new AsyncDriver()
  //  val connection = driver.connect(mongoUri)
  //  val futureConnection = Future.fromTry(connection)

  val mongoDatabaseFuture = for {
    con <- driver.connect(mongoUri)
    db <- con.database(database)
  } yield db

  //  val mongoDatabaseFuture: Future[DefaultDB] = futureConnection.flatMap(_.database(s"$database"))
  val mongoDatabase: DefaultDB = Await.result(mongoDatabaseFuture, 10.seconds)

  log.info("Start schedulers...")
  val scheduler = system.actorOf(Scheduler.props(hostname))
  val scheduleAutoUpdater = system.actorOf(EmptyCabinetUpdater.props(mongoDatabase))

  override def repository: Repository = ScheduleRepository(system, mongoDatabase)

  val apiVersion = Try(config.getString("api.version")).getOrElse("v1")
  val routes = concat(
    path("healthcheck") {
      get {
        complete("OK")
      }
    },
    pathPrefix("api" / apiVersion) {
      scheduleRoutes
    }
  )

  Http().bindAndHandle(routes, host, port)

  log.info(s"Schedule server API server running at $host:$port")
  Await.result(system.whenTerminated, Duration.Inf)
}
