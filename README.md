<p align="center">
  <svg width="560" height="140" viewBox="0 0 560 140" fill="none" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="RoomReady Brand Banner">
    <defs>
      <linearGradient id="mintGlow" x1="60" y1="30" x2="180" y2="115" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#00FF94" stop-opacity="0.28"/>
        <stop offset="100%" stop-color="#00EEDA" stop-opacity="0"/>
      </linearGradient>
      <linearGradient id="mintCheck" x1="82" y1="68" x2="122" y2="44" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stop-color="#00FF94"/>
        <stop offset="100%" stop-color="#00EEDA"/>
      </linearGradient>
    </defs>
    <rect width="560" height="140" rx="20" fill="#0F172A"/>
    <circle cx="96" cy="70" r="64" fill="url(#mintGlow)"/>
    <rect x="64" y="34" width="52" height="72" rx="6" fill="#090F1D" stroke="#334155" stroke-width="2"/>
    <path d="M65 35 L99 41 L99 99 L65 105 Z" fill="#FFFFFF" stroke="#0F172A" stroke-width="2.5" stroke-linejoin="round"/>
    <circle cx="92" cy="70" r="2.6" fill="#0F172A"/>
    <path d="M84 71 L95 82 L120 53" stroke="#0F172A" stroke-width="9" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M84 71 L95 82 L120 53" stroke="url(#mintCheck)" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/>
    <text x="150" y="76" fill="#FFFFFF" font-family="system-ui, -apple-system, Segoe UI, Roboto, sans-serif" font-size="38" font-weight="800" letter-spacing="-0.8">RoomReady</text>
    <rect x="150" y="90" width="56" height="4" rx="2" fill="#00F1CE"/>
    <text x="218" y="96" fill="#94A3B8" font-family="system-ui, -apple-system, Segoe UI, Roboto, sans-serif" font-size="13" font-weight="600" letter-spacing="1.2">OFFLINE-FIRST RESORT HOUSEKEEPING</text>
  </svg>
</p>

# RoomReady

> *I remixed my own Yard #6 entry — RoomReady, an offline housekeeping web app — into a native Android app that syncs with a live resort PMS. Same soul, new body, and for the first time, a heartbeat.*

RoomReady is an offline-first native Android housekeeping companion built in Kotlin and Jetpack Compose for resort staff at properties like Krishnachura Retreat (`demo13`), enabling reliable room turnover workflows (`Dirty → Cleaning → Cleaned → Ready`), 8-step inspection checklists, and maintenance ticketing on lightweight 2 GB RAM devices with bilingual English and Bengali parity and encrypted PMS synchronization.

## The remix story

Yard #6 produced RoomReady as an offline housekeeping **web app**. For Yard #7 it was rebuilt as a **native Android app** against the same product contract: the same flow, the same 8-step checklist, the same offline-first soul — plus a live sync heartbeat with the resort PMS housekeeping API. Nothing about the PMS changed; the new body just learned to talk to it.

*The PMS is my existing production product; this Android app was built during Yard #7 week and syncs with the PMS housekeeping API.*

## Screenshots

The six store screenshots are captured on-device from the real app (see [the screenshot runbook](playstore/screenshots/SCREENS.md)) and framed with ink captions via `playstore/screenshots/frame-shot.py`. Store assets live in [playstore/](playstore/): app icon, feature graphic, listing copy, and the demo-video storyboard.

## Setup

Requirements: JDK 21, Android SDK (compileSdk 36.1), AGP 9.1.1.

```sh
# Unit tests
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=$HOME/Library/Android/sdk \
  sh gradlew :app:testDebugUnitTest

# Lint
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=$HOME/Library/Android/sdk \
  sh gradlew :app:lintDebug

# Release bundle (founder signs with the production keystore locally;
# never commit keystores or keystore.properties)
KEYSTORE_PATH=/path/to/upload.jks STORE_PASSWORD=… KEY_PASSWORD=… \
  sh gradlew :app:bundleRelease
```

First run is connect-first: enter the PMS address, property API key, and staff name (or scan the QR pairing code). See [VERIFICATION.md](VERIFICATION.md) for the full verification matrix and the live acceptance script.
