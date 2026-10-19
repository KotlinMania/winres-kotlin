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
