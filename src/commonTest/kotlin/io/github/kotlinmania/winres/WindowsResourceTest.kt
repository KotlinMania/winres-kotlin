// port-lint: tests winres/lib.rs
package io.github.kotlinmania.winres

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowsResourceTest {
    @Test
    fun stringEscaping() {
        assertEquals("", escapeString(""))
        assertEquals("foo", escapeString("foo"))
        assertEquals("\"\"Hello\"\"", escapeString("\"Hello\""))
        assertEquals("C:\\\\Program Files\\\\Foobar", escapeString("C:\\Program Files\\Foobar"))
        assertEquals("", escapeResourceString(""))
        assertEquals("foo", escapeResourceString("foo"))
    }

    @Test
    fun toolkitIncludeWin10() {
        val result =
            winSdkIncludeRoot(
                "C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17763.0\\x64\\rc.exe",
            )
        assertEquals("C:\\Program Files (x86)\\Windows Kits\\10\\Include\\10.0.17763.0", result)
        assertEquals(result, winSdkInlcudeRoot("C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17763.0\\x64\\rc.exe"))
    }

    @Test
    fun toolkitIncludeWin8() {
        val result =
            winSdkIncludeRoot(
                "C:\\Program Files (x86)\\Windows Kits\\8.1\\bin\\x86\\rc.exe",
            )
        assertEquals("C:\\Program Files (x86)\\Windows Kits\\8.1\\Include", result)
        assertEquals(result, winSdkInlcudeRoot("C:\\Program Files (x86)\\Windows Kits\\8.1\\bin\\x86\\rc.exe"))
    }

    @Test
    fun testParseCargoToml() {
        val toml =
            """
            [package]
            name = "my-app"
            version = "0.1.0"

            [package.metadata.winres]
            OriginalFilename = "testing.exe"
            FileDescription = "My Winres App"
            LegalCopyright = "Copyright 2026"
            """.trimIndent()

        val props = mutableMapOf<String, String>()
        val ok = parseCargoToml(props, toml)
        assertTrue(ok)
        assertEquals("testing.exe", props["OriginalFilename"])
        assertEquals("My Winres App", props["FileDescription"])
        assertEquals("Copyright 2026", props["LegalCopyright"])
    }

    @Test
    fun testWindowsResourceGeneration() {
        val res =
            WindowsResource()
                .set("InternalName", "TEST.EXE")
                .set("FileDescription", "Sample Application")
                .setIcon("app.ico")
                .setIconWithId("doc.ico", "2")
                .setLanguage(0x0409u)
                .setVersionInfo(VersionInfo.PRODUCTVERSION, 0x0001000200030004uL)
                .setVersionInfo(VersionInfo.FILEVERSION, 0x0001000000000000uL)
                .appendRcContent("100 MENU { MENUITEM \"&Exit\", 101 }")

        val rc = res.writeResourceText()
        assertTrue(rc.contains("#pragma code_page(65001)"))
        assertTrue(rc.contains("1 VERSIONINFO"))
        assertTrue(rc.contains("VALUE \"InternalName\", \"TEST.EXE\""))
        assertTrue(rc.contains("VALUE \"FileDescription\", \"Sample Application\""))
        assertTrue(rc.contains("1 ICON \"app.ico\""))
        assertTrue(rc.contains("2 ICON \"doc.ico\""))
        assertTrue(rc.contains("BLOCK \"040904b0\""))
        assertTrue(rc.contains("100 MENU { MENUITEM \"&Exit\", 101 }"))
    }

    @Test
    fun testManifestEmbedding() {
        val manifestXml =
            """
            <assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0">
                <trustInfo xmlns="urn:schemas-microsoft-com:asm.v3">
                </trustInfo>
            </assembly>
            """.trimIndent()

        val res =
            WindowsResource()
                .setManifest(manifestXml)

        val rc = res.writeResourceText()
        assertTrue(rc.contains("1 24"))
        assertTrue(rc.contains("\" <assembly xmlns=\"\"urn:schemas-microsoft-com:asm.v1\"\" manifestVersion=\"\"1.0\"\"> \""))

        val resWithFile =
            WindowsResource()
                .setManifestFile("app.manifest")

        val rcFile = resWithFile.writeResourceText()
        assertTrue(rcFile.contains("1 24 \"app.manifest\""))
    }

    @Test
    fun testBuilderMethods() {
        val res =
            WindowsResource
                .new()
                .setToolkitPath("C:\\SDK")
                .setResourceFile("custom.rc")
                .setOutputDirectory("build/out")
                .setWindresPath("x86_64-w64-mingw32-windres")
                .setArPath("x86_64-w64-mingw32-ar")
                .addToolkitInclude(true)

        assertEquals("C:\\SDK", res.toolkitPath)
        assertEquals("custom.rc", res.rcFile)
        assertEquals("build/out", res.outputDirectory)
        assertEquals("x86_64-w64-mingw32-windres", res.windresPath)
        assertEquals("x86_64-w64-mingw32-ar", res.arPath)
        assertTrue(res.addToolkitInclude)
        assertTrue(res.compile())
        assertTrue(res.compileWithToolkitGnu("test.rc", "build/out"))
        assertTrue(res.compileWithToolkitMsvc("test.rc", "build/out"))
        assertEquals(emptyList<String>(), getSdk())
    }
}
