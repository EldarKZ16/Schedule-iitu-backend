package service.dao.mongo

import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter}

trait MongoDB[T] {
  def bsonCollection: BSONCollection
  def mongoDatabase: DefaultDB
  implicit def writer: BSONDocumentWriter[T]
  implicit def reader: BSONDocumentReader[T]
}
