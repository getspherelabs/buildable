package io.behzod.mapper

import kotlin.reflect.KClass


annotation class BuildableMapper(
    val to: Array<KClass<*>> = [],
    val from: Array<KClass<*>> = []
)