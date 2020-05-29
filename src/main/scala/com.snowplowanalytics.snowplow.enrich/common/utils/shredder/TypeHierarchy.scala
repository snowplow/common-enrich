/*
 * Copyright (c) 2014-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich.common.utils.shredder

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JacksonUtils
import io.circe.generic.auto._
import io.circe.jackson._
import io.circe.syntax._
import scalaz._

/** Companion object contains helpers. */
object TypeHierarchy {
  private val NodeFactory = JacksonUtils.nodeFactory()
}

/** Expresses the hierarchy of types for this type. */
final case class TypeHierarchy(
  val rootId: String,
  val rootTstamp: String,
  val refRoot: String,
  val refTree: List[String],
  val refParent: String
) {

  /**
   * Converts a TypeHierarchy into a JSON containing each element.
   * @return the TypeHierarchy as a Jackson JsonNode
   */
  def toJsonNode: JsonNode =
    circeToJackson(this.asJson)

  /**
   * Completes a partial TypeHierarchy with the supplied refTree elements, and uses
   * the final refTree to replace the refParent too.
   * @param refTree the rest of the type tree to append onto existing refTree
   * @return the completed TypeHierarchy
   */
  def complete(refTree: List[String]): TypeHierarchy =
    partialHierarchyLens.set(this, refTree)

  /** A Scalaz Lens to complete the refTree within a TypeHierarchy object. */
  private val partialHierarchyLens: Lens[TypeHierarchy, List[String]] =
    Lens.lensu((ph, rt) => {
      val full = ph.refTree ++ rt
      ph.copy(
        refTree = full,
        refParent = secondTail(full)
      )
    }, _.refTree)

  /**
   * Get the last-but-one element ("tail-tail") from a list.
   * @param ls The list to return the last-but-one element from
   * @return the last-but-one element from this list
   */
  private def secondTail[A](ls: List[A]): A = ls match {
    case h :: _ :: Nil => h
    case _ :: tail => secondTail(tail)
    case _ => throw new NoSuchElementException
  }

}
