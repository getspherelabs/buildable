package io.spherelabs.buildableprocessors

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import io.spherelabs.buildableannotations.BuildableComponent

class BuildableFactoryProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
    private val factoryGenerator: Generator,
    private val factoryFinder: Finder
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("BuildableFactoryProcessor was invoked.")
        val factories = factoryFinder(resolver)
        val components = components(resolver, factories)

        components.forEach {
            factoryGenerator(it.key, it.value).writeTo(codeGenerator, Dependencies(true))
        }

        return emptyList()
    }

    private fun components(
        resolver: Resolver,
        factories: Set<ClassName>
    ): Map<ClassName, List<ClassName>> {
        val result = mutableMapOf<ClassName, MutableList<ClassName>>()
        factories.forEach {
            result[it] = mutableListOf()
        }
        resolver.getSymbolsWithAnnotation(BuildableComponent::class.qualifiedName.orEmpty())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach { d ->
                d.superTypes
                    .map { it.resolve().declaration.closestClassDeclaration()?.toClassName() }
                    .filter { result.containsKey(it) }
                    .forEach { name ->
                        result[name]?.add(d.toClassName())
                    }
            }

        return result
    }
}