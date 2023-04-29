package io.spherelabs.mapper.importer

import com.google.devtools.ksp.symbol.KSType

interface Importer {
  fun add(packageName: String,className: String)
  fun add(ksType: KSType)
  fun format(): String
}
