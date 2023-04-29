package io.spherelabs.mapper.importer

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import io.spherelabs.mapper.getName

private const val IMPORT_STATEMENT = "import"

class PackageImporter(var targetClassTypeParameters: Set<KSTypeParameter> = mutableSetOf()) :
    Importer {

  private val imports = mutableSetOf<Imports>()

  override fun add(packageName: String, className: String) {
    imports.add(Imports(packageName, className))
  }

  override fun add(ksType: KSType) {
    val declaration: KSDeclaration = ksType.declaration
    val packageName: String = declaration.packageName.asString()
    val className: String = declaration.getName()

    imports.add(Imports(packageName, className))
  }

  override fun format(): String {
    val typeParams: List<String> = targetClassTypeParameters.map { parameter -> parameter.simpleName.asString() }

    return imports
      .filterNot { import -> typeParams.contains(import.className) }
      .joinToString(separator = "\n") { import -> "$IMPORT_STATEMENT ${import.packageName}.${import.className}" } + "\n"

  }
}
