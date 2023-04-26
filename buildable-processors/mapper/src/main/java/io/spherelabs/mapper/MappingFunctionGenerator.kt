package io.spherelabs.mapper

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Variance

private const val DIAMOND_OPERATOR_OPEN = "<"
private const val DIAMOND_OPERATOR_CLOSE = ">"
private const val KOTLIN_FUNCTION_KEYWORD = "fun"
private const val CLOSE_FUNCTION = ")"
private const val OPEN_FUNCTION = "("

class MappingFunctionGenerator(
  private val resolver: Resolver,
  private val logger: KSPLogger
) {

  /**
   *
   * Generates a function in the form of: `fun SourceClass.toTargetClass(): TargetClass`
   *
   * @param targetClass The class we want to map to.
   *        It defines the schema which the `sourceClass` properties should be mapped to.
   *        Therefore, it is also the return type of the mapper extension function.
   *
   * @param sourceClass The class which is the source for properties we want to map to the constructor of the
   *        target class. It is therefore at the same time the class for which we generate the extension function.
   */
  fun generateMappingFunction(
    sourceClass: KSClassDeclaration,
    targetClass: KSClassDeclaration,
    packageImports: PackageImports
  ): String {
    val targetClassName: String = targetClass.simpleName.getShortName()
    val packageName: String = targetClass.packageName.asString()
    val targetClassTypeParameters: List<KSTypeParameter> = targetClass.typeParameters

    packageImports.targetClassTypeParameters += targetClassTypeParameters

    // Add import for the target class
    packageImports.addImport(packageName, targetClassName)

    // Add import for source class
    packageImports.addImport(
      sourceClass.packageName.asString(),
      sourceClass.simpleName.asString()
    )

    // Create mapping extension function for source class to target class
    return generateExtensionMapperFunctionForSourceClass(
      sourceClass = sourceClass,
      targetClass = targetClass,
      targetClassTypeParameters = targetClassTypeParameters,
      targetClassName = targetClassName,
      packageImports = packageImports
    )
  }

  private fun generateExtensionMapperFunctionForSourceClass(
    sourceClass: KSClassDeclaration,
    targetClass: KSClassDeclaration,
    targetClassTypeParameters: List<KSTypeParameter>,
    targetClassName: String,
    packageImports: PackageImports
  ): String {
    var extensionFunctions = ""
    val sourceClassName: String = sourceClass.toString()

    // Find all matching arguments from the source class that can be mapped to the target class
    val (
      missingConstructorArguments: List<KSValueParameter>,
      matchingConstructorArguments: List<MatchingArgument>
    ) = extractMatchingAndMissingConstructorArguments(
      targetClass = targetClass,
      sourceClass = sourceClass,
      targetClassTypeParameters = targetClassTypeParameters,
      packageImports = packageImports,
      sourceClassName = sourceClassName,
      targetClassName = targetClassName
    )

    if (missingConstructorArguments.isNotEmpty()) {
      if (matchingConstructorArguments.isEmpty()) {
        logger.warn(
          message = "No matching arguments for mapping from class `$sourceClassName` to `$targetClassName`. \n" +
            "You can instead remove the argument `$sourceClassName::class` from the " +
            "`$BuildableMapperConstants.MAPPER_ANNOTATION_NAME` annotation in class `$targetClassName` " +
            "and use its constructor directly.",
          symbol = targetClass
        )
      }

      // Add type parameters from target class to function head
      if (targetClassTypeParameters.isNotEmpty()) {
        extensionFunctions += "$KOTLIN_FUNCTION_KEYWORD $DIAMOND_OPERATOR_OPEN"
        targetClassTypeParameters.forEachIndexed { index, targetClassTypeParameter: KSTypeParameter ->
          val separator: String = getArgumentDeclarationLineEnding(
            hasNextLine = targetClassTypeParameters.lastIndex != index,
            addSpace = true
          )

          extensionFunctions += targetClassTypeParameter.name.asString()
         
          targetClassTypeParameter.bounds.firstOrNull()
            ?.let { upperBound: KSTypeReference ->
              packageImports.addImport(upperBound.resolve())
              extensionFunctions += ": $upperBound"
            }

          extensionFunctions += separator
        }
        extensionFunctions += "$DIAMOND_OPERATOR_CLOSE ${
          generateExtensionFunctionName(
            sourceClass,
            targetClass,
            packageImports
          )
        }(\n"
      } else {
        extensionFunctions += "$KOTLIN_FUNCTION_KEYWORD ${
          generateExtensionFunctionName(
            sourceClass,
            targetClass,
            packageImports
          )
        }(\n"
      }

      missingConstructorArguments.forEachIndexed { missingArgumentIndex: Int, missingArgument: KSValueParameter ->
        extensionFunctions += convertMissingConstructorArgumentToDeclarationText(
          isLastIndex = missingConstructorArguments.lastIndex == missingArgumentIndex,
          missingArgument = missingArgument,
          packageImports = packageImports,
          targetClass = targetClass
        )
      }

      extensionFunctions += "$CLOSE_FUNCTION = $targetClassName(\n"
    }
    // No missing arguments, we can create the default empty param function head
    else {
      // Add type parameters from target class to function head
      if (targetClassTypeParameters.isNotEmpty()) {
        extensionFunctions += "$KOTLIN_FUNCTION_KEYWORD $DIAMOND_OPERATOR_OPEN"
        targetClassTypeParameters.forEachIndexed { index: Int, targetClassTypeParameter: KSTypeParameter ->
          val lineEnding: String = getArgumentDeclarationLineEnding(
            hasNextLine = targetClassTypeParameters.lastIndex != index,
            addSpace = true
          )
          extensionFunctions += targetClassTypeParameter.name.asString() + lineEnding
        }
        extensionFunctions += "$DIAMOND_OPERATOR_CLOSE ${
          generateExtensionFunctionName(
            sourceClass = sourceClass,
            targetClass = targetClass,
            packageImports = packageImports
          )
        }$OPEN_FUNCTION$CLOSE_FUNCTION = $targetClassName$OPEN_FUNCTION\n"
      } else {
        extensionFunctions += "$KOTLIN_FUNCTION_KEYWORD ${
          generateExtensionFunctionName(
            sourceClass = sourceClass,
            targetClass = targetClass,
            packageImports = packageImports
          )
        }$OPEN_FUNCTION$CLOSE_FUNCTION = $targetClassName$OPEN_FUNCTION\n"
      }
    }

    // Assign matching values from source class to target class constructor
    matchingConstructorArguments.forEachIndexed { index: Int, matchingArgument: MatchingArgument ->
      val lineEnding: String =
        getArgumentDeclarationLineEnding(
          hasNextLine = missingConstructorArguments.lastIndex != index || missingConstructorArguments.isNotEmpty()
        )
      extensionFunctions += "\t${matchingArgument.targetFieldName} = this.${matchingArgument.sourceFieldName}"

      // If source class is a generic type parameter we need to cast it to avoid compilation errors
      matchingArgument.commonTargetFieldName?.let { targetClassPropertyGenericTypeName: String ->
        extensionFunctions += " as $targetClassPropertyGenericTypeName"
      }

      extensionFunctions += "$lineEnding\n"
    }

    // Assign values from function head to constructor param
    missingConstructorArguments.forEachIndexed { index: Int, paramName: KSValueParameter ->
      val lineEnding: String =
        getArgumentDeclarationLineEnding(hasNextLine = missingConstructorArguments.lastIndex != index)
      extensionFunctions += "\t$paramName = $paramName$lineEnding\n"
    }

    extensionFunctions += "$CLOSE_FUNCTION\n\n"

    return extensionFunctions
  }

  private fun convertMissingConstructorArgumentToDeclarationText(
    isLastIndex: Boolean,
    missingArgument: KSValueParameter,
    packageImports: PackageImports,
    targetClass: KSClassDeclaration
  ): String {
    var missingArgumentDeclarationText = ""
    val typeArguments: MutableList<TypeArgument> = mutableListOf()
    val missingArgumentType: KSType = missingArgument.type.resolve()

    packageImports.addImport(missingArgumentType)

    // Find all required types for the argument class
    missingArgumentType.arguments.forEach { ksTypeArgument: KSTypeArgument ->
      if (ksTypeArgument.variance == Variance.STAR) {
        typeArguments.add(TypeArgument.Wildcard)
      } else {
        val argumentClass: KSType? = ksTypeArgument.type?.resolve()

        if (argumentClass != null) {
          typeArguments.add(TypeArgument.Argument(argumentClass))
        } else {
          logger.logAndThrowError(
            errorMessage = "Type for not provided argument `${missingArgument.name}` couldn't get resolved.",
            targetClass = targetClass
          )
        }
      }
    }

    missingArgumentDeclarationText += "\t${missingArgument.name?.asString()}: ${missingArgumentType.getName()}"

    if (typeArguments.isNotEmpty()) {
      missingArgumentDeclarationText += DIAMOND_OPERATOR_OPEN
      typeArguments.forEachIndexed { argumentTypeIndex: Int, typeArgument: TypeArgument ->
        val typeSeparator: String = getArgumentDeclarationLineEnding(
          hasNextLine = typeArguments.lastIndex != argumentTypeIndex,
          addSpace = true
        )
        when (typeArgument) {
          is TypeArgument.Argument -> {
            val argumentClass: KSType = typeArgument.ksType

            missingArgumentDeclarationText += convertTypeArgumentToString(
              typeText = argumentClass.getName(),
              typeParametersDequeue = ArrayDeque(argumentClass.arguments)
            )
            missingArgumentDeclarationText += argumentClass.markedNullableAsString() + typeSeparator
            packageImports.addImport(argumentClass)
          }

          TypeArgument.Wildcard -> missingArgumentDeclarationText += "*$typeSeparator"
        }
      }
      missingArgumentDeclarationText += DIAMOND_OPERATOR_CLOSE
    }

    missingArgumentDeclarationText += missingArgumentType.markedNullableAsString()

    val lineEnding: String = getArgumentDeclarationLineEnding(hasNextLine = !isLastIndex)
    missingArgumentDeclarationText += "$lineEnding\n"

    return missingArgumentDeclarationText
  }

  private fun extractMatchingAndMissingConstructorArguments(
    targetClass: KSClassDeclaration,
    sourceClass: KSClassDeclaration,
    targetClassTypeParameters: List<KSTypeParameter>,
    packageImports: PackageImports,
    sourceClassName: String,
    targetClassName: String
  ): Pair<MutableList<KSValueParameter>, MutableList<MatchingArgument>> {
    val missingArguments = mutableListOf<KSValueParameter>()
    val matchingArguments = mutableListOf<MatchingArgument>()

    targetClass.primaryConstructor?.parameters?.forEach { valueParam: KSValueParameter ->

      val valueName: String = valueParam.name?.asString()!!
      var matchingArgument: MatchingArgument? = null

      // Search for any matching fields, also considers fields of supertype
      sourceClass.getAllProperties()
        .forEach { parameterFromSourceClass: KSPropertyDeclaration ->
          val parameterNameFromSourceClass: String =
            parameterFromSourceClass.simpleName.asString()

          val aliases: Set<String> =
            findPropertyAliases(parameterFromSourceClass.annotations + valueParam.annotations)

          if ((parameterNameFromSourceClass == valueName) ||
            aliases.any { alias ->
              alias == valueName ||
                alias == parameterNameFromSourceClass
            }
          ) {
            val parameterTypeFromTargetClass: KSType = valueParam.type.resolve()
            val parameterTypeFromSourceClass: KSType =
              parameterFromSourceClass.type.resolve()

            val referencedTargetClassGenericTypeParameter: KSTypeParameter? =
              targetClassTypeParameters.firstOrNull { targetClassTypeParam ->
                targetClassTypeParam.simpleName.asString() == parameterTypeFromTargetClass.getName()
              }

            val targetClassTypeParamUpperBoundDeclaration: KSDeclaration? =
              referencedTargetClassGenericTypeParameter
                ?.bounds
                ?.firstOrNull()
                ?.resolve()
                ?.declaration

            if (targetClassTypeParamUpperBoundDeclaration != null &&
              parameterTypeFromSourceClass.declaration.containsSupertype(
                targetClassTypeParamUpperBoundDeclaration
              ) &&
              evaluateKSTypeAssignable(
                parameterTypeFromSourceClass = parameterTypeFromSourceClass,
                parameterTypeFromTargetClass = parameterTypeFromTargetClass,
                isGenericType = true
              )
            ) {
              matchingArgument = MatchingArgument(
                targetFieldName = valueName,
                sourceFieldName = parameterNameFromSourceClass,
                commonTargetFieldName = run {
                  if (targetClassTypeParamUpperBoundDeclaration.containingFile != null) {
                    packageImports.addImport(
                      targetClassTypeParamUpperBoundDeclaration.packageName.asString(),
                      targetClassTypeParamUpperBoundDeclaration.getName()
                    )
                  }
                  targetClassTypeParamUpperBoundDeclaration.getName() + parameterTypeFromTargetClass.markedNullableAsString()
                }
              )
            } else if (evaluateKSTypeAssignable(
                parameterTypeFromSourceClass = parameterTypeFromSourceClass,
                parameterTypeFromTargetClass = parameterTypeFromTargetClass,
                isGenericType = referencedTargetClassGenericTypeParameter != null
              )
            ) {
              matchingArgument = MatchingArgument(
                targetFieldName = valueName,
                sourceFieldName = parameterNameFromSourceClass,
                commonTargetFieldName = referencedTargetClassGenericTypeParameter?.let { typeParam: KSTypeParameter ->
                  typeParam.simpleName.asString() + parameterTypeFromTargetClass.markedNullableAsString()
                }
              )
            } else {
              logger.warn(
                message = "Found matching parameter `$valueName` from class `$sourceClassName` for target class `$targetClassName`, " +
                  "but the type `$parameterTypeFromSourceClass` doesn't match the expected type `$parameterTypeFromTargetClass`.",
                symbol = targetClass
              )
            }
          }
        }

      if (matchingArgument != null) {
        matchingArguments.add(matchingArgument!!)
      } else if (!valueParam.hasDefault) {
        missingArguments.add(valueParam)
      }
    }

    return Pair(missingArguments, matchingArguments)
  }

  /**
   * Checks if any of respective [KSDeclaration] contains the [searchedSuperType].
   *
   * @param searchedSuperType super type to check for in the [KSDeclaration]
   */
  private fun KSDeclaration.containsSupertype(searchedSuperType: KSDeclaration): Boolean {
    val classDeclaration: KSClassDeclaration =
      this.qualifiedName?.let(resolver::getClassDeclarationByName) ?: return false
    val containsSuperType: Boolean =
      classDeclaration.superTypes.any { superType: KSTypeReference ->
        val comparableSuperTypeDeclaration: KSDeclaration = superType.resolve().declaration
        searchedSuperType.compareByQualifiedName(comparableSuperTypeDeclaration) || comparableSuperTypeDeclaration.containsSupertype(
          searchedSuperType
        )
      }

    return containsSuperType
  }

  private fun convertTypeArgumentToString(
    typeText: String,
    typeParametersDequeue: ArrayDeque<KSTypeArgument>,
    shouldAddOpenOperator: Boolean = true
  ): String {
    val typeParameter: KSTypeArgument =
      typeParametersDequeue.removeFirstOrNull() ?: return typeText
    val resolvedTypeParameter: KSType = typeParameter.type?.resolve() ?: return typeText
    var appendedTypeText: String = typeText

    if (shouldAddOpenOperator) appendedTypeText += DIAMOND_OPERATOR_OPEN

    appendedTypeText += resolvedTypeParameter.getName()
    appendedTypeText += convertTypeArgumentToString(
      "",
      ArrayDeque(resolvedTypeParameter.arguments)
    )

    val typeParamLineEnding: String =
      getArgumentDeclarationLineEnding(
        hasNextLine = typeParametersDequeue.isNotEmpty(),
        addSpace = true
      )

    return if (typeParametersDequeue.isNotEmpty()) {
      appendedTypeText += typeParamLineEnding

      convertTypeArgumentToString(
        typeText = appendedTypeText,
        typeParametersDequeue = typeParametersDequeue,
        shouldAddOpenOperator = false
      )
    } else {
      appendedTypeText += DIAMOND_OPERATOR_CLOSE
      appendedTypeText += typeParamLineEnding
      appendedTypeText
    }
  }

  /**
   * @param parameterTypeFromSourceClass
   * @param parameterTypeFromTargetClass
   * @param isGenericType accepts that the argument names differ
   */
  private fun evaluateKSTypeAssignable(
    parameterTypeFromSourceClass: KSType,
    parameterTypeFromTargetClass: KSType,
    isGenericType: Boolean
  ): Boolean {
    if (parameterTypeFromSourceClass.isMarkedNullable && !parameterTypeFromTargetClass.isMarkedNullable) return false
    if (!isGenericType && !parameterTypeFromSourceClass.compareByDeclaration(
        parameterTypeFromTargetClass.declaration
      )
    ) {
      return false
    }
    return isGenericType || parameterTypeFromSourceClass.arguments.matches(
      parameterTypeFromTargetClass.arguments
    )
  }

  private fun List<KSTypeArgument>.matches(otherArguments: List<KSTypeArgument>): Boolean {
    if (isEmpty() == otherArguments.isEmpty()) return true
    if (size != otherArguments.size) return false
    return this.all { argFromThis: KSTypeArgument ->
      otherArguments.firstOrNull { argFromOther: KSTypeArgument ->
        val argTypeFromThis: KSType = argFromThis.type?.resolve() ?: return@all false
        val argTypeFromOther: KSType = argFromOther.type?.resolve() ?: return@all false
        val hasSameTypeName: Boolean =
          argTypeFromThis.compareByDeclaration(argTypeFromOther.declaration)
        val matchesNullability: Boolean =
          if (argTypeFromThis.isMarkedNullable) !argTypeFromOther.isMarkedNullable else true

        return@firstOrNull hasSameTypeName &&
          matchesNullability &&
          (argFromThis.type?.resolve()?.arguments?.matches(argTypeFromOther.arguments) == true)
      } != null
    }
  }

  private fun generateExtensionFunctionName(
    sourceClass: KSDeclaration,
    targetClass: KSDeclaration,
    packageImports: PackageImports
  ): String {
    val sourceClassName: String = sourceClass.getName()
    val targetClassName: String = targetClass.getName()

    val sourceClassType: String =
      sourceClass.typeParameters.firstOrNull()
        ?.let KsTypeParameterLet@{ ksTypeParameter: KSTypeParameter ->

          val upperBound = ksTypeParameter.bounds.firstOrNull()?.resolve()
            ?.let UpperBoundLet@{ upperBoundType ->
              packageImports.addImport(upperBoundType)
              return@UpperBoundLet upperBoundType.getName()
            } ?: ""

          DIAMOND_OPERATOR_OPEN + upperBound + DIAMOND_OPERATOR_CLOSE
        } ?: ""

    return "$sourceClassName$sourceClassType.to$targetClassName"
  }

  private fun getArgumentDeclarationLineEnding(
    hasNextLine: Boolean,
    addSpace: Boolean = false
  ): String =
    if (hasNextLine) "," + if (addSpace) " " else "" else ""

  private fun findPropertyAliases(annotations: Sequence<KSAnnotation>): Set<String> {
    return annotations
      .filter { ksAnnotation -> ksAnnotation.shortName.asString() == BuildableMapperConstants.PROPERTY_ANNOTATION_NAME }
      .flatMap { arguments: KSAnnotation -> arguments.arguments }
      .filter { ksAnnotationArgument -> ksAnnotationArgument.name?.asString() == BuildableMapperConstants.PROPERTY_ALIAS_PARAM_NAME }
      .mapNotNull { ksAnnotationArgument: KSValueArgument ->
        if (ksAnnotationArgument.value is ArrayList<*>) {
          val aliases: ArrayList<*> = (ksAnnotationArgument.value as ArrayList<*>)
          aliases.filterIsInstance<String>().toSet()
        } else {
          null
        }
      }
      .reduceOrNull(Set<String>::plus)
      ?: emptySet()
  }
}
