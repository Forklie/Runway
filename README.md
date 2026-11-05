# Runway

Android personal finance app for SE3092 Platform Based Development, Assignment 01.

## Overview

Runway is built for Kavindu Silva's scenario: irregular income from four sources, one in USD, and a LKR 490,000 MacBook goal.

- **Quick Add:** log an expense in 3 taps, with Undo.
- **Income:** logged per source. USD entries keep their own exchange rate.
- **Safe to Spend:** a daily figure based on base salary only.
- **Goal:** required monthly saving, actual pace, projected date and a what-if for purchases.
- **Weekly check-in:** enter real balances so totals stay true after missed days.
- **Also:** committed versus discretionary split, freelance milestones with overdue alerts, subscription audit, crypto ledger.

![Use-case diagram: the user's 14 use cases and the Firebase Authentication, Google Credential Manager and Cloud Firestore actors](docs/diagrams/use-case.png)

## Firebase setup

The app uses the live project `runway-a1a74`. `app/google-services.json` is committed, so a clone runs with no setup.

To use your own project:

1. Create a Firebase project and add an Android app with package `com.example.runway`.
2. Add your debug SHA-1 (`./gradlew signingReport`). Google Sign-In needs it.
3. Replace `app/google-services.json` with yours.
4. Enable Email/Password and Google under Authentication.
5. Create a Firestore database.
6. Deploy the rules and indexes:

   ```bash
   firebase deploy --only firestore:rules,firestore:indexes
   ```

## Build the APK

**Android Studio (any system):** open the project folder, let Gradle sync, and run the `app` configuration. Android Studio brings its own JDK and installs any missing SDK components.

**Terminal:** needs JDK 17 or newer, and the Android SDK location in the `ANDROID_HOME` environment variable.

macOS and Linux:

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

`assembleDebug` builds the debug APK at `app/build/outputs/apk/debug/app-debug.apk`. It connects to the live Firebase project. Min SDK 26, target SDK 36.

With the same wrapper, `installDebug` builds and installs it on a connected device or emulator, and `testDebugUnitTest` runs the unit tests.

## Install on a phone

Build the release APK on any computer with the same wrapper and `assembleRelease`:

```bash
./gradlew assembleRelease
```

Copy `app/build/outputs/apk/release/app-release.apk` to the phone, open it, and allow installs from that app if asked. Use this file, not an older `app-release-unsigned.apk`, because Android rejects unsigned APKs.

Release builds are signed with the computer's debug key, so they are for sideloading and not for Google Play.

### "App not installed"

Android blocks an update signed with a different key from the installed copy, and each computer has its own debug key. So this error usually means Runway is already on the phone from another computer.

Uninstall Runway, including from any other profile or Secure Folder, then install the new APK. Uninstalling removes anything not yet synced to Firestore.

To stop this happening, use one key everywhere. Copy `debug.keystore` from one computer to the others, replacing the file there:

- **macOS and Linux:** `~/.android/debug.keystore`
- **Windows:** `%USERPROFILE%\.android\debug.keystore`

### Google Sign-In on a new computer

Google Sign-In only works for keys registered in Firebase. Run the wrapper with `signingReport`, and if the SHA-1 for the `debug` variant is new, add it to the Android app in the Firebase console and replace `app/google-services.json` with the new download. A copied key that is already registered needs neither step.

## Architecture

MVVM, single activity, Kotlin and Jetpack Compose only.

![MVVM architecture: Compose views, Hilt ViewModels, pure Kotlin use cases, repositories, and the Firestore cache and Firebase data sources](docs/diagrams/mvvm-architecture.png)

- **Screens** (`ui/feature`) render state and send events. They never touch Firebase.
- **ViewModels** come from Hilt and expose read-only `StateFlow`.
- **Use cases** (`domain/usecase`) hold the formulas in pure Kotlin.
- **Repositories** are interfaces in `domain`, implemented in `data` with Firestore snapshot listeners.
- **Offline** uses the Firestore persistent cache, so there is no Room database.
- **Data** lives under `users/{uid}`, and `firestore.rules` limits access to the owner.

### Firestore schema

![Firestore schema: users/{uid} with subcollections for accounts, transactions, categories, income sources, freelance projects, milestones, goals, balance check-ins, recurring items and crypto entries](docs/diagrams/firestore-schema.png)
