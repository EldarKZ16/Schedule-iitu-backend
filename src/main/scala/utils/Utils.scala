package utils

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

object Utils {
  private val config: Config = ConfigFactory.load()

  final val UPDATE_INTERVAL_IN_DAYS: FiniteDuration = config.getInt("schedule.update-interval-days").days

  final val SCHEDULE_REST_TIMETABLE_ROOM_URL: String = config.getString("schedule.room-url")
  final val EMPTY_CABINET_COLLECTION: String = config.getString("mongo.collection.cabinet")
  final val USER_COLLECTION: String = config.getString("mongo.collection.user")
  final val ACCOUNT_COLLECTION: String = config.getString("mongo.collection.account")
  final val TOKEN_COLLECTION: String = config.getString("mongo.collection.token")
  final val TEST_BUNDLE_ID = config.getString("schedule.test-room-id")

  final val DAYS: Seq[String] = for (day <- 1 to 6) yield day.toString
  final val TIMES: Seq[String] = for (time <- 1 to 13) yield time.toString
  final val CABINETS: Seq[String] = for (room <- 276 to 323) yield room.toString
}
