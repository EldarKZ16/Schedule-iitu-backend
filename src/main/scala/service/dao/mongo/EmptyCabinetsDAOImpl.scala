package service.dao.mongo

import akka.http.scaladsl.model.StatusCodes
import entities.domain.EmptyCabinets
import entities.http.Response
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}
import service.dao.EmptyCabinetsDAO
import utils.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmptyCabinetsDAOImpl(override val mongoDatabase: DefaultDB) extends EmptyCabinetsDAO with MongoDB[EmptyCabinets] {

  override implicit val reader: BSONDocumentReader[EmptyCabinets] = Macros.reader[EmptyCabinets]
  override implicit val writer: BSONDocumentWriter[EmptyCabinets] = Macros.writer[EmptyCabinets]

  override val bsonCollection: BSONCollection = mongoDatabase.collection(s"${Utils.EMPTY_CABINET_COLLECTION}")

  def add(day: String, time: String, cabinet: String): Future[Response] = {
    val selector = BSONDocument("day" -> day)
    val modifier = BSONDocument(
      "$addToSet" -> BSONDocument(
        s"timetable.$time" -> cabinet)
    )

    val updateResult =
      bsonCollection
        .update
        .one(
          q = selector,
          u = modifier,
          upsert = true)

    updateResult
      .map(_ => Response())
      .recover {
        case e => Response(StatusCodes.InternalServerError.intValue, e.getLocalizedMessage)
      }
  }

  def deleteAll(days: Seq[String]): Future[Response] = {
    val deleteBuilder: bsonCollection.DeleteBuilder = bsonCollection.delete(ordered = false)

    val deletingDays = days.map(day =>
      deleteBuilder.element(q = BSONDocument("day" -> day))
    )
    val deleteElementsList = Future.sequence(deletingDays)

    val deleteAllResult = deleteElementsList.flatMap { ops => deleteBuilder.many(ops) }

    deleteAllResult
      .map(_ => Response())
      .recover {
        case e => Response(StatusCodes.InternalServerError.intValue, e.getLocalizedMessage)
      }
  }

  def delete(day: String): Future[Response] = {
    val selector = BSONDocument("day" -> day)

    val deleteResult = bsonCollection.delete.one(selector)

    deleteResult
      .map(_ => Response())
      .recover {
        case e => Response(StatusCodes.InternalServerError.intValue, e.getLocalizedMessage)
      }
  }

  def get(day: String): Future[Option[EmptyCabinets]] = {
    bsonCollection
      .find(document("day" -> day), None)
      .one
      .mapTo[Option[EmptyCabinets]]
  }
}
