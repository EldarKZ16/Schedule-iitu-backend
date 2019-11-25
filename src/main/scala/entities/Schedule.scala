package entities

case class Schedule(day: String, timetable: Map[String, Set[String]])