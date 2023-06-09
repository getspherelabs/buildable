package io.spherelabs.mapper

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import io.spherelabs.mapper.importer.PackageImporter
import java.io.OutputStream

private fun OutputStream.appendText(str: String) {
  this.write(str.toByteArray())
}

private const val GENERATED_CLASS_SUFFIX = "BuildableMapperExtensions"
private const val SUPPRESS_UNCHECKED_CAST_STATEMENT = "@file:Suppress(\"UNCHECKED_CAST\")\n\n"
private const val GENERATED_FILE_PATH = "io.behzod.buildable.mapper"

class BuildableMapperFinder(
  private val codeGenerator: CodeGenerator,
  private val resolver: Resolver,
  private val logger: KSPLogger
) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val annotatedClass: KSClassDeclaration = classDeclaration
    val buildableMapperAnnotation: KSAnnotation = findBuildableMapperAnnotation(annotatedClass)
    val mapFromClasses: List<KSClassDeclaration> = findArgumentsFromClasses(
      buildableMapperAnnotation,
      BuildableMapperConstants.MAPPER_FROM_PARAM_NAME
    )
    val mapToClasses: List<KSClassDeclaration> = findArgumentsFromClasses(
      buildableMapperAnnotation,
      BuildableMapperConstants.MAPPER_TO_PARAM_NAME
    )

    if (mapFromClasses.isEmpty() && mapToClasses.isEmpty()) {
      logger.warn("Missing mapping functions for @${BuildableMapperConstants.MAPPER_ANNOTATION_NAME} annotated class $annotatedClass.")
      return
    }

    val mappingFunctionGenerator = MappingFunctionGenerator(
      resolver = resolver,
      logger = logger
    )

    var extensionFunctions = ""
    val packageImporter = PackageImporter()

    if (mapFromClasses.isNotEmpty()) {
      mapFromClasses.forEach { sourceClass: KSClassDeclaration ->
        extensionFunctions += mappingFunctionGenerator.generateMappingFunction(
          targetClass = annotatedClass,
          sourceClass = sourceClass,
          packageImporter = packageImporter
        )
      }
    }

    if (mapToClasses.isNotEmpty()) {
      mapToClasses.forEach { targetClass: KSClassDeclaration ->
        extensionFunctions += mappingFunctionGenerator.generateMappingFunction(
          targetClass = targetClass,
          sourceClass = annotatedClass,
          packageImporter = packageImporter
        )
      }
    }

    generateMapperFunctions(
      containingFile = classDeclaration.containingFile!!,
      targetClassName = annotatedClass.simpleName.getShortName(),
      packageImporter = packageImporter,
      extensionFunctions = extensionFunctions
    )
  }

  override fun visitAnnotation(annotation: KSAnnotation, data: Unit) {
    annotation.annotationType.resolve().declaration.accept(this, data)
  }

  private fun generateMapperFunctions(
    containingFile: KSFile,
    targetClassName: String,
    packageImporter: PackageImporter,
    extensionFunctions: String
  ) {
    codeGenerator.createNewFile(
      dependencies = Dependencies(true, containingFile),
      packageName = GENERATED_FILE_PATH,
      fileName = "${targetClassName}$GENERATED_CLASS_SUFFIX"
    ).use { generatedFileOutputStream: OutputStream ->

      if (packageImporter.targetClassTypeParameters.isNotEmpty()) {
        generatedFileOutputStream.appendText(
          SUPPRESS_UNCHECKED_CAST_STATEMENT
        )
      }
      generatedFileOutputStream.appendText("${BuildableMapperConstants.PACKAGE_KEYWORD} $GENERATED_FILE_PATH\n\n")
      generatedFileOutputStream.appendText(packageImporter.format())
      generatedFileOutputStream.appendText(extensionFunctions)
    }
  }

  private fun findBuildableMapperAnnotation(targetClass: KSClassDeclaration): KSAnnotation {
    val buildableMapperAnnotation: KSAnnotation = targetClass.annotations
      .first { targetClassAnnotations ->
        targetClassAnnotations.shortName.asString() == BuildableMapperConstants.MAPPER_ANNOTATION_NAME
      }

    buildableMapperAnnotation.arguments.firstOrNull { constructorParam ->
      constructorParam.name?.asString() == BuildableMapperConstants.MAPPER_FROM_PARAM_NAME
    } ?: run {
      logger.logAndThrowError(
        errorMessage = BuildableMapperConstants.FINDER_ERROR_MESSAGE,
        targetClass = targetClass
      )
    }

    return buildableMapperAnnotation
  }

  @Suppress("UNCHECKED_CAST")
  private fun findArgumentsFromClasses(buildableMapperAnnotation: KSAnnotation, paramName: String): List<KSClassDeclaration> {
    return buildableMapperAnnotation
      .arguments
      .find { annotationArgument: KSValueArgument -> annotationArgument.name?.asString() == paramName }
      ?.let { ksValueArgument -> ksValueArgument.value as List<KSType> }
      ?.mapNotNull { argumentClassType -> resolver.getClassDeclarationByName(argumentClassType.declaration.qualifiedName!!) }
      ?: emptyList()
  }
}
