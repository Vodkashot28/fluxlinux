# App Development Stack

*Script: `setup_appdev_debian.sh`*

---

## Overview

Installs a complete Android/Flutter/Kotlin development environment on Debian ARM64.

---

## Java Development Kit

| Component | Version | Path |
|-----------|---------|------|
| OpenJDK | 21 (or 17 fallback) | `/usr/lib/jvm/java-21-openjdk-arm64` |

> **Why JDK 21?** Debian 13 Trixie ships with OpenJDK 21 as default. Required for Android Gradle Plugin 8.x and Flutter.

---

## Android SDK

| Component | Version | Path |
|-----------|---------|------|
| SDK Root | - | `/opt/android-sdk` |
| Command Line Tools | 11076708 | `/opt/android-sdk/cmdline-tools/latest` |
| Platform Tools | Latest | `/opt/android-sdk/platform-tools` |
| Build Tools | 35.0.2 (ARM64) | `/opt/android-sdk/build-tools/35.0.2` |
| Platform | Android 35 | `/opt/android-sdk/platforms/android-35` |

> **Why this version?** Command Line Tools 11076708 is stable with ARM64 compatibility. Build tools from [lzhiyong/android-sdk-tools](https://github.com/lzhiyong/android-sdk-tools) are ARM64-native.

---

## Android NDK

| Version | Location | Source |
|---------|----------|--------|
| r27d (27.3.13750724) | `/opt/android-sdk/ndk/27.3.13750724` | [HomuHomu833/android-ndk-custom](https://github.com/HomuHomu833/android-ndk-custom) |
| r29 (29.0.14206865) | `/opt/android-sdk/ndk/29.0.14206865` | [HomuHomu833/android-ndk-custom](https://github.com/HomuHomu833/android-ndk-custom) |

> **Why custom NDK?** Official Android NDK ships with x86-only binaries. Custom ARM64 NDKs are statically linked (musl-based) and work natively on ARM64 Linux.

---

## ADB/Fastboot

| Tool | Path | Notes |
|------|------|-------|
| adb | `/opt/android-sdk/platform-tools/adb` | Wrapper → `/usr/bin/adb` |
| fastboot | `/opt/android-sdk/platform-tools/fastboot` | Wrapper → `/usr/bin/fastboot` |

> SDK's x86 binaries are replaced with wrappers to Debian's ARM64-native tools.

---

## Flutter SDK

| Component | Version | Path |
|-----------|---------|------|
| Flutter SDK | Latest stable | `/opt/flutter` |
| Dart SDK | Bundled | `/opt/flutter/bin/cache/dart-sdk` |

---

## Gradle

| Component | Version | Path |
|-----------|---------|------|
| Gradle | 8.10.2+ | `/opt/gradle` |

> **Why not apt?** Debian ships ancient Gradle 4.4.1. Modern Android requires Gradle 8.x.

---

## Other Tools

| Tool | Source | Purpose |
|------|--------|---------|
| IntelliJ IDEA CE | JetBrains | IDE |
| cmake | apt | Native builds |
| ninja-build | apt | Build system |
| clang | apt | C/C++ compiler |
| chromium | apt | Chrome DevTools |

---

## Environment Variables

```bash
export ANDROID_HOME=/opt/android-sdk
export FLUTTER_ROOT=/opt/flutter
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:/opt/flutter/bin
export PATH=$PATH:/opt/gradle/bin
```

---

## Verification

```bash
flutter doctor
java -version
gradle --version
adb version
```
