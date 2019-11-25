package routing

import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import serialization.Json4s

trait Routes extends RoutesHelper with Json4s {
  val service = "api"
  val version = "v1"

  val routes: Route = pathPrefix(service / version) {
    concat(
      pathPrefix("schedule") {
        concat(
          pathPrefix("room") {
            pathEndOrSingleSlash {
              get {
                parameter('day.as[Int]) { day =>
                  onSuccess(getSchedule(day)) { schedule =>
                    complete(schedule)
                  }
                }
              }
            }
          },
          pathPrefix("clear") {
            pathEndOrSingleSlash {
              post {
                onSuccess(emptyDatabase()) { response =>
                  complete(response)
                }
              }
            }
          },
          pathPrefix("update") {
            pathEndOrSingleSlash {
              post {
                onSuccess(updateDatabase()) {response =>
                  complete(response)
                }
              }
            }
          }
        )
      }
    )
  }
}

