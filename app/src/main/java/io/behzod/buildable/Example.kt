package io.behzod.buildable

import io.spherelabs.buildableannotations.BuildableComponent
import io.spherelabs.buildableannotations.BuildableFactory



@BuildableFactory
interface Car {
    fun drive()
}

@BuildableComponent
class Nexia: Car {
    override fun drive() {
        println("Nexia is driving...")
    }
}

@BuildableComponent
class Matiz: Car {
    override fun drive() {
        println("Matiz is driving...")
    }
}