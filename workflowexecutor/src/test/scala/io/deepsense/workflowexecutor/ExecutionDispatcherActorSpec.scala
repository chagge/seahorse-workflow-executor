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

package io.deepsense.workflowexecutor

import akka.actor.{ActorContext, ActorRef, ActorSelection, ActorSystem}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.apache.spark.SparkContext
import org.scalatest.concurrent.{Eventually, ScalaFutures, ScaledTimeSpans}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import io.deepsense.deeplang.ExecutionContext
import io.deepsense.deeplang.catalogs.doperable.DOperableCatalog
import io.deepsense.deeplang.doperables.ReportLevel
import io.deepsense.deeplang.doperables.ReportLevel.ReportLevel
import io.deepsense.models.workflows.Workflow
import io.deepsense.workflowexecutor.communication.Connect
import io.deepsense.workflowexecutor.rabbitmq.WorkflowConnect


class ExecutionDispatcherActorSpec
  extends TestKit(ActorSystem("ExecutionDispatcherActorSpec"))
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers
  with MockitoSugar
  with Eventually
  with ScalaFutures
  with ScaledTimeSpans {

  "ExecutionDispatcherActor" should {
    "create WorkflowExecutorActor" when {
      "received Connect messages for non existing actor" in {
        val (workflowId, testProbe, dispatcher) = fixtureWithNonExistingExecutor
        val connect: Connect = Connect(workflowId)
        val msg = WorkflowConnect(connect, testProbe.ref.path)
        dispatcher ! msg
        eventually {
          testProbe.expectMsg(connect)
        }
      }
    }
    "pass Connect to WorkflowExecutorActor" in {
      val (workflowId, testProbe, dispatcher) = fixtureWithExistingExecutor
      val connect: Connect = Connect(workflowId)
      val msg = WorkflowConnect(connect, testProbe.ref.path)
      dispatcher ! msg
      eventually {
        testProbe.expectMsg(connect)
      }
    }
  }

  def fixtureWithExistingExecutor:
      (Workflow.Id, TestProbe, TestActorRef[ExecutionDispatcherActor]) = {
    val expectedWorkflowId = Workflow.Id.randomId
    val testProbe = TestProbe()
    val dispatcher: TestActorRef[ExecutionDispatcherActor] =
      TestActorRef(new ExecutionDispatcherActor(
        mock[SparkContext],
        mock[DOperableCatalog],
        mock[ReportLevel],
        TestProbe().ref
      ) with WorkflowExecutorsFactory with WorkflowExecutorFinder {
        override def createExecutor(
          context: ActorContext,
          executionContext: ExecutionContext,
          workflowId: Workflow.Id,
          statusLogger: ActorRef,
          publisher: ActorSelection): ActorRef = {
          fail("Tried to create an executor but it exists!")
        }

        def findFor(context: ActorContext, workflowId: Workflow.Id): Option[ActorRef] =
          Some(testProbe.ref)
      })

    (expectedWorkflowId, testProbe, dispatcher)
  }

  def fixtureWithNonExistingExecutor:
  (Workflow.Id, TestProbe, TestActorRef[ExecutionDispatcherActor]) = {
    val expectedWorkflowId = Workflow.Id.randomId
    val testProbe = TestProbe()
    val dispatcher: TestActorRef[ExecutionDispatcherActor] =
      TestActorRef(new ExecutionDispatcherActorTest(
        mock[SparkContext],
        mock[DOperableCatalog],
        mock[ReportLevel]
      ) with WorkflowExecutorsFactory with WorkflowExecutorFinder {
        override def createExecutor(
          context: ActorContext,
          executionContext: ExecutionContext,
          workflowId: Workflow.Id,
          statusLogger: ActorRef,
          publisher: ActorSelection): ActorRef = {
          workflowId shouldBe expectedWorkflowId
          testProbe.ref
        }

      def findFor(context: ActorContext, workflowId: Workflow.Id): Option[ActorRef] =
        None
    })

    (expectedWorkflowId, testProbe, dispatcher)
  }

  class ExecutionDispatcherActorTest(
      sparkContext: SparkContext,
      dOperableCatalog: DOperableCatalog,
      reportLevel: ReportLevel)
    extends ExecutionDispatcherActor(sparkContext, dOperableCatalog, reportLevel, TestProbe().ref) {

    self: WorkflowExecutorsFactory with WorkflowExecutorFinder =>

    override def createExecutionContext(
      reportLevel: ReportLevel.ReportLevel,
      sparkContext: Option[SparkContext],
      dOperableCatalog: Option[DOperableCatalog]): ExecutionContext = mock[ExecutionContext]
  }
}