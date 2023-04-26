package io.spherelabs.mapperannotation

import kotlin.reflect.KClass

annotation class BuildableMapper(
  val to: Array<KClass<*>> = [],
  val from: Array<KClass<*>> = []
)
