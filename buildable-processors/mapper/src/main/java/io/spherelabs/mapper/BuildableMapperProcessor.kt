package io.spherelabs.mapper

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import io.spherelabs.mapperannotation.BuildableMapper

class BuildableMapperProcessor(
  val codeGenerator: CodeGenerator,
  val logger: KSPLogger
) : SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val resolvedSymbols: Sequence<KSAnnotated> = getAnnotatedSymbols(resolver)

    resolvedSymbols
      .forEach { ksAnnotated: KSAnnotated ->
        handleClassDeclaration(ksAnnotated, resolver)
      }

    return emptyList()
  }

  private fun getAnnotatedSymbols(resolver: Resolver): Sequence<KSAnnotated> {
    val annotationName = BuildableMapper::class.qualifiedName.orEmpty()
    return resolver.getSymbolsWithAnnotation(annotationName)
      .filter { ksAnnotated: KSAnnotated -> ksAnnotated is KSClassDeclaration && ksAnnotated.validate() }
  }

  private fun handleClassDeclaration(ksAnnotated: KSAnnotated, resolver: Resolver) {
    val classDeclaration: KSClassDeclaration = (ksAnnotated as KSClassDeclaration)
    when (classDeclaration.classKind) {
      ClassKind.INTERFACE,
      ClassKind.ENUM_CLASS,
      ClassKind.ENUM_ENTRY,
      ClassKind.OBJECT,
      ClassKind.ANNOTATION_CLASS -> {
        logger.logAndThrowError(
          errorMessage = "Unable to generate a function for class '${classDeclaration.getName()}'. " +
              "The class type '${classDeclaration.classKind}' is not supported. " +
              "Please ensure that the '@BuildableMapper' annotation is only applied to " +
              "regular classes (not interfaces, enums, objects, or annotation classes).",
          targetClass = classDeclaration
        )
      }

      else -> {
        ksAnnotated.accept(BuildableMapperFinder(codeGenerator, resolver, logger), Unit)
      }
    }
  }
}
