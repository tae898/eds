package de.rwth.gross.util

import org.apache.flink.streaming.api.operators.{AbstractStreamOperator, OneInputStreamOperator}
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord

import scala.collection.mutable
import scala.util.Random

class EDSOperator[IN] extends AbstractStreamOperator[IN] with OneInputStreamOperator[IN, IN]{

  case class Element(priority:Double, data:StreamRecord[IN])
  def elementOrder(e:Element) = -e.priority

  val collection = mutable.PriorityQueue.empty[Element](Ordering.by(elementOrder))
  var time:Int = 1
  var lastOut:Int = 0

  val X0:Double = 0.5
  val P0:Double = 1.0
  val alpha:Double = 0.999
  val rnd = new Random()

  //max sample size
  var maxSize:Int = math.floor(1.0+(math.log(X0/P0)/math.log(alpha))).toInt


  //new element arrives
  override def processElement(element: StreamRecord[IN]) = {
    sample(element)
  }

  //perform sampling step
  def sample(element: StreamRecord[IN]) = {
    val prio:Double = rnd.nextDouble() * P0/Math.pow(alpha, time)
    collection += Element(prio, element)

    filter()
    out()
  }

  //filter out old elements
  def filter() = {
    //increase time step
    time+1

    var head:Double = collection.head.priority
    while(head < X0/Math.pow(alpha, time)){
      collection.dequeue()
      head = collection.head.priority
    }

  }


  def out() = {
    def onlyData(e:Element) = e.data

    if(time-lastOut > maxSize){
      val list = collection.toList.map(e => onlyData(e)).map(x => output.collect(x))
    }
  }

}
