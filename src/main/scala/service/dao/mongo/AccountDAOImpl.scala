package service.dao.mongo

import entities.domain.Account
import entities.http
import entities.http.Response
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}
import serialization.BSONSerialization
import service.dao.AccountDAO
import utils.Utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AccountDAOImpl(override val mongoDatabase: DefaultDB)
  extends AccountDAO
    with MongoDB[Account]
    with BSONSerialization {

  override implicit val reader: BSONDocumentReader[Account] = Macros.reader[Account]
  override implicit val writer: BSONDocumentWriter[Account] = Macros.writer[Account]
  override val bsonCollection: BSONCollection = mongoDatabase.collection(s"${Utils.ACCOUNT_COLLECTION}")

  def add(newAccount: Account): Future[Response] = {
    val user = Await.result(get(newAccount.id), 30.seconds)
    user match {
      case Some(_) =>
        val response = Response(400, "AccountId is already registered")
        Future.successful(response)
      case None =>
        bsonCollection.insert.one(newAccount).map(_ => Response())
    }
  }

  def get(id: String): Future[Option[Account]] = {
    bsonCollection
      .find(document("id" -> id), None)
      .one
      .mapTo[Option[Account]]
  }


  def update(user: Account): Future[http.Response] = {
    val selector = BSONDocument("id" -> user.id)

    val updateResult: Future[UpdateWriteResult] = bsonCollection.update.one(
      q = selector,
      u = user,
      upsert = true
    )

    updateResult
      .map(_ => Response())
  }

}
