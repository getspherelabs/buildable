package io.spherelabs.buildable

import io.spherelabs.mapperannotation.BuildableMapper

@BuildableMapper(
  from = [NotificationEntity::class],
  to = [NotificationEntity::class]
)
data class NotificationDto(
  val name: String
)

data class NotificationEntity(
  val name: String
)

fun main() {
}
