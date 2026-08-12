// port-lint: source lib.rs
package io.github.kotlinmania.winres

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
