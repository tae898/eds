package de.rwth.gross.streamlearner

import org.apache.flink.ml.common.{LabeledVector, WeightVector}
import org.apache.flink.ml.math.DenseVector
import org.apache.flink.ml.optimization.LossFunction

class GradientDescent(loss:LossFunction, numberOfIterations: Int, learningRate: Double) {

  def fit(training:List[LabeledVector] ) = {
    //create initial weights vector
    val values = Array.fill(training(0).vector.size)(0.0)
    var weights = WeightVector(DenseVector(values), 0.0)

    val it = training.iterator
    while(it.hasNext){
      var tr = it.next()
      // complete all epochs
      for( step <- 1 to numberOfIterations){
        var (l, wv ) = loss.lossGradient(tr, weights)


        //weights = weights - learningRate
      }

    }


  }



}

object GradientDescent {

}