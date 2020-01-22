package entities.http

case class Time(start_time: String, end_time: String)

case class ScheduleTime(times: Map[String, Time])
