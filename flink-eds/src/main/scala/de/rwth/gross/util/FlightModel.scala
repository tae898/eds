package de.rwth.gross.util

import scala.collection.mutable
import scala.collection.parallel.immutable

case class FlightModel(id: String, year: Int, month: Int,
                       dayOfMonth: Int, dayOfWeek: Int,
                       depTime: String, CRSDepTime: String,
                       arrTime: String, CRSArrTime: String,
                       uniqueCarrier: String,
                       flightNum: String,
                       tailNum: String,
                       actualElapsedTime: Int,
                       CRSElapsedTime: Int, airTime: Int,
                       arrDelay: Int, depDelay: Int,
                       origin: String,
                       dest: String,
                       distance: Int,
                       taxiIn: Int,
                       taxiOut: Int,
                       cancelled: Boolean,
                       reasonForCancellation: String,
                       diverted: Boolean, onTime:Boolean) {

}

object FlightModel {
  val iataCodes = collection.immutable.HashMap(
    "AA" -> 1.0,
    "AS" -> 2.0,
    "B6" -> 3.0,
    "DL" -> 4.0,
    "EV" -> 5.0,
    "F9" -> 6.0,
    "HA" -> 7.0,
    "MQ" -> 8.0,
    "NK" -> 9.0,
    "OO" -> 10.0,
    "UA" -> 11.0,
    "US" -> 12.0,
    "VX" -> 13.0,
    "WN" -> 14.0
  )

  def string2Double(airport:String):Double = {
    var r = 0.0
    airport.map(c => r = r+c.toDouble)
    return r
  }


}
