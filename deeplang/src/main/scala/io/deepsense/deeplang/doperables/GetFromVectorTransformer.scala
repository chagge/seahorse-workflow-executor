/**
 * Copyright 2016, deepsense.io
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

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import io.deepsense.commons.types.ColumnType
import io.deepsense.deeplang._
import io.deepsense.deeplang.doperables.dataframe._
import io.deepsense.deeplang.params.{NumericParam, Param}
import io.deepsense.deeplang.params.validators.RangeValidator

case class GetFromVectorTransformer() extends MultiColumnTransformer {

  val index = NumericParam(
    name = "index",
    description = "Index of value to extract (starting from 0).",
    validator = RangeValidator.positiveIntegers)
  setDefault(index, 0.0)

  def getIndex: Int = $(index).toInt
  def setIndex(value: Double): this.type = set(index, value)

  override def getSpecificParams: Array[Param[_]] = Array(index)

  override def transformSingleColumn(
      inputColumn: String,
      outputColumn: String,
      context: ExecutionContext,
      dataFrame: DataFrame): DataFrame = {
    val inputColumnIndex = dataFrame.schema.get.fieldIndex(inputColumn)
    val indexInVector = getIndex
    val transformedRdd = dataFrame.sparkDataFrame.map { case r =>
      val vector = r.get(inputColumnIndex).asInstanceOf[org.apache.spark.mllib.linalg.Vector]
      // Append output column as the last column
      if (vector != null) {
        Row.fromSeq(r.toSeq :+ vector.apply(indexInVector))
      } else {
        Row.fromSeq(r.toSeq :+ null)
      }
    }
    val expectedSchema =
      transformSingleColumnSchema(inputColumn, outputColumn, dataFrame.schema.get).get
    val transformedDataFrame = context.sqlContext.createDataFrame(transformedRdd, expectedSchema)
    DataFrame.fromSparkDataFrame(transformedDataFrame)
  }

  override def transformSingleColumnSchema(
      inputColumn: String,
      outputColumn: String,
      schema: StructType): Option[StructType] = {
    MultiColumnTransformer.assertColumnExist(inputColumn, schema)
    DataFrame.assertExpectedColumnType(schema(inputColumn), ColumnType.vector)
    MultiColumnTransformer.assertColumnDoesNotExist(outputColumn, schema)
    Some(schema.add(StructField(outputColumn, DoubleType)))
  }
}
