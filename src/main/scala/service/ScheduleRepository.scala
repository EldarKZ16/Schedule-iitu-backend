package service

import akka.actor.ActorSystem
import entities.auth.{AuthContext, AuthCredentials, OAuthToken}
import entities.domain.{Account, EmptyCabinets, User}
import entities.http.Response
import org.slf4j.LoggerFactory
import reactivemongo.api.DefaultDB
import service.actors.EmptyCabinetStreamer
import service.dao.mongo.{AccountDAOImpl, AuthContextDAOImpl, EmptyCabinetsDAOImpl, UserDAOImpl}
import service.dao.{AccountDAO, AuthContextDAO, EmptyCabinetsDAO, UserDAO}
import utils.Utils
import com.github.t3hnar.bcrypt._

import scala.concurrent.duration._
import scala.collection.immutable.ListMap
import scala.concurrent.{Await, Future}

object ScheduleRepository {
  private var scheduleRepository: Option[ScheduleRepository] = None

  def apply(system: ActorSystem,
            mongoDatabase: DefaultDB): ScheduleRepository = {
    scheduleRepository match {
      case Some(value) =>
        value
      case None =>
        val newInst = new ScheduleRepository(system, mongoDatabase)
        scheduleRepository = Some(newInst)
        newInst
    }
  }
}

class ScheduleRepository private(system: ActorSystem,
                                 mongoDatabase: DefaultDB)
  extends Repository {

  import system.dispatcher

  private val log = LoggerFactory.getLogger(this.getClass)
  private val userDAO: UserDAO = new UserDAOImpl(mongoDatabase)
  private val emptyCabinetsDAO: EmptyCabinetsDAO = new EmptyCabinetsDAOImpl(mongoDatabase)
  private val accountDAO: AccountDAO = new AccountDAOImpl(mongoDatabase)
  private val authContextDAO: AuthContextDAO = new AuthContextDAOImpl(mongoDatabase)

  override def getEmptyCabinets(day: String): Future[Option[EmptyCabinets]] = {
    emptyCabinetsDAO.get(day)
      .map(_.map(emptyCabinet => EmptyCabinets(emptyCabinet.day, ListMap(emptyCabinet.timetable.toSeq.sortBy(_._1.toInt): _*))))
      .map(_.orElse(Some(EmptyCabinets(day, Map()))))
  }

  override def deleteAllEmptyCabinets(): Future[Response] = {
    log.info(s"Deleting all empty cabinets")
    emptyCabinetsDAO.deleteAll(Utils.DAYS)
  }

  override def updateAllEmptyCabinets(): Future[Response] = {
    log.info(s"Updating all empty cabinets")
    val scheduleStreamer = system.actorOf(EmptyCabinetStreamer.props(mongoDatabase))
    scheduleStreamer ! EmptyCabinetStreamer.RetrieveCabinet
    Future {
      Response()
    }
  }

  override def addUser(user: User): Future[Response] = {
    log.info(s"Adding new user, userID: ${user.id} and groupID: ${user.groupId}")
    userDAO.add(user)
  }

  override def getUser(userId: Int): Future[Option[User]] = {
    userDAO.get(userId).map(_.orElse(Some(User(userId, -1))))
  }

  override def addAccount(account: Account): Future[Response] = {
    log.info(s"Received add new account request for id: ${account.id}")
    accountDAO.add(account.copy(password = account.password.bcrypt)).recover {
      case e =>
        log.error(s"Couldn't add new account, exception: ${e.getLocalizedMessage}")
        Response(message = e.getLocalizedMessage)
    }
  }

  override def getAllAuthContext(): Future[List[AuthContext]] = authContextDAO.getAll()

  override def login(credentials: AuthCredentials): Future[Either[Response, OAuthToken]] = {
    log.info(s"Received login request for accountId: ${credentials.accountId}")
    val retrievedCtx = Await.result(authContextDAO.get(credentials.accountId), 20.seconds)

    retrievedCtx match {
      case Some(ctx) if ctx.attempts == 0 && ctx.loggedInAt.plusMinutes(30).isAfterNow =>
        Future.successful(Left(Response(400, "You've made too many attempts, try to authenticate after 30 minutes")))
      case Some(ctx) =>
        val retrievedUser = Await.result(accountDAO.get(credentials.accountId), 20.seconds)
        retrievedUser match {
          case Some(account) =>
            if (credentials.password.isBcrypted(account.password)) {
              val ctx = AuthContext(account.id)
              authContextDAO.update(ctx).map(Right(_)).recover {
                case e =>
                  log.error(s"Couldn't add new authContext, exception: ${e.getLocalizedMessage}")
                  Left(Response(message = e.getLocalizedMessage))
              }
            } else {
              authContextDAO.update(ctx.copy(attempts = ctx.attempts - 1))
                .map(_ => Left(Response(400, "Password is incorrect, try again")))
            }
          case None =>
            Future.successful(Left(Response(400, "You don't access to this bot, contact to the owner")))
        }
      case None =>
        val retrievedUser = Await.result(accountDAO.get(credentials.accountId), 20.seconds)
        retrievedUser match {
          case Some(account) =>
              if (credentials.password.isBcrypted(account.password)) {
                val ctx = AuthContext(account.id)
                authContextDAO.update(ctx).map(Right(_)).recover {
                  case e =>
                    log.error(s"Couldn't add new authContext, exception: ${e.getLocalizedMessage}")
                    Left(Response(message = e.getLocalizedMessage))
                }
              } else {
                Future.successful(Left(Response(400, "Password is incorrect, try again")))
              }
          case None =>
            Future.successful(Left(Response(400, "You don't access to this bot, contact to the owner")))
        }
    }
  }
}
