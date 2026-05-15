# LinuxQuest 🐧

**Master the Terminal. Own the System.**

An OverTheWire Bandit-inspired Android game that teaches Linux mastery through 56 progressive levels with a fully sandboxed, built-in terminal emulator.

## Features

- 🔒 **Fully Sandboxed** — Virtual filesystem + custom shell interpreter. Zero access to your real Android OS.
- 🖥️ **Built-in Terminal** — Complete terminal emulator with ANSI colors, tab completion, command history.
- 📚 **56 Progressive Levels** across 6 categories:
  - File System Basics (Levels 0-9)
  - Text Processing & Pipes (Levels 10-19)
  - Permissions & Users (Levels 20-29)
  - Advanced Data & Encoding (Levels 30-39)
  - Shell Scripting (Levels 40-49)
  - Master Challenges (Levels 50-55)
- ⚡ **60+ Linux Commands** — ls, grep, find, sed, awk, chmod, tar, curl, and many more.
- 🏆 **Achievements & Progress** — Stars, badges, password collection (Bandit-style).
- 📖 **Built-in Manual** — Searchable reference for all commands.
- 🌐 **100% Offline** — No internet required.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Terminal | Custom Canvas-based composable |
| Shell | Custom Kotlin interpreter |
| Filesystem | In-memory Virtual Filesystem |
| Persistence | Room DB + DataStore |
| Build | Gradle 8.7 + AGP 8.5.1 |

## Building the APK

### Prerequisites

1. **Android Studio** (Ladybug or later) — [Download](https://developer.android.com/studio)
2. **JDK 17+** — Usually bundled with Android Studio
3. **Android SDK 35** — Install via Android Studio SDK Manager

### Option A: Build with Android Studio (Recommended)

1. Open Android Studio
2. **File → Open** → Select the `linux-quest` folder
3. Wait for Gradle sync to complete
4. **Build → Generate Signed Bundle / APK**
   - Select **APK**
   - Create a new keystore or use an existing one
   - Choose **release** build variant
   - Click **Create**
5. APK will be at: `app/build/outputs/apk/release/app-release.apk`

### Option B: Build from Command Line

```bash
# Set Android SDK path
export ANDROID_HOME=$HOME/Android/Sdk   # Linux/Mac
set ANDROID_HOME=C:\Users\<user>\AppData\Local\Android\Sdk  # Windows

# Generate debug APK (no signing required)
./gradlew assembleDebug

# Generate release APK (requires signing config)
./gradlew assembleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
Release APK: `app/build/outputs/apk/release/app-release.apk`

### Installing on Android

```bash
adb install app-release.apk
```

Or transfer the APK to your phone and open it (enable "Install from unknown sources" first).

## Project Structure

```
linux-quest/
├── app/src/main/java/com/linuxquest/
│   ├── filesystem/     # Virtual filesystem (VNode, Permissions, VFS)
│   ├── shell/          # Shell interpreter (Parser, Pipeline, Commands)
│   ├── terminal/       # Terminal UI (Composable, ANSI, Keyboard)
│   ├── game/           # Game engine (Levels, Validator, Progress)
│   ├── data/           # Room DB + DataStore
│   └── ui/             # Compose screens + theme
└── app/build.gradle.kts
```

## Sandboxing

This app is **completely sandboxed**:
- ❌ No `Runtime.exec()` or `ProcessBuilder` — no real processes are spawned
- ❌ No `java.io.File` — no real filesystem access for game operations
- ❌ No network permissions — fully offline
- ✅ Virtual filesystem exists only in memory
- ✅ Shell interpreter only dispatches to our command implementations
- ✅ All "network" commands produce simulated output for educational purposes

## License

MIT
