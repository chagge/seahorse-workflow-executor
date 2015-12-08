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

package io.deepsense.workflowexecutor.communication.mq.serialization.json

import java.nio.charset.StandardCharsets

import org.scalatest.mock.MockitoSugar
import spray.json._

import io.deepsense.commons.StandardSpec
import io.deepsense.graph.DirectedGraph
import io.deepsense.models.json.graph.GraphJsonProtocol.GraphReader
import io.deepsense.models.workflows.{ThirdPartyData, Workflow, WorkflowMetadata, WorkflowType}
import io.deepsense.workflowexecutor.communication.message.global.Connect
import io.deepsense.workflowexecutor.communication.message.workflow.{Abort, Init, Launch, UpdateWorkflow}
import io.deepsense.workflowexecutor.executor.Executor

class ProtocolJsonDeserializerSpec
  extends StandardSpec
  with MockitoSugar {

  "ProtocolJsonDeserializer" should {
    "deserialize Launch messages" in {
      val protocolDeserializer = ProtocolJsonDeserializer(mock[GraphReader])

      val workflowId = Workflow.Id.randomId
      val nodesToExecute = Vector(Workflow.Id.randomId, Workflow.Id.randomId, Workflow.Id.randomId)
      val jsNodesToExecute = JsArray(nodesToExecute.map(id => JsString(id.toString)))

      val rawMessage = JsObject(
        "messageType" -> JsString("launch"),
        "messageBody" -> JsObject(
          "workflowId" -> JsString(workflowId.toString),
          "nodesToExecute" -> jsNodesToExecute
        )
      )

      val readMessage: Any = serializeAndRead(protocolDeserializer, rawMessage)

      readMessage shouldBe Launch(workflowId, nodesToExecute.toSet)
    }
    "deserialize Abort messages" in {
      val protocolDeserializer = ProtocolJsonDeserializer(mock[GraphReader])
      val workflowId = Workflow.Id.randomId

      val rawMessage = JsObject(
        "messageType" -> JsString("abort"),
        "messageBody" -> JsObject(
          "workflowId" -> JsString(workflowId.toString)
        )
      )

      val readMessage: Any = serializeAndRead(protocolDeserializer, rawMessage)
      readMessage shouldBe Abort(workflowId)
    }
    "deserialize Connect messages" in {
      val protocolDeserializer = ProtocolJsonDeserializer(mock[GraphReader])
      val workflowId = Workflow.Id.randomId

      val rawMessage = JsObject(
        "messageType" -> JsString("connect"),
        "messageBody" -> JsObject(
          "workflowId" -> JsString(workflowId.toString)
        )
      )

      val readMessage: Any = serializeAndRead(protocolDeserializer, rawMessage)
      readMessage shouldBe Connect(workflowId)
    }
    "deserialize Init messages" in {
      val protocolDeserializer = ProtocolJsonDeserializer(mock[GraphReader])
      val workflowId = Workflow.Id.randomId

      val rawMessage = JsObject(
        "messageType" -> JsString("init"),
        "messageBody" -> JsObject(
          "workflowId" -> JsString(workflowId.toString)
        )
      )

      val readMessage: Any = serializeAndRead(protocolDeserializer, rawMessage)
      readMessage shouldBe Init(workflowId)
    }
    "deserialize UpdateWorkflow messages" in {
      val graphReader = new GraphReader(Executor.createDOperationsCatalog())
      val protocolDeserializer = ProtocolJsonDeserializer(graphReader)

      val rawMessage = JsObject(
        "messageType" -> JsString("updateWorkflow"),
        "messageBody" -> JsObject(
          "metadata" -> JsObject(
            "type" -> JsString("batch"),
            "apiVersion" -> JsString("1.0.0")
          ),
          "workflow" -> JsObject(
            "nodes" -> JsArray(),
            "connections" -> JsArray()
          ),
          "thirdPartyData" -> JsObject()
        )
      )

      val readMessage: Any = serializeAndRead(protocolDeserializer, rawMessage)
      readMessage shouldBe UpdateWorkflow(
        Workflow(WorkflowMetadata(WorkflowType.Batch, "1.0.0"), DirectedGraph(), ThirdPartyData()))
    }
  }

  def serializeAndRead(
      protocolDeserializer: ProtocolJsonDeserializer,
      rawMessage: JsObject): Any = {
    val bytes = rawMessage.compactPrint.getBytes(StandardCharsets.UTF_8)
    protocolDeserializer.deserializeMessage(bytes)
  }
}