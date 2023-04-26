package io.spherelabs.factory

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName

object BuildableFactoryFinder : Finder {

  override fun invoke(resolver: Resolver): Set<ClassName> {
    return resolver.getSymbolsWithAnnotation(BuildableFactory::class.qualifiedName.orEmpty())
      .filterIsInstance<KSClassDeclaration>()
      .filter(KSNode::validate)
      .map { it.toClassName() }
      .toSet()
  }
}
