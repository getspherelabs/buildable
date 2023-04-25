package io.spherelabs.buildableprocessors

import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.ClassName

interface Finder {
    operator fun invoke(resolver: Resolver):Set<ClassName>
}