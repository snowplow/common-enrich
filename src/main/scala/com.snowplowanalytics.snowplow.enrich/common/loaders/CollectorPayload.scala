/*
 * Copyright (c) 2012-2014 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.enrich.common
package loaders

// Java
import java.net.URI
import java.net.URLDecoder

// Scala
import scala.collection.JavaConversions._
// import scala.language.existentials

// Scalaz
import scalaz._
import Scalaz._

// Apache URLEncodedUtils
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils

// Joda-Time
import org.joda.time.DateTime

object CollectorPayload {

  /**
   * Defaults for the tracker vendor and version
   * before we implemented this into Snowplow.
   */
  object Defaults {
    val vendor = "com.snowplowanalytics.snowplow"
    val version = "tp1"
  }

}

/**
 * The canonical input format for the ETL
 * process: it should be possible to
 * convert any collector input format to
 * this format, ready for the main,
 * collector-agnostic stage of the ETL.
 */
final case class CollectorPayload(
    timestamp:   DateTime, // Collector timestamp
    vendor:      String,
    version:     String,
    querystring: List[NameValuePair], // May be Nil so not a NEL
    // body:     String,
    source:      InputSource,    // See below for defn.
    encoding:    String,         // TODO: move into input source
    ipAddress:   Option[String],
    userAgent:   Option[String],
    refererUri:  Option[String],
    headers:     List[String],   // May be Nil so not a NEL
    userId:      Option[String])

/**
 * Unambiguously identifies the collector
 * source of this input line.
 */
final case class InputSource(
    collector: String, // Collector name/version
    hostname:  Option[String])
