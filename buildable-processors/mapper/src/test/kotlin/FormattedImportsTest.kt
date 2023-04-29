import io.spherelabs.mapper.importer.Importer
import org.junit.Before
import org.junit.Test

class FormattedImportsTest {

  private lateinit var importer: Importer

  @Before
  fun setup() {
    importer = FakePackageImporter(listOf("io.behzod.mapper","Notification"))
  }

  @Test
  fun `check the value inserted to imports`() {
  }
}
