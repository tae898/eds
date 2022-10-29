package de.rwth.gross.util

import org.apache.flink.api.common.functions.{FlatMapFunction, MapFunction}
import org.apache.flink.ml.common.LabeledVector
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.util.Collector

class DataModel{

}

case class Electric  (
                   Date:Double,
                   Day:Double,
                   Period:Double,
                   NswPrice:Double,
                   NswDemand:Double,
                   VicPrice:Double,
                   VicDemand:Double,
                   Transfer:Double,
                   Class:Int
                   )  {

}


/**
  * Implements simple line based csv parser and returns Electric
  */
class CSVSplitterElectric extends FlatMapFunction[String, Electric] {
  override def flatMap(value: String, out: Collector[Electric]) = {
    var entries:Array[String] = value.split(",")

    if(entries.length > 0){
      val date = entries(0).toDouble
      val day = entries(1).toDouble
      val period = entries(2).toDouble
      val nswPrice = entries(3).toDouble
      val nswDemand = entries(4).toDouble
      val vicPrice = entries(5).toDouble
      val vicDemand = entries(6).toDouble
      val transfer = entries(7).toDouble
      var category = 1
      if( entries(8).toString.equals("DOWN")){
        category = -1
      }

      val elec:Electric = Electric(date, day, period, nswPrice, nswDemand, vicPrice, vicDemand, transfer, category)
      out.collect(elec)
    }
  }
}

/**
  * Implements transformation method from sampled list to individual LabeledVector
  */
class ElectricPreprocessor extends FlatMapFunction[List[Electric], LabeledVector] {
  override def flatMap(value: List[Electric], out: Collector[LabeledVector]) = {
    val it = value.iterator
    while(it.hasNext) {
      var e = it.next()
      var vec = DenseVector(Array(e.Date,e.Day, e.Period, e.NswPrice, e.NswDemand, e.VicPrice, e.VicDemand, e.Transfer))
      out.collect(new LabeledVector(e.Class, vec))
    }
  }
}
