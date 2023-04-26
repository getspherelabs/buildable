package io.spherelabs.mapper

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class BuildableMapperProcessorProvider : SymbolProcessorProvider {

  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return BuildableMapperProcessor(
      codeGenerator = environment.codeGenerator,
      logger = environment.logger
    )
  }
}
