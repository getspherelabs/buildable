package io.spherelabs.mapper

object BuildableMapperConstants {
  const val MAPPER_ANNOTATION_NAME = "BuildableMapper"
  const val MAPPER_TO_PARAM_NAME = "to"
  const val MAPPER_FROM_PARAM_NAME = "from"

  const val PROPERTY_ANNOTATION_NAME = "BuildableProperty"
  const val PROPERTY_ALIAS_PARAM_NAME = "aliases"

  const val PACKAGE_KEYWORD = "package"
  const val IMPORT_KEYWORD = "import"

  const val FINDER_ERROR_MESSAGE = "Invalid usage of `@${BuildableMapperConstants.MAPPER_ANNOTATION_NAME}` annotation: " +
    "the `classes` argument is missing or invalid. " +
    "The `classes` argument should contain at least one class to map, like this: " +
    "`${BuildableMapperConstants.MAPPER_ANNOTATION_NAME}(classes = [YourClassToMap::class])`." +
    "Please make sure to provide a valid list of classes to map."
}
