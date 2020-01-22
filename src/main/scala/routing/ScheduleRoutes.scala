package routing

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import entities.domain.User
import serialization.Json4sSerialization

trait ScheduleRoutes extends ScheduleRoutesHelper with Json4sSerialization {

  val scheduleRoutes: Route = {
    concat(
      pathPrefix("schedule") {
        concat(
          pathPrefix("room") {
            pathEndOrSingleSlash {
              get {
                parameter('day.as[String]) { day =>
                  onSuccess(getEmptyCabinets(day)) { schedule =>
                    complete(schedule)
                  }
                }
              }
            }
          },
          pathPrefix("clear") {
            pathEndOrSingleSlash {
              post {
                onSuccess(deleteAllEmptyCabinets()) { response =>
                  complete(response)
                }
              }
            }
          },
          pathPrefix("update") {
            pathEndOrSingleSlash {
              post {
                onSuccess(updateAllEmptyCabinets()) { response =>
                  complete(response)
                }
              }
            }
          },
          pathPrefix("user") {
            pathEndOrSingleSlash {
              concat(
                post {
                  entity(as[User]) { user =>
                    onSuccess(addUser(user)) { response =>
                      complete(response)
                    }
                  }
                },
                get {
                  parameters('id.as[Int]) { userId =>
                    onSuccess(getUser(userId)) { user =>
                      complete(user)
                    }
                  }
                }
              )
            }
          }
        )
      }
    )
  }
}

