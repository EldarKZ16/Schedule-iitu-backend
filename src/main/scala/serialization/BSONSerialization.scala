package serialization

import entities.Schedule
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, Macros}

trait BSONSerialization {
  implicit val scheduleWriter: BSONDocumentWriter[Schedule] = Macros.writer[Schedule]
  implicit val scheduleReader: BSONDocumentReader[Schedule] = Macros.reader[Schedule]
}
