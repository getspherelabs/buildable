package io.spherelabs.factory

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class BuildableFactoryProcessorProvider : SymbolProcessorProvider {

  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return BuildableFactoryProcessor(
      logger = environment.logger,
      codeGenerator = environment.codeGenerator,
      factoryGenerator = BuildableFactoryGenerator,
      factoryFinder = BuildableFactoryFinder
    )
  }
}
