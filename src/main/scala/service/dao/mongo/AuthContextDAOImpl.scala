package service.dao.mongo

import entities.auth.{AuthContext, OAuthToken}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.{Cursor, DefaultDB}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}
import serialization.BSONSerialization
import service.dao.AuthContextDAO
import utils.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthContextDAOImpl(override val mongoDatabase: DefaultDB)
  extends AuthContextDAO
    with MongoDB[AuthContext]
    with BSONSerialization {

  override implicit val reader: BSONDocumentReader[AuthContext] = Macros.reader[AuthContext]
  override implicit val writer: BSONDocumentWriter[AuthContext] = Macros.writer[AuthContext]
  override val bsonCollection: BSONCollection = mongoDatabase.collection(s"${Utils.TOKEN_COLLECTION}")

  def update(ctx: AuthContext): Future[OAuthToken] = {
    val selector = BSONDocument("accountId" -> ctx.accountId)

    val updateResult: Future[UpdateWriteResult] = bsonCollection.update.one(
      q = selector,
      u = ctx,
      upsert = true
    )

    updateResult
      .map(_ => ctx.oAuthToken)
  }

  def get(accountId: String): Future[Option[AuthContext]] = {
    bsonCollection
      .find(document("accountId" -> accountId))
      .one
      .mapTo[Option[AuthContext]]
  }

  def getAll(): Future[List[AuthContext]] =
    bsonCollection
      .find(document())
      .cursor[AuthContext]()
      .collect[List](-1, Cursor.FailOnError[List[AuthContext]]())

}