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

package io.deepsense.deeplang

import scala.concurrent.Future

import io.deepsense.commons.models.Id

trait CustomOperationExecutor {
  import io.deepsense.deeplang.CustomOperationExecutor.Result

  /**
   * Executes custom operation. The result future will be completed when operation execution ends.
   * @param workflowId Id of the workflow.
   * @param nodeId Id of the node.
   * @return Future, which will be completed when operation execution ends.
   */
  def execute(workflowId: Id, nodeId: Id): Future[Result]
}

object CustomOperationExecutor {
  type Error = String
  type Result = Either[Error, Unit]
}
