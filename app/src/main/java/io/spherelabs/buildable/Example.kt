package io.spherelabs.buildable

import io.spherelabs.factory.BuildableComponent
import io.spherelabs.factory.BuildableFactory


@BuildableFactory
interface Car {
  fun drive()
}

@BuildableComponent
class Nexia : Car {
  override fun drive() {
    println("Nexia is driving...")
  }
}

@BuildableComponent
class Matiz : Car {
  override fun drive() {
    println("Matiz is driving...")
  }
}

fun main() {
  val nexia = CarFactory(CarType.NEXIA)
}
