package io.spherelabs.mapper

data class MatchingArgument(
  val targetFieldName: String,
  val sourceFieldName: String,
  val commonTargetFieldName: String? = null
)
