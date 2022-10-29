package de.rwth.gross.sampling

import de.rwth.gross.util.{DataModel, Electric}
import org.apache.flink.api.common.functions.{FlatMapFunction, FoldFunction, MapFunction, ReduceFunction}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.util.Random

class EDSSampler[IN: ClassTag : TypeInformation] {
  var X0: Double = 0.5
  var P0:Double = 1.0
  var alpha:Double = 0.9
  var rnd = new Random()

  //  SET PARAMETERS

  /**
    * Sets the lower X0 bound of the sampler
    * @param X0
    * @return
    */
  def setLowerBound(X0:Double) : EDSSampler[IN] = {
    this.X0 = X0
    this
  }

  /**
    * Sets the decay ratio of the sampler
    * @param Alpha
    * @return
    */
  def setDecayRate(Alpha:Double) : EDSSampler[IN] = {
    this.alpha = Alpha
    this
  }

  // METHODS


  /**
    * Performs the sampling operation on the DataStream
    * @param data
    * @return
    */
  def sample(data: DataStream[IN]) = {
    // perform sampling operation on flink platform

    data.flatMap(new DistributedSampler[IN](X0, P0, alpha, rnd)).map(new KVStream[IN]())
     .keyBy(1).reduce(new SampleMerge[IN]()).map(new ListTransformer[IN]())
  }

}


/**
  * Distributed Sampling operation
  * @param X0
  * @param P0
  * @param alpha
  * @param rnd
  */
private class DistributedSampler[IN](X0:Double, P0:Double, alpha:Double, rnd:Random) extends FlatMapFunction[IN, List[(IN,Double)]] {
  case class Element(data:IN, priority:Double)
  def elementOrder(e:Element) = -e.priority

  val collection = mutable.PriorityQueue.empty[Element](Ordering.by(elementOrder))
  var time:Int = 1
  var sampledItems:Int = 0

  //max sample size
  var maxSize:Int = math.floor(1.0+(math.log(X0/P0)/math.log(alpha))).toInt

  override def flatMap(value: IN, out: Collector[List[(IN, Double)]]): Unit = {
    sample(value)
    sampledItems = sampledItems+1

    //filter old elements
    filter()

    if( sampledItems >= maxSize) {
      sampledItems = 0
      val list = collection.toList.map(e => (e.data, e.priority))
      out.collect(list)
    }
  }

  //perform sampling step
  def sample(data: IN) = {
    val prio:Double = rnd.nextDouble() * P0/Math.pow(alpha, time)
    collection += Element(data.asInstanceOf[IN], prio)
  }

  //filter out old elements
  def filter() = {
    //increase time step
    time = time+1

    while(!collection.isEmpty && collection.head.priority < X0/Math.pow(alpha, time)){
      collection.dequeue()
    }
  }
}

private class KVStream[IN]() extends MapFunction[List[(IN, Double)], (List[(IN, Double)], Int)] {
  override def map(value: List[(IN, Double)]): (List[(IN, Double)], Int) = {
    return (value, 1)
  }
}

private class ListTransformer[IN]() extends MapFunction[(List[(IN, Double)], Int), List[IN]] {
  override def map(value: (List[(IN, Double)], Int)): List[IN] = {
    val list = value._1.map(e => e._1)
    return list
  }
}

// TODO: Implement Stream Merger
private class SampleMerge[IN]() extends ReduceFunction[(List[(IN, Double)],Int)] {
  override def reduce(value1: (List[(IN, Double)], Int), value2: (List[(IN, Double)], Int)): (List[(IN, Double)], Int) = {
    return value2
  }
}

