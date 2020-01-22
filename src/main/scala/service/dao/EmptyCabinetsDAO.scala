package service.dao

import entities.domain.EmptyCabinets
import entities.http.Response

import scala.concurrent.Future

trait EmptyCabinetsDAO {
  def add(day: String, time: String, cabinet: String): Future[Response]

  def deleteAll(days: Seq[String]): Future[Response]

  def delete(day: String): Future[Response]

  def get(day: String): Future[Option[EmptyCabinets]]
}
