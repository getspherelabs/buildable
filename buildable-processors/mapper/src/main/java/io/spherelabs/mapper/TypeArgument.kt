package io.spherelabs.mapper

import com.google.devtools.ksp.symbol.KSType

sealed class TypeArgument {
  class Argument(val ksType: KSType) : TypeArgument()
  object Wildcard : TypeArgument()
}
