package service.dao.mongo

import akka.http.scaladsl.model.StatusCodes
import entities.domain.User
import entities.http.Response
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}
import service.dao.UserDAO
import utils.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserDAOImpl(override val mongoDatabase: DefaultDB) extends UserDAO with MongoDB[User] {

  override implicit val reader: BSONDocumentReader[User] = Macros.reader[User]
  override implicit val writer: BSONDocumentWriter[User] = Macros.writer[User]

  override val bsonCollection: BSONCollection = mongoDatabase.collection(s"${Utils.USER_COLLECTION}")

  def add(user: User): Future[Response] = {
    val selector = BSONDocument("id" -> user.id)

    val modifier = BSONDocument("$set" -> BSONDocument("groupId" -> user.groupId))

    val updateResult: Future[UpdateWriteResult] = bsonCollection.update.one(
      q = selector,
      u = modifier,
      upsert = true
    )

    updateResult
      .map(_ => Response())
      .recover {
        case e => Response(StatusCodes.InternalServerError.intValue, e.getLocalizedMessage)
      }
  }

  def get(userId: Int): Future[Option[User]] = {
    bsonCollection
      .find(document("id" -> userId), None)
      .one
      .mapTo[Option[User]]
  }

}
