import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import routing.RestApi
import service.MongoDBManager

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

object Main extends App {

  implicit val config: Config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val log = LoggerFactory.getLogger(this.getClass)

  // MongoDB configuration
  val mongoHost = config.getString("mongo.host")
  val database = config.getString("mongo.database")
  val collection = config.getString("mongo.collection")
  val authMode = config.getString("mongo.authMode")
  val user = config.getString("mongo.user")
  val password = config.getString("mongo.password")

  val mongoUri = s"mongodb://$user:$password@$mongoHost/$database?authenticationMechanism=$authMode"
  val driver = MongoDriver()
  val parsedURI = MongoConnection.parseURI(mongoUri)
  val connection = parsedURI.flatMap(driver.connection(_, strictUri = false))
  val futureConnection = Future.fromTry(connection)

  def mongoDatabase: Future[DefaultDB] = futureConnection.flatMap(_.database(s"$database"))
  def bsonCollection: Future[BSONCollection] = mongoDatabase.map(_.collection(s"$collection"))

  val mongoDBManager = system.actorOf(Props(new MongoDBManager(bsonCollection)))

  val timeout: Timeout = Timeout.durationToTimeout(config.getInt("http.request-timeout").seconds)
  val api = new RestApi(timeout, mongoDBManager, bsonCollection)

  val routes = concat(
    path("healthcheck") {
      get {
        complete("OK")
      }
    },
    api.routes
  )

  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  Http().bindAndHandle(routes, host, port)

  log.info(s"Schedule server API server running at $host:$port")
  Await.result(system.whenTerminated, Duration.Inf)
}

