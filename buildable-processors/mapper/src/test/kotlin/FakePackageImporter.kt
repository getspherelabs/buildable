import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.spherelabs.mapper.getName
import io.spherelabs.mapper.importer.Importer
import io.spherelabs.mapper.importer.Imports

class FakePackageImporter(val typeParams: List<String>) : Importer {

   val imports = mutableSetOf<Imports>()

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
    return imports
      .filterNot { import -> typeParams.contains(import.className) }
      .joinToString(separator = "\n") { import -> "$import ${import.packageName}.${import.className}" } + "\n"

  }
}
