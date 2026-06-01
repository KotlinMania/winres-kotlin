// port-lint: tests lib.rs
package io.github.kotlinmania.winres

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowsResourceTest {
    @Test
    fun stringEscaping() {
        assertEquals("", escapeResourceString(""))
        assertEquals("foo", escapeResourceString("foo"))
        assertEquals("\"\"Hello\"\"", escapeResourceString("\"Hello\""))
        assertEquals("C:\\\\Program Files\\\\Foobar", escapeResourceString("C:\\Program Files\\Foobar"))
    }

    @Test
    fun toolkitIncludeWin10() {
        val result = winSdkIncludeRoot(
            "C:\\Program Files (x86)\\Windows Kits\\10\\bin\\10.0.17763.0\\x64\\rc.exe",
        )
        assertEquals("C:\\Program Files (x86)\\Windows Kits\\10\\Include\\10.0.17763.0", result)
    }

    @Test
    fun toolkitIncludeWin8() {
        val result = winSdkIncludeRoot(
            "C:\\Program Files (x86)\\Windows Kits\\8.1\\bin\\x86\\rc.exe",
        )
        assertEquals("C:\\Program Files (x86)\\Windows Kits\\8.1\\Include", result)
    }
}
