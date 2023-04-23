package io.spherelabs.buildableprocessors

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import io.spherelabs.buildableannotations.BuildableComponent
import io.spherelabs.buildableannotations.BuildableFactory

class BuildableFactoryProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("BuildableFactoryProcessor was invoked.")
        val factories = factories(resolver)
        val components = components(resolver, factories)

        components.forEach {
            fileGeneration(it.key, it.value).writeTo(codeGenerator, Dependencies(true))
        }

        return emptyList()
    }

    private fun factories(resolver: Resolver): Set<ClassName> {
        return resolver.getSymbolsWithAnnotation(BuildableFactory::class.qualifiedName.orEmpty())
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .map { it.toClassName() }
            .toSet()
    }

    private fun fileGeneration(key: ClassName, list: List<ClassName>): FileSpec {
        val packageName = key.packageName
        val funcName = key.simpleName + "Factory"
        val enumName = key.simpleName + "Type"

        return FileSpec.builder(packageName, funcName)
            .addType(
                TypeSpec.enumBuilder(enumName)
                    .apply {
                        list.forEach {
                            addEnumConstant(it.simpleName.uppercase())
                        }
                    }
                    .build())
            .addFunction(
                FunSpec.builder(funcName)
                    .addParameter("key", ClassName(packageName, enumName))
                    .returns(key)
                    .beginControlFlow("return when (key)")
                    .apply {
                        list.forEach {
                            addStatement("${enumName}.${it.simpleName.uppercase()} -> %T()", it)
                        }
                    }
                    .endControlFlow()
                    .build())
            .build()
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