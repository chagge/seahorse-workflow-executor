/**
 * Copyright 2015, deepsense.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.workflowexecutor.pythongateway

import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.{SparkConf, SparkContext}

import io.deepsense.commons.utils.Logging
import io.deepsense.deeplang._
import io.deepsense.workflowexecutor.pythongateway.PythonEntryPoint.PythonEntryPointConfig

/**
  * An entry point to our application designed to be accessible by Python process.
  */
class PythonEntryPoint(
    val pythonEntryPointConfig: PythonEntryPointConfig,
    val sparkContext: SparkContext,
    val sqlContext: SQLContext,
    val dataFrameStorage: DataFrameStorage,
    val operationExecutionDispatcher: OperationExecutionDispatcher)
  extends Logging {

  import io.deepsense.workflowexecutor.pythongateway.PythonEntryPoint._

  def getSparkContext: JavaSparkContext = sparkContext

  def getSqlContext: SQLContext = sqlContext

  def getSparkConf: SparkConf = sparkContext.getConf

  private val codeExecutor: AtomicReference[Promise[PythonCodeExecutor]] =
    new AtomicReference(Promise())

  private val pythonPort: AtomicReference[Promise[Int]] =
    new AtomicReference(Promise())

  def getCodeExecutor: PythonCodeExecutor =
    getFromPromise(codeExecutor.get, pythonEntryPointConfig.pyExecutorSetupTimeout)

  def getPythonPort: Int =
    getFromPromise(pythonPort.get, pythonEntryPointConfig.pyExecutorSetupTimeout)

  def registerCodeExecutor(newCodeExecutor: PythonCodeExecutor): Unit =
    replacePromise(codeExecutor, newCodeExecutor)

  def registerCallbackServerPort(newPort: Int): Unit =
    replacePromise(pythonPort, newPort)

  def retrieveInputDataFrame(workflowId: String, nodeId: String, portNumber: Int): DataFrame =
    dataFrameStorage.getInputDataFrame(workflowId, nodeId, portNumber).get

  def retrieveOutputDataFrame(workflowId: String, nodeId: String, portNumber: Int): DataFrame =
    dataFrameStorage.getOutputDataFrame(workflowId, nodeId, portNumber).get

  def registerOutputDataFrame(
      workflowId: String, nodeId: String, portNumber: Int, dataFrame: DataFrame): Unit =
    dataFrameStorage.setOutputDataFrame(workflowId, nodeId, portNumber, dataFrame)

  def executionCompleted(workflowId: String, nodeId: String): Unit =
    operationExecutionDispatcher.executionEnded(workflowId, nodeId, Right())

  def executionFailed(workflowId: String, nodeId: String, error: String): Unit =
    operationExecutionDispatcher.executionEnded(workflowId, nodeId, Left(error))
}

object PythonEntryPoint {
  private case class PromiseReplacedException() extends Exception

  @tailrec
  private def getFromPromise[T](promise: => Promise[T], timeout: Duration): T = {
    try {
      Await.result(promise.future, timeout)
    } catch {
      case e: TimeoutException => throw e
      case e: PromiseReplacedException => getFromPromise(promise, timeout)
    }
  }

  private def replacePromise[T](promise: AtomicReference[Promise[T]], newValue: T): Unit = {
    val oldPromise = promise.getAndSet(Promise.successful(newValue))
    try {
      oldPromise.failure(new PromiseReplacedException)
    } catch {
      // The oldPromise will have been completed always, except for the first time.
      // The illegal state is expected, but we have to complete the oldPromise,
      // since someone might be waiting on it.
      case e: IllegalStateException => ()
    }
  }

  case class PythonEntryPointConfig(
    pyExecutorSetupTimeout: Duration = 5.seconds)
}
