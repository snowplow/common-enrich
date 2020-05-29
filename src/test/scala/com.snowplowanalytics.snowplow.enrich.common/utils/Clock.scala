/*
 * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
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
package utils

import java.util.concurrent.TimeUnit

import cats.Eval
import cats.effect.{Clock => CEClock}

object Clock {
  implicit val evalClock: CEClock[Eval] = new CEClock[Eval] {
    final def realTime(unit: TimeUnit): Eval[Long] =
      Eval.later(unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS))
    final def monotonic(unit: TimeUnit): Eval[Long] =
      Eval.later(unit.convert(System.nanoTime(), TimeUnit.NANOSECONDS))
  }
}
