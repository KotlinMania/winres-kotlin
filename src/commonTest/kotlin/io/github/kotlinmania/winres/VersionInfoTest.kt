// port-lint: tests winres/lib.rs
package io.github.kotlinmania.winres

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionInfoTest {
    @Test
    fun testVersionInfoEntries() {
        val entries = VersionInfo.entries
        assertEquals(7, entries.size)
        assertTrue(entries.contains(VersionInfo.FILEVERSION))
        assertTrue(entries.contains(VersionInfo.PRODUCTVERSION))
        assertTrue(entries.contains(VersionInfo.FILEOS))
        assertTrue(entries.contains(VersionInfo.FILETYPE))
        assertTrue(entries.contains(VersionInfo.FILESUBTYPE))
        assertTrue(entries.contains(VersionInfo.FILEFLAGSMASK))
        assertTrue(entries.contains(VersionInfo.FILEFLAGS))
    }
}
