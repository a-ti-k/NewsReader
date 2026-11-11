# Daily Brief — Android News Reader

<p align="center">
  <img src="app/src/main/res/drawable/ic_splash_icon.svg" width="96" alt="Daily Brief Logo">
</p>

> **An open-source native Android RSS reader built with Kotlin, Jetpack Compose, OkHttp, and Android WebView.**

---

## 💡 Origin & Vision
This application was conceptualized and brought to life based on an inspiring idea by **[Your Name / User]**, aiming to deliver a fast, clean, distraction-free, and privacy-respecting daily news briefing app without requiring paid news APIs, external subscriptions, or server infrastructure.

---

## ✨ Features

- **Categorized Feed Discovery**: Home screen organized into National, World, Politics, Business, Technology, Sports, and Entertainment categories.
- **Localized Prioritization**: National category highlights regional publishers such as *The Daily Star*, *bdnews24.com*, *Prothom Alo*, and *Dhaka Tribune*.
- **Modern Splash Screen**: Integrated Android 12+ SplashScreen API with seamless background feed pre-loading so headlines are instantly ready upon launch.
- **In-App Article Reader**: Opens original publisher articles smoothly in an embedded WebView with robust system Back navigation support.
- **Robust Networking & Parsing**: Parallel feed loading (`Dispatchers.IO`), intelligent duplicate removal, date sorting, and fallback error states.
- **Zero Configuration / API Keys**: Fully driven by a lightweight local JSON configuration (`news_sources.json`). No accounts, analytics, ads, tracking, or cloud backend required.

---

## 🏗️ Architecture & Technical Stack

- **UI**: Jetpack Compose + Material 3 design system with custom color palettes and responsive layouts.
- **Splash Screen**: `androidx.core:core-splashscreen` with active data pre-loading condition.
- **Networking**: OkHttp with custom User-Agent headers and timeouts.
- **Parsing**: Android `XmlPullParser` supporting standard RSS and Atom feeds.
- **Min SDK**: 24 | **Compile/Target SDK**: 36 | **Java/Kotlin**: Java 17 & Kotlin 2.2+.

---

## 📂 Project Structure

```text
NewsReader/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/news_sources.json
│       ├── res/
│       │   ├── drawable/
│       │   ├── mipmap/
│       │   └── values/
│       └── java/com/example/newsreader/
│           ├── MainActivity.kt
│           ├── NewsModels.kt
│           └── NewsRepository.kt
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Getting Started

1. Clone or download this repository.
2. Open the project in **Android Studio** (Koala or newer recommended).
3. Allow Gradle sync to complete and download dependencies.
4. Run the app on an Android emulator or physical device (Android 7.0+ / API 24+).

---

## 🤝 Contributing & Open Source
Contributions, feature suggestions, and new RSS source additions are welcome! Feel free to open an issue or submit a pull request.

---

## 📜 License
This project is open-source software made available under the [MIT License](LICENSE).
