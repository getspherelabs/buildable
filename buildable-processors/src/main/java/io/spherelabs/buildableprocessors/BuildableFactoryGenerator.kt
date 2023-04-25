package io.spherelabs.buildableprocessors

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

object BuildableFactoryGenerator : Generator {

    override fun invoke(key: ClassName, list: List<ClassName>): FileSpec {
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
}