# winres-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fwinres--kotlin-blue.svg)](https://github.com/KotlinMania/winres-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/winres-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/winres-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/winres-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/winres-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`mxre/winres`](https://github.com/mxre/winres).

**Original Project:** This port is based on [`mxre/winres`](https://github.com/mxre/winres). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `mxre/winres`

> The text below is reproduced and lightly edited from [`https://github.com/mxre/winres`](https://github.com/mxre/winres). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## winres

A simple library to facilitate adding metainformation and icons to windows
executables and dynamic libraries.

[Documentation](https://docs.rs/winres/*/winres/)

## Toolkit

Before we begin you need to have the approptiate tools installed.
 - `rc.exe` from the [Windows SDK]
 - `windres.exe` and `ar.exe` from [minGW64]
 
[Windows SDK]: https://developer.microsoft.com/en-us/windows/downloads/windows-10-sdk
[minGW64]: http://mingw-w64.org

If you are using Rust with the MSVC ABI you will need the Windows SDK
for the GNU ABI you'll need minGW64.

Windows SDK can be found in the registry, minGW64 has to be in the path.

## Using winres

First, you will need to add a build script to your crate (`build.rs`)
by adding it to your crate's `Cargo.toml` file:

```toml
[package]
#...
build = "build.rs"

[build-dependencies]
winres = "0.1"
```

Next, you have to write a build script. A short
example is shown below.

```rust
// build.rs

extern crate winres;

fn main() {
  if cfg!(target_os = "windows") {
    let mut res = winres::WindowsResource::new();
    res.set_icon("test.ico");
    res.compile().unwrap();
  }
}
```

Thats it. The file `test.ico` should be located in the same directory as `build.rs`.
Metainformation (like program version and description) is taken from `Cargo.toml`'s `[package]`
section.

Note that using this crate on non windows platform is undefined behavior. It does not contain
safeguards against doing so. None-the-less it will compile; however `build.rs`, as shown above, should contain
a `cfg` option.

Another possibility is using `cfg` as a directive to avoid building `winres` on unix platforms
alltogether. This will save build time. So the example from before could look like this

```toml
[package]
#...
build = "build.rs"

[target.'cfg(windows)'.build-dependencies]
winres = "0.1"
```

Next, you have to write a build script. A short
example is shown below.

```rust
// build.rs

#[cfg(windows)]
extern crate winres;

#[cfg(windows)]
fn main() {
    let mut res = winres::WindowsResource::new();
    res.set_icon("test.ico");
    res.compile().unwrap();
}

#[cfg(unix)]
fn main() {
}
```

## Additional Options

For added convenience, `winres` parses, `Cargo.toml` for a `package.metadata.winres` section:

```toml
[package.metadata.winres]
OriginalFilename = "PROGRAM.EXE"
LegalCopyright = "Copyright © 2016"
#...
```

This section may contain arbitrary string key-value pairs, to be included
in the version info section of the executable/library file.

The following keys have special meanings and will be shown in the file properties
of the Windows Explorer:

`FileDescription`, `ProductName`, `ProductVersion`, `OriginalFilename` and `LegalCopyright`

See [MSDN]
for more details on the version info section of executables/libraries.

[MSDN]: https://msdn.microsoft.com/en-us/library/windows/desktop/aa381058.aspx

## About this project

I've written this crate chiefly for my personal projects and although I've tested it
on my personal computers I have no idea if the behaviour is the same everywhere.

To be brief, I'm very much reliant on your bug reports and feature suggestions
to make this crate better.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:winres-kotlin:0.1.0-SNAPSHOT")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`mxre/winres`](https://github.com/mxre/winres). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the winres authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`mxre/winres`](https://github.com/mxre/winres) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
