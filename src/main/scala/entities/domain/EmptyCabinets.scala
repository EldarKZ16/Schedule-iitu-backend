package entities.domain

import scala.collection.SortedSet

case class EmptyCabinets(day: String, timetable: Map[String, SortedSet[String]])
