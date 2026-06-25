# MrDanielHlg Linux Terminal

<p align="center">
  <strong>Advanced Linux Terminal for Android — built by MrDanielHlg</strong>
</p>

## ✨ Innovative Features

### 🤖 AI Command Suggestion (Offline)
- Learns from your command history using frequency + context bigram model
- Suggests commands as you type — completely offline
- Built-in cheat sheet for 100+ Linux/Android commands
- Gets smarter the more you use it

### 🎭 Named Sessions with Color Coding
- Create named sessions (e.g., "Python Dev", "SSH Server", "Git Work")
- Color-coded for instant visual identification
- Pin important sessions to the top
- Auto-remembers last working directory per session
- Add notes and tags to sessions

### 📜 Quick Script Library
- Save and organize your most-used commands/scripts
- Categories: System, Network, Git, Python, Files, Custom
- `{{VARIABLE}}` placeholders — app prompts you to fill them before running
- Built-in useful scripts pre-loaded (system info, network check, git tools...)
- One-tap execution, export/import as JSON

### 🎨 Advanced Theme Engine
- 10+ professional themes: Dracula, Nord, Gruvbox, One Dark, Monokai, Tokyo Night, Matrix, Cyberpunk...
- Full 16-color ANSI palette customization
- Font size control (8–32sp)
- Background opacity — see your wallpaper through the terminal
- Cursor style: block / underline / bar
- Auto dark/light mode based on time of day

### 📊 Real-Time System Monitor
- CPU usage, RAM, storage, battery, network stats
- Reads directly from `/proc` and `/sys` — no root needed
- Shows compact status bar inside the terminal
- Full-screen detailed report on demand

### 👆 Gesture Command Mapper
- Map swipe gestures to actions (switch sessions, paste, clear screen, open scripts...)
- Single and two-finger swipes
- Double tap, long press, pinch in/out
- Fully configurable per session

## 📱 Device Compatibility

- **Target:** arm64-v8a (Android 5.0+)
- **Optimized for:** Tecno KL4, Unisoc T615, 3GB RAM devices

## 🏗 Building from Source

### Via GitHub Actions (Recommended)
1. Fork this repository
2. Push to `master` branch
3. GitHub Actions will automatically build the arm64 APK
4. Download from the **Actions** → latest run → **Artifacts**

### Manual Build
```bash
git clone https://github.com/YOUR_USERNAME/mrdanielhlg-linux
cd mrdanielhlg-linux
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/
```

## 📦 Package Info

| Property | Value |
|---|---|
| Package Name | `com.mrdanielhlg.linux` |
| App Name | MrDanielHlg Linux Terminal |
| Target ABI | arm64-v8a |
| Min SDK | Android 5.0 (API 21) |
| Target SDK | Android 9 (API 28) |

## 📜 License

Based on [Termux](https://github.com/termux/termux-app) (GPL-3.0).
New features © MrDanielHlg.
