package io.spherelabs.buildable

import io.spherelabs.factory.BuildableFactory


@BuildableFactory
interface Person {
  fun sleep()
}
