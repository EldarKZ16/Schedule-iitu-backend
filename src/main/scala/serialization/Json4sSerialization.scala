package serialization

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, Serialization, jackson}

trait Json4sSerialization extends Json4sSupport {
  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization = jackson.Serialization
}
