package io.spherelabs.buildable

import io.spherelabs.factory.BuildableComponent


@BuildableComponent
class Student : Person {
  override fun sleep() {
    println("Student sleeps 5 hours")
  }
}

fun main() {
}
