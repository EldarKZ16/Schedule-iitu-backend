package entities.domain

import org.joda.time.DateTime

case class Account(id: String, password: String, createdAt: DateTime = DateTime.now())
