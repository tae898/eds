package de.rwth.gross

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang

import de.rwth.gross.sampling.EDSSampler
import de.rwth.gross.util._
import org.apache.flink.api.common.functions.{FlatMapFunction, MapFunction, MapPartitionFunction, RichMapFunction}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.{TypeHint, TypeInformation}
import org.apache.flink.api.java.DataSet
import org.apache.flink.configuration.Configuration
import org.apache.flink.ml.common.LabeledVector
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.streaming.api.functions.co.{CoFlatMapFunction, RichCoFlatMapFunction}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord
import org.apache.flink.util.Collector
import smile.classification._
import org.apache.flink.streaming.api.scala._
import smile.data.{Attribute, NominalAttribute, NumericAttribute, StringAttribute}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import org.sgdtk._
import org.sgdtk.DenseVectorN
import org.sgdtk.FeatureVector

/**
 * Skeleton for a Flink Job.
 *
 * You can also generate a .jar file that you can submit on your Flink
 * cluster. Just type
 * {{{
 *   sbt clean assembly
 * }}}
 * in the projects root directory. You will find the jar in
 * target/scala-2.11/Flink\ Project-assembly-0.1-SNAPSHOT.jar
 *
 */
object Job {
  def main(args: Array[String]) {
    // CONFIGURATIONS
    val filePath:String = "data/2008s.csv"
    val filePath2:String = "data/2008s2.csv"
    val hostName = "127.0.0.1"
    val port = 15000

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // perform sampling
    val text = env.socketTextStream(hostName, port)
    val eds = new EDSSampler[Electric]().setLowerBound(0.5).setDecayRate(0.9)

    val transformedStream = text.flatMap(new CSVSplitterElectric())//.flatMap(new EDS[Electric]())
    val sampleStream = eds.sample(transformedStream)

    sampleStream.print()




    //train model
    val modelStream = sampleStream.map(new TrainModelSVM())


    val unclassified = env.socketTextStream(hostName, 15001).flatMap(new CSVSplitterElectric())
    val classify = unclassified.connect(modelStream).flatMap(new StreamPredictor()).setParallelism(1)

    classify.print()


    //          .map( i => (i.length, 1))
    //sampleStream.print()

    //train model using sample (mini batch style)
    //val labledVectors = sampleStream.flatMap(new ElectricPreprocessor()).map(i => (i,1)).keyBy(1).window(GlobalWindows.create())



    val toBroadcast = env.fromElements(1, 2, 3)

  /*
    val model = sampleStream.map(new RichMapFunction[List[Electric], String]() {
      var broadcastSet: Traversable[String] = null

      override def open(config: Configuration): Unit = {
        // 3. Access the broadcasted DataSet as a Collection
        broadcastSet = getRuntimeContext().getBroadcastVariable[String]("broadcastSetName").asScala
      }

    }).withBroadcastSet(toBroadcast, "broadcastSetName") // 2. Broadcast the DataSet
*/
    // train model mini batch style
    /*val modelStream = sampleStream
                .map(new TrainModel).setParallelism(1).reduce( (d1, d2) => d1).collect().head


    //predict new labels
    val predictInput = env.readTextFile(filePath2)
    val labels = predictInput.flatMap(new CSVSpliter()).map { e =>
      implicit def bool2int(b:Boolean) = if (b) 1 else 0
      val input = ArrayBuffer[Double]()
      //pick attributes for training the model
      input += e._1.year.toDouble
      input += e._1.month.toDouble
      input += e._1.dayOfMonth.toDouble
      input += e._1.dayOfWeek.toDouble
      input += e._1.distance.toDouble
      input += (e._1.cancelled: Int).toDouble
      input += FlightModel.string2Double(e._1.uniqueCarrier)
      input += FlightModel.string2Double(e._1.origin)
      input += FlightModel.string2Double(e._1.dest)

      //val out = modelStream.predict(input.toArray)

      (math.abs(out-e._1.onTime:Int), 1)
    }.groupBy(1).sum(0)
    // execute program
    labels.print()*/

    //text.print()
    env.execute("Scala EDS Example")
  }


  private class StreamPredictor extends RichCoFlatMapFunction[Electric, LinearModel, Double] {
    private var model: LinearModel = null

    /*
    override def open(parameters: Configuration): Unit = {
      val descr = new ValueStateDescriptor[LinearModel]("model", TypeInformation.of(new TypeHint[LinearModel]() {}))
      model = getRuntimeContext().getState(descr)
    }
    */

    override def flatMap1(e: Electric, out: Collector[Double]): Unit = {
      if(this.model != null){
        var vec = new org.sgdtk.DenseVectorN(Array(e.Date,e.Day, e.Period, e.NswPrice, e.NswDemand, e.VicPrice, e.VicDemand, e.Transfer))
        var fv = new org.sgdtk.FeatureVector(e.Class, vec)
        out.collect(this.model.predict(fv))
      }
      out.collect(1)
    }

    override def flatMap2(value: LinearModel, out: Collector[Double]): Unit = {
      println("new model")
      model = value
    }
  }



  private class TrainModel extends MapFunction[List[FlightModel], DecisionTree ] {
    implicit def bool2int(b:Boolean) = if (b) 1 else 0

    override def map(values: List[FlightModel]): DecisionTree = {
      //define attributes, which are respected
      var attributes = new ArrayBuffer[smile.data.Attribute]()
      attributes += new NumericAttribute("Year")
      attributes += new NumericAttribute("Month")
      attributes += new NumericAttribute("DayOfMonth")
      attributes += new NumericAttribute("DayOfWeek")
      attributes += new NumericAttribute("Distance")
      attributes += new NumericAttribute("Cancelled")
      attributes += new NumericAttribute("IATA")
      attributes += new NumericAttribute("Origin")
      attributes += new NumericAttribute("Destination")

      //define traning and response set
      var trainingInstances = Array.ofDim[Double](values.length, attributes.size)
      var responseInstances = new Array[Int](values.length)

      //iterate the element in sample and fill trainingInstance/responseInstance
      var i = 0
      for( e <- values) {
        var input = ArrayBuffer[Double]()
        //pick attributes for training the model
        input += e.year.toDouble
        input += e.month.toDouble
        input += e.dayOfMonth.toDouble
        input += e.dayOfWeek.toDouble
        input += e.distance.toDouble
        input += (e.cancelled:Int).toDouble
        input += FlightModel.string2Double(e.uniqueCarrier)
        input += FlightModel.string2Double(e.origin)
        input += FlightModel.string2Double(e.dest)

        trainingInstances(i) = input.toArray
        responseInstances(i) = (e.onTime:Int)
        i=i+1
      }
      //finally train Tree with parameters
      val maxNodes = 30
      val splitRule = DecisionTree.SplitRule.ENTROPY
      val numUnits = Array(9, 9, 9, 9, 9)
      val tree = smile.classification.cart(trainingInstances, responseInstances, maxNodes, attributes.toArray, splitRule)
      //val tree = smile.classification.mlp(trainingInstances, responseInstances, numUnits = numUnits, error = NeuralNetwork.ErrorFunction.CROSS_ENTROPY, activation = NeuralNetwork.ActivationFunction.SOFTMAX)


      return tree
    }
  }


  /**
    * Implements simple line based csv parser and returns FlightModel
    */
  private class CSVSpliterFlight extends FlatMapFunction[String, (FlightModel, Double)] {

    def getInt(i:String):Int = {
      i match {
        case "NA" => 0
        case "" => 0
        case i => i.toInt
      }
    }


    override def flatMap(value: String, out: Collector[(FlightModel, Double)]): Unit = {
      var entries:Array[String] = value.split(",")

      if(entries.length > 0 && entries(1) != "\"Year\""){
        val year = getInt(entries(1))
        val month = getInt(entries(2))
        val dofMonth = getInt(entries(3))
        val dofWeek = getInt(entries(4))
        val acElapsedTime = getInt(entries(12))
        val crseElapsedTime = getInt(entries(13))
        val airTime = getInt(entries(14))
        val arrDelay = getInt(entries(15))
        val depDelay = getInt(entries(16))
        val distance = getInt(entries(19))
        val taxiIn = getInt(entries(20))
        val taxiOut = getInt(entries(21))
        val cancelled = (entries(22).toInt == 1)
        val reason4Cancel = " " //entries[23].charAt(0);
        val diverted:Boolean = (entries(24).toInt == 1)
        val onTime:Boolean = (arrDelay <= 0)

        val flight:FlightModel = FlightModel(
          entries(0),
          year,
          month,
          dofMonth,
          dofWeek,
          entries(5),
          entries(6),
          entries(7),
          entries(8),
          entries(9),
          entries(10),
          entries(11),
          acElapsedTime,
          crseElapsedTime,
          airTime,
          arrDelay,
          depDelay,
          entries(17),
          entries(18),
          distance,
          taxiIn,
          taxiOut,
          cancelled,
          reason4Cancel,
          diverted,
          onTime
        )

        out.collect(flight, 1.0)
      }

    }
  }


  private class EDS[IN] extends FlatMapFunction[IN, List[IN]] {
    override def flatMap(value:IN, out: Collector[List[IN]]): Unit = {
      sample(value)
      sampledItems = sampledItems+1

      //filter old elements
      filter()

      if( sampledItems >= maxSize) {
        sampledItems = 0

        //output current Reservoir
        def onlyData(e:Element) = e.data
        val list = collection.toList.map(e => onlyData(e))
        out.collect(list)
      }
    }

    case class Element(data:IN, priority:Double)
    def elementOrder(e:Element) = -e.priority

    val collection = mutable.PriorityQueue.empty[Element](Ordering.by(elementOrder))
    var time:Int = 1
    var sampledItems:Int = 0

    val X0:Double = 0.5
    val P0:Double = 1.0
    val alpha:Double = 0.9
    val rnd = new Random()

    //max sample size
    var maxSize:Int = math.floor(1.0+(math.log(X0/P0)/math.log(alpha))).toInt


    //perform sampling step
    def sample(element: IN) = {
      val prio:Double = rnd.nextDouble() * P0/Math.pow(alpha, time)
      collection += Element(element, prio)
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

}


private class TrainModelSVM extends MapFunction[List[Electric], LinearModel] {
  import org.sgdtk.Learner
  import org.sgdtk.LinearModelFactory
  import org.sgdtk.ModelFactory
  import org.sgdtk.DenseVectorN
  import org.sgdtk.FeatureVector

  import org.sgdtk.Learner
  import org.sgdtk.LinearModelFactory
  import org.sgdtk.ModelFactory
  import org.sgdtk.SGDLearner

  lazy val modelFactory = new LinearModelFactory
  lazy val learner = new SGDLearner(new HingeLoss(), 1e-5, -1, modelFactory)
  lazy val model = learner.create(8)

  override def map(value: List[Electric]): LinearModel = {
    val it = value.iterator
    while(it.hasNext) {
      var e = it.next()
      var vec = new org.sgdtk.DenseVectorN(Array(e.Date,e.Day, e.Period, e.NswPrice, e.NswDemand, e.VicPrice, e.VicDemand, e.Transfer))
      var fv = new org.sgdtk.FeatureVector(e.Class, vec)
      learner.trainOne(model, fv)
    }
    return model.asInstanceOf[LinearModel]
  }
}
