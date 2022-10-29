package de.rwth.gross.util

import org.apache.flink.api.scala.DataSet
import org.apache.flink.ml.common._
import org.apache.flink.ml.optimization.{GenericLossFunction, GradientDescent, HingeLoss, LinearPrediction}
import org.apache.flink.ml.pipeline.{FitOperation, PredictOperation, Predictor}
import org.apache.flink.ml.math.{Vector}
import org.apache.flink.ml.math.Breeze._
import org.apache.flink.ml.optimization.LearningRateMethod.LearningRateMethodTrait

class SGDSVM extends Predictor[SGDSVM]{
  import SGDSVM._

  /** Stores the learned weight vector after the fit operation */
  var weightsOption: Option[DataSet[WeightVector]] = None

  /** Sets the number of outer iterations
    *
    * @param iterations the maximum number of iterations of the outer loop method
    * @return itself
    */
  def setIterations(iterations: Int): SGDSVM = {
    parameters.add(Iterations, iterations)
    this
  }


  def setConvergenceThreshold(convergenceThreshold: Double): SGDSVM = {
    parameters.add(ConvergenceThreshold, convergenceThreshold)
    this
  }

  def setLearningRateMethod(
                             learningRateMethod: LearningRateMethodTrait)
  : SGDSVM = {
    parameters.add(LearningRateMethodValue, learningRateMethod)
    this
  }



  /** Sets the threshold above which elements are classified as positive.
    *
    * The [[predict ]] and [[evaluate]] functions will return +1.0 for items with a decision
    * function value above this threshold, and -1.0 for items below it.
    * @param threshold the limiting value for the decision function above which examples are
    *                  labeled as positive
    * @return itself
    */
  def setThreshold(threshold: Double): SGDSVM = {
    parameters.add(ThresholdValue, threshold)
    this
  }

  /** Sets whether the predictions should return the raw decision function value or the
    * thresholded binary value.
    *
    * When setting this to true, predict and evaluate return the raw decision value, which is
    * the distance from the separating hyperplane.
    * When setting this to false, they return thresholded (+1.0, -1.0) values.
    *
    * @param outputDecisionFunction When set to true, [[predict ]] and [[evaluate]] return the raw
    *                               decision function values. When set to false, they return the
    *                               thresholded binary values (+1.0, -1.0).
    * @return itself
    */
  def setOutputDecisionFunction(outputDecisionFunction: Boolean): SGDSVM = {
    parameters.add(OutputDecisionFunction, outputDecisionFunction)
    this
  }

}


object SGDSVM {

  val lossFunction = GenericLossFunction(HingeLoss, LinearPrediction)

  // ====================================== Parameters =============================================

  case object Iterations extends Parameter[Int] {
    val defaultValue = Some(10)
  }

  case object Stepsize extends Parameter[Double] {
    val defaultValue = Some(0.1)
  }

  case object ConvergenceThreshold extends Parameter[Double] {
    val defaultValue = None
  }

  case object LearningRateMethodValue extends Parameter[LearningRateMethodTrait] {
    val defaultValue = None
  }



  case object ThresholdValue extends Parameter[Double] {
    val defaultValue = Some(0.0)
  }

  case object OutputDecisionFunction extends Parameter[Boolean] {
    val defaultValue = Some(false)
  }


  // ======================================== Factory methods ======================================

  def apply(): SGDSVM = {
    new SGDSVM()
  }

  // ========================================== Operations =========================================


  /** Trains SGSVM model to fit the training data. The resulting weight vector is stored in
    * the [[SGDSVM]] instance.
    *
    */
  implicit def fitSGDSVM = new FitOperation[SGDSVM, LabeledVector] {
    override def fit(
                      instance: SGDSVM,
                      fitParameters: ParameterMap,
                      input: DataSet[LabeledVector]): Unit = {

      val paras = instance.parameters ++ fitParameters

      //retrieve parameters of the algorithm
      val numberOfIterations = paras(Iterations)
      val stepsize = paras(Stepsize)
      val convergenceThreshold = paras.get(ConvergenceThreshold)
      val learningRateMethod = paras.get(LearningRateMethodValue)


      val lossFunction = GenericLossFunction(HingeLoss, LinearPrediction)

      val optimizer = GradientDescent()
        .setIterations(numberOfIterations)
        .setStepsize(stepsize)
        .setLossFunction(lossFunction)

      convergenceThreshold match {
        case Some(threshold) => optimizer.setConvergenceThreshold(threshold)
        case None =>
      }

      learningRateMethod match {
        case Some(method) => optimizer.setLearningRateMethod(method)
        case None =>
      }

      instance.weightsOption = Some(optimizer.optimize(input, None))

    }
  }



  /** Provides the operation that makes the predictions for individual examples.
    *
    * @tparam T Input data type which is a subtype of [[Vector]]
    * @return A PredictOperation, through which it is possible to predict a value, given a
    *         feature vector
    */
  implicit def predictVectors[T <: Vector] = {
    new PredictOperation[SGDSVM, WeightVector, T, Double](){

      var thresholdValue: Double = _
      var outputDecisionFunction: Boolean = _

      override def getModel(self: SGDSVM, predictParameters: ParameterMap): DataSet[WeightVector] = {
        thresholdValue = predictParameters(ThresholdValue)
        outputDecisionFunction = predictParameters(OutputDecisionFunction)
        self.weightsOption match {
          case Some(model) => model
          case None => {
            throw new RuntimeException("The SVM model has not been trained. Call first fit" +
              "before calling the predict operation.")
          }
        }
      }

      override def predict(value: T, model: WeightVector): Double = {
        import org.apache.flink.ml.math.Breeze._

        val WeightVector(weights, weight0) = model
        val rawValue = value.asBreeze dot weights.asBreeze
        rawValue + weight0

        if (outputDecisionFunction) {
          rawValue
        } else {
          if (rawValue > thresholdValue) 1.0 else -1.0
        }
      }
    }
  }


}
