// port-lint: source lib.rs
package io.github.kotlinmania.winres

/*
 * Copyright (c) 2016 Max Resch
 * Copyright (c) 2025 Sydney Renee, The Solace Project
 *
 * Permission is hereby granted, free of charge, to any
 * person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the
 * Software without restriction, including without
 * limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
 * ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

/**
 * Version info field names used in Windows VERSIONINFO resources.
 */
public enum class VersionInfo {
    FILEVERSION,
    PRODUCTVERSION,
    FILEOS,
    FILETYPE,
    FILESUBTYPE,
    FILEFLAGSMASK,
    FILEFLAGS,
}

private data class Icon(
    val path: String,
    val nameId: String,
)

/**
 * Windows resource description that can render the `.rc` text used by Windows
 * resource compilers.
 */
public class WindowsResource {
    public companion object {
        /** Creates a new default WindowsResource instance. */
        public fun new(): WindowsResource = WindowsResource()
    }

    private val properties: MutableMap<String, String> = linkedMapOf()
    private val versionInfo: MutableMap<VersionInfo, ULong> =
        linkedMapOf(
            VersionInfo.FILEOS to 0x00040004u,
            VersionInfo.FILETYPE to 1u,
            VersionInfo.FILESUBTYPE to 0u,
            VersionInfo.FILEFLAGSMASK to 0x3Fu,
            VersionInfo.FILEFLAGS to 0u,
        )
    private val icons: MutableList<Icon> = mutableListOf()
    private var language: UShort = 0u
    private var manifest: String? = null
    private var manifestFile: String? = null
    private var appendRcContent: String = ""
    public var toolkitPath: String = ""
        private set
    public var rcFile: String? = null
        private set
    public var outputDirectory: String = "."
        private set
    public var windresPath: String = "windres"
        private set
    public var arPath: String = "ar"
        private set
    public var addToolkitInclude: Boolean = false
        private set

    public fun setToolkitPath(path: String): WindowsResource {
        this.toolkitPath = path
        return this
    }

    public fun setResourceFile(path: String): WindowsResource {
        this.rcFile = path
        return this
    }

    public fun setOutputDirectory(path: String): WindowsResource {
        this.outputDirectory = path
        return this
    }

    public fun setWindresPath(path: String): WindowsResource {
        this.windresPath = path
        return this
    }

    public fun setArPath(path: String): WindowsResource {
        this.arPath = path
        return this
    }

    public fun addToolkitInclude(add: Boolean): WindowsResource {
        this.addToolkitInclude = add
        return this
    }

    public fun set(name: String, value: String): WindowsResource {
        properties[name] = value
        return this
    }

    public fun setLanguage(language: UShort): WindowsResource {
        this.language = language
        return this
    }

    public fun setIcon(path: String): WindowsResource = setIconWithId(path, "1")

    public fun setIconWithId(path: String, nameId: String): WindowsResource {
        icons += Icon(path = path, nameId = nameId)
        return this
    }

    public fun setVersionInfo(field: VersionInfo, value: ULong): WindowsResource {
        versionInfo[field] = value
        return this
    }

    public fun setManifest(manifest: String): WindowsResource {
        manifestFile = null
        this.manifest = manifest
        return this
    }

    public fun setManifestFile(file: String): WindowsResource {
        manifestFile = file
        manifest = null
        return this
    }

    public fun appendRcContent(content: String): WindowsResource {
        if (appendRcContent.isNotEmpty() && !appendRcContent.endsWith('\n')) {
            appendRcContent += "\n"
        }
        appendRcContent += content
        return this
    }

    public fun writeResourceText(): String =
        buildString {
            appendLine("#pragma code_page(65001)")
            appendLine("1 VERSIONINFO")
            for ((field, value) in versionInfo) {
                when (field) {
                    VersionInfo.FILEVERSION,
                    VersionInfo.PRODUCTVERSION,
                    -> appendLine("${field.name} ${value.word(48)}, ${value.word(32)}, ${value.word(16)}, ${value.word(0)}")

                    else -> appendLine("${field.name} ${value.toHexLiteral()}")
                }
            }
            appendLine("{")
            appendLine("BLOCK \"StringFileInfo\"")
            appendLine("{")
            appendLine("BLOCK \"${language.toInt().toString(16).padStart(4, '0')}04b0\"")
            appendLine("{")
            for ((key, value) in properties) {
                if (value.isNotEmpty()) {
                    appendLine("VALUE \"${escapeResourceString(key)}\", \"${escapeResourceString(value)}\"")
                }
            }
            appendLine("}")
            appendLine("}")
            appendLine("BLOCK \"VarFileInfo\" {")
            appendLine("VALUE \"Translation\", 0x${language.toInt().toString(16)}, 0x04b0")
            appendLine("}")
            appendLine("}")
            for (icon in icons) {
                appendLine("${escapeResourceString(icon.nameId)} ICON \"${escapeResourceString(icon.path)}\"")
            }
            val fileType = versionInfo[VersionInfo.FILETYPE]
            if (fileType != null) {
                val manifestText = manifest
                val manifestPath = manifestFile
                when {
                    manifestText != null -> {
                        appendLine("${fileType.toInt()} 24")
                        appendLine("{")
                        for (line in manifestText.lines()) {
                            appendLine("\" ${escapeResourceString(line.trim())} \"")
                        }
                        appendLine("}")
                    }

                    manifestPath != null -> appendLine("${fileType.toInt()} 24 \"${escapeResourceString(manifestPath)}\"")
                }
            }
            appendLine(appendRcContent)
        }
}

internal fun escapeResourceString(string: String): String =
    buildString {
        for (character in string) {
            when (character) {
                '"' -> append("\"\"")
                '\'' -> append("\\'")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
    }

internal fun winSdkInlcudeRoot(path: String): String {
    val parts = path.split('\\')
    val binIndex = parts.indexOf("bin")
    if (binIndex < 0 || binIndex == 0) return ""

    val root = parts.take(binIndex).toMutableList()
    root += "Include"
    val version = parts.getOrNull(binIndex + 1)
    if (version != null && version.startsWith("10.")) {
        root += version
    }
    return root.joinToString("\\")
}

private fun ULong.word(shift: Int): Int = ((this shr shift) and 0xffffu).toInt()

private fun ULong.toHexLiteral(): String = "0x${toString(16)}"
