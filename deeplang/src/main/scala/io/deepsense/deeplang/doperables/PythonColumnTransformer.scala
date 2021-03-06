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

package io.deepsense.deeplang.doperables

import java.util.UUID

import org.apache.spark.sql.types._

import io.deepsense.deeplang._
import io.deepsense.deeplang.doperables.dataframe._
import io.deepsense.deeplang.doperations.exceptions.CustomOperationExecutionException
import io.deepsense.deeplang.params.{CodeSnippetLanguage, CodeSnippetParam, Param}
import io.deepsense.deeplang.params.choice.ChoiceParam

case class PythonColumnTransformer() extends MultiColumnTransformer {

  import PythonColumnTransformer._

  val targetType = ChoiceParam[TargetTypeChoice](
    name = "target type",
    description = "Target type of the columns.")

  def getTargetType: TargetTypeChoice = $(targetType)
  def setTargetType(value: TargetTypeChoice): this.type = set(targetType, value)

  val codeParameter = CodeSnippetParam(
    name = "column operation code",
    description = "Column operation source code",
    language = CodeSnippetLanguage(CodeSnippetLanguage.python)
  )
  setDefault(codeParameter -> "def transform_value(value, column_name):\n    return value")

  override def getSpecificParams: Array[Param[_]] =
    Array(codeParameter, targetType)

  private def getComposedCode(
      userCode: String,
      inputColumn: String,
      outputColumn: String,
      targetType: DataType): String = {
    val newFieldName = UUID.randomUUID().toString.replace("-", "")
    val newFieldJson =
      s"""{"name": "$newFieldName", "type":${targetType.json}, "nullable":true, "metadata":null}"""

    s"""
      |$userCode
      |
      |from pyspark.sql.types import *
      |import json
      |
      |def transform(dataframe):
      |    new_field = StructField.fromJson(json.loads(\"\"\"$newFieldJson\"\"\"))
      |    schema = StructType(dataframe.schema.fields + [new_field])
      |    def _transform_row(row):
      |        return row + (transform_value(row['$inputColumn'], '$inputColumn'),)
      |    return sqlContext.createDataFrame(dataframe.map(_transform_row), schema)
    """.stripMargin
  }

  private def executePythonCode(
      code: String,
      inputColumn: String,
      outputColumn: String,
      context: ExecutionContext,
      dataFrame: DataFrame): DataFrame = {
    context.pythonCodeExecutor.run(code) match {
      case Left(error) =>
        throw CustomOperationExecutionException(s"Execution exception:\n\n$error")

      case Right(_) =>
        val sparkDataFrame =
          context.dataFrameStorage.getOutputDataFrame(OutputPortNumber).getOrElse {
            throw CustomOperationExecutionException(
              "Operation finished successfully, but did not produce a DataFrame.")
          }

        val newSparkDataFrame = context.sqlContext.createDataFrame(
          sparkDataFrame.rdd,
          transformSingleColumnSchema(inputColumn, outputColumn, dataFrame.schema.get).get)
        DataFrame.fromSparkDataFrame(newSparkDataFrame)
    }
  }

  override def transformSingleColumn(
      inputColumn: String,
      outputColumn: String,
      context: ExecutionContext,
      dataFrame: DataFrame): DataFrame = {
    val code = getComposedCode(
      $(codeParameter), inputColumn, outputColumn, getTargetType.columnType)
    logger.debug(s"Python code to be validated and executed:\n$code")

    if (!context.pythonCodeExecutor.isValid(code)) {
      throw CustomOperationExecutionException("Code validation failed")
    }

    context.dataFrameStorage.setInputDataFrame(InputPortNumber, dataFrame.sparkDataFrame)

    executePythonCode(code, inputColumn, outputColumn, context, dataFrame)
  }

  override def transformSingleColumnSchema(
      inputColumn: String,
      outputColumn: String,
      schema: StructType): Option[StructType] = {
    MultiColumnTransformer.assertColumnExist(inputColumn, schema)
    MultiColumnTransformer.assertColumnDoesNotExist(outputColumn, schema)
    Some(schema.add(StructField(outputColumn, getTargetType.columnType, nullable = true)))
  }
}

object PythonColumnTransformer {
  val InputPortNumber: Int = 0
  val OutputPortNumber: Int = 0
}
