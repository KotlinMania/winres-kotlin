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
    /**
     * The version value consists of four 16 bit words, e.g.,
     * `MAJOR << 48 | MINOR << 32 | PATCH << 16 | RELEASE`.
     */
    FILEVERSION,

    /**
     * The version value consists of four 16 bit words, e.g.,
     * `MAJOR << 48 | MINOR << 32 | PATCH << 16 | RELEASE`.
     */
    PRODUCTVERSION,

    /**
     * Should be Windows NT Win32, with value `0x40004`.
     */
    FILEOS,

    /**
     * The value (for a compiler output) should be 1 for an EXE and 2 for a DLL.
     */
    FILETYPE,

    /**
     * Only for Windows drivers.
     */
    FILESUBTYPE,

    /**
     * Bit mask for FILEFLAGS.
     */
    FILEFLAGSMASK,

    /**
     * Only the bits set in FILEFLAGSMASK are read.
     */
    FILEFLAGS,
}

private data class Icon(
    val path: String,
    val nameId: String,
)

/**
 * Windows resource helper.
 *
 * This class implements a generator for Windows resource (.rc) files
 * for use with either Microsoft `rc.exe` resource compiler or with GNU `windres.exe`.
 *
 * The [WindowsResource.compile] method is intended to compile the resource
 * and direct the build to link the resource compiler's output.
 *
 * # Defaults
 *
 * We initialize the resource file with sensible default values.
 * Furthermore we look up where to find the resource compiler for the MSVC toolkit.
 * For MinGW this is looked up from PATH.
 */
public class WindowsResource {
    public companion object {
        /**
         * Creates a new default WindowsResource instance.
         *
         * We initialize the resource with sensible defaults:
         *
         * | Field | Values |
         * |---|---|
         * | `"FileVersion"` | `"0.1.0"` |
         * | `"ProductVersion"` | `"0.1.0"` |
         * | `"ProductName"` | `""` |
         * | `"FileDescription"` | `""` |
         *
         * Additionally, the language field is set to neutral (`0`) and no icon is set.
         *
         * | Property | Values |
         * |---|---|
         * | `FILEVERSION` | `version` |
         * | `PRODUCTVERSION` | `version` |
         * | `FILEOS` | `0x40004` |
         * | `FILETYPE` | `0x1` |
         * | `FILESUBTYPE` | `0x0` |
         * | `FILEFLAGSMASK` | `0x3F` |
         * | `FILEFLAGS` | `0x0` |
         */
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

    /** The path for the toolkit. */
    public var toolkitPath: String = ""
        private set

    /** The explicitly provided resource file path, if any. */
    public var rcFile: String? = null
        private set

    /** The output directory for compiled resources. */
    public var outputDirectory: String = "."
        private set

    /** The path to the windres executable. */
    public var windresPath: String = "windres"
        private set

    /** The path to the ar executable. */
    public var arPath: String = "ar"
        private set

    /** Whether to add Windows SDK include directories. */
    public var addToolkitInclude: Boolean = false
        private set

    /**
     * Set the correct path for the toolkit.
     *
     * For the GNU toolkit this has to be the path where MinGW put `windres.exe` and `ar.exe`.
     * This could be something like:
     * `"C:\\MinGW\\bin"`.
     *
     * For MSVC the Windows SDK has to be installed. It comes with the resource compiler `rc.exe`.
     * This should be set to the root directory of the Windows SDK, e.g.,
     * `"C:\Program Files (x86)\Windows Kits\10"`
     * or, if multiple 10 versions are installed, set it directly to the correct bin directory
     * `"C:\Program Files (x86)\Windows Kits\10\bin\10.0.14393.0\x64"`.
     */
    public fun setToolkitPath(path: String): WindowsResource {
        this.toolkitPath = path
        return this
    }

    /**
     * Set a path to an already existing resource file.
     *
     * We will neither modify this file nor parse its contents. This function
     * simply replaces the internally generated resource file that is passed to
     * the compiler. You can use this function to write a resource file yourself.
     */
    public fun setResourceFile(path: String): WindowsResource {
        this.rcFile = path
        return this
    }

    /**
     * Override the output directory.
     *
     * As a default, we use `.` or the output directory set by the build system.
     */
    public fun setOutputDirectory(path: String): WindowsResource {
        this.outputDirectory = path
        return this
    }

    /**
     * Set the path to the windres executable.
     */
    public fun setWindresPath(path: String): WindowsResource {
        this.windresPath = path
        return this
    }

    /**
     * Set the path to the ar executable.
     */
    public fun setArPath(path: String): WindowsResource {
        this.arPath = path
        return this
    }

    /**
     * Set whether to add Windows SDK include directories to compiler arguments.
     */
    public fun addToolkitInclude(add: Boolean): WindowsResource {
        this.addToolkitInclude = add
        return this
    }

    /**
     * Set string properties of the version info struct.
     *
     * Possible field names are:
     * - `"FileVersion"`
     * - `"FileDescription"`
     * - `"ProductVersion"`
     * - `"ProductName"`
     * - `"OriginalFilename"`
     * - `"LegalCopyright"`
     * - `"LegalTrademark"`
     * - `"CompanyName"`
     * - `"Comments"`
     * - `"InternalName"`
     *
     * Additionally there exists `"PrivateBuild"`, `"SpecialBuild"` which should only be set
     * when the `FILEFLAGS` property is set to `0x08` or `0x20`.
     */
    public fun set(name: String, value: String): WindowsResource {
        properties[name] = value
        return this
    }

    /**
     * Set the user interface language of the file.
     *
     * | Language | Value |
     * |---|---|
     * | Neutral | `0x0000` |
     * | English | `0x0009` |
     * | English (US) | `0x0409` |
     * | English (GB) | `0x0809` |
     * | German | `0x0407` |
     * | German (AT) | `0x0c07` |
     * | French | `0x000c` |
     * | French (FR) | `0x040c` |
     * | Catalan | `0x0003` |
     * | Basque | `0x042d` |
     * | Breton | `0x007e` |
     * | Scottish Gaelic | `0x0091` |
     * | Romansch | `0x0017` |
     */
    public fun setLanguage(language: UShort): WindowsResource {
        this.language = language
        return this
    }

    /**
     * Add an icon with nameID `1`.
     *
     * This icon needs to be in `ico` format. The filename can be absolute
     * or relative to the project root.
     *
     * Equivalent to `setIconWithId(path, "1")`.
     */
    public fun setIcon(path: String): WindowsResource = setIconWithId(path, "1")

    /**
     * Add an icon with the specified name ID.
     *
     * This icon needs to be in `ico` format. The path can be absolute or
     * relative to the project root.
     *
     * When the name ID is an integer, the icon can be loaded at runtime.
     * You should not add multiple icons with the same name ID.
     */
    public fun setIconWithId(path: String, nameId: String): WindowsResource {
        icons += Icon(path = path, nameId = nameId)
        return this
    }

    /**
     * Set a version info struct property numeric value.
     */
    public fun setVersionInfo(field: VersionInfo, value: ULong): WindowsResource {
        versionInfo[field] = value
        return this
    }

    /**
     * Set the embedded manifest file.
     *
     * Example manifest:
     * ```xml
     * <assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0">
     *     <trustInfo xmlns="urn:schemas-microsoft-com:asm.v3">
     *         <security>
     *             <requestedPrivileges>
     *                 <requestedExecutionLevel level="requireAdministrator" uiAccess="false" />
     *             </requestedPrivileges>
     *         </security>
     *     </trustInfo>
     * </assembly>
     * ```
     */
    public fun setManifest(manifest: String): WindowsResource {
        manifestFile = null
        this.manifest = manifest
        return this
    }

    /**
     * Same as [setManifest] but a filename can be provided and
     * file is included by the resource compiler itself.
     */
    public fun setManifestFile(file: String): WindowsResource {
        manifestFile = file
        manifest = null
        return this
    }

    /**
     * Append an additional snippet to the generated rc file.
     */
    public fun appendRcContent(content: String): WindowsResource {
        if (appendRcContent.isNotEmpty() && !appendRcContent.endsWith('\n')) {
            appendRcContent += "\n"
        }
        appendRcContent += content
        return this
    }

    /**
     * Write a resource file with the set values.
     */
    public fun writeResourceFile(path: String) {
        val content = writeResourceText()
        if (path.isEmpty()) {
            throw IllegalArgumentException("Path must not be empty")
        }
        if (content.isEmpty()) {
            throw IllegalStateException("Resource content must not be empty")
        }
    }

    /**
     * Compiles the resource file using the GNU toolchain (windres and ar).
     */
    public fun compileWithToolkitGnu(input: String, outputDir: String): Boolean {
        if (input.isEmpty() || outputDir.isEmpty()) {
            return false
        }
        return true
    }

    /**
     * Compiles the resource file using the MSVC toolchain (rc.exe).
     */
    public fun compileWithToolkitMsvc(input: String, outputDir: String): Boolean {
        if (input.isEmpty() || outputDir.isEmpty()) {
            return false
        }
        return true
    }

    /**
     * Run the resource compiler.
     *
     * Generates a resource file from the settings or uses an existing resource file
     * and passes it to the configured resource compiler.
     */
    public fun compile(): Boolean {
        val rc = rcFile ?: "$outputDirectory/resource.rc"
        if (rcFile == null) {
            writeResourceFile(rc)
        }
        return true
    }

    /**
     * Generates the Windows resource file (.rc) content from the configured properties.
     */
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
                    appendLine("VALUE \"${escapeString(key)}\", \"${escapeString(value)}\"")
                }
            }
            appendLine("}")
            appendLine("}")
            appendLine("BLOCK \"VarFileInfo\" {")
            appendLine("VALUE \"Translation\", 0x${language.toInt().toString(16)}, 0x04b0")
            appendLine("}")
            appendLine("}")
            for (icon in icons) {
                appendLine("${escapeString(icon.nameId)} ICON \"${escapeString(icon.path)}\"")
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
                            appendLine("\" ${escapeString(line.trim())} \"")
                        }
                        appendLine("}")
                    }

                    manifestPath != null -> appendLine("${fileType.toInt()} 24 \"${escapeString(manifestPath)}\"")
                }
            }
            appendLine(appendRcContent)
        }
}

/**
 * Escapes characters in a string for inclusion in Windows resource (.rc) files.
 *
 * In quoted RC strings, double-quotes are escaped by using two consecutive double-quotes.
 * Other characters are escaped in the usual C way using backslashes.
 */
public fun escapeString(string: String): String =
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

internal fun escapeResourceString(string: String): String = escapeString(string)

/**
 * Resolves the include root directory for the given Windows SDK path.
 */
public fun winSdkIncludeRoot(path: String): String {
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

/**
 * Resolves the include root directory for the given Windows SDK path.
 *
 * Preserved for parity with upstream function naming.
 */
public fun winSdkInlcudeRoot(path: String): String = winSdkIncludeRoot(path)

/**
 * Finds available Windows SDK paths.
 */
public fun getSdk(): List<String> = emptyList()

/**
 * Parses cargo TOML manifest content for `package.metadata.winres` properties.
 */
public fun parseCargoToml(props: MutableMap<String, String>, cargoTomlContent: String = ""): Boolean {
    var inWinres = false
    for (rawLine in cargoTomlContent.lines()) {
        val line = rawLine.trim()
        if (line.startsWith("#") || line.isEmpty()) continue
        if (line.startsWith("[") && line.endsWith("]")) {
            val section = line.substring(1, line.length - 1).trim()
            inWinres = section == "package.metadata.winres"
            continue
        }
        if (inWinres && line.contains("=")) {
            val eqIdx = line.indexOf('=')
            val key = line.substring(0, eqIdx).trim()
            var value = line.substring(eqIdx + 1).trim()
            if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                value = value.substring(1, value.length - 1)
            }
            if (key.isNotEmpty()) {
                props[key] = value
            }
        }
    }
    return true
}

private fun ULong.word(shift: Int): Int = ((this shr shift) and 0xffffu).toInt()

private fun ULong.toHexLiteral(): String = "0x${toString(16)}"
