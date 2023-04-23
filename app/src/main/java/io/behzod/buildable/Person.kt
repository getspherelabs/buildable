package io.behzod.buildable

import io.spherelabs.buildableannotations.BuildableFactory

@BuildableFactory
interface Person {
    fun sleep()
}