package serialization

import org.joda.time.{DateTime, LocalDate}
import reactivemongo.bson.{BSONDateTime, BSONHandler}

trait BSONSerialization {
  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(time: BSONDateTime) = new DateTime(time.value)
    def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
  }

  implicit object BSONLocalDateHandler extends BSONHandler[BSONDateTime, LocalDate] {
    def read(time: BSONDateTime) = new LocalDate(time.value)
    def write(ltime: LocalDate) = BSONDateTime(ltime.toDateTimeAtStartOfDay.plusHours(6).getMillis)
  }
}