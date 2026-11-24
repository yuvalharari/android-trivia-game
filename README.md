# Android Trivia Game

An Android trivia application developed in **Java** using **Android Studio** and **Firebase**. Users can register, sign in, create and manage quizzes, play trivia games, and review their results and statistics.

## Features

- User registration and sign-in with Firebase Authentication
- Quiz creation, editing, and management
- Trivia gameplay with configurable game settings
- Results tracking, game history, and performance statistics
- Quiz data and user information stored in Firebase Realtime Database

## Tech Stack

- **Language:** Java
- **Platform:** Android / Android Studio
- **UI:** Android XML layouts
- **Backend services:** Firebase Authentication and Firebase Realtime Database
- **Build system:** Gradle

## Getting Started

1. Clone this repository and open the project root in Android Studio.
2. Configure a Firebase project for the Android package `com.example.trivia`, or use your own configuration after updating the app's Firebase database references.
3. Download your Firebase `google-services.json` file and place it at `app/google-services.json` (not in version control).
4. Enable the required Firebase Authentication method and Firebase Realtime Database in your Firebase console, with appropriate security rules.
5. Sync Gradle and run the app on an emulator or Android device.

> **Security:** `google-services.json`, local SDK settings, generated build artifacts, and signing credentials are intentionally excluded. Never commit secrets. Firebase client API keys are not sufficient security controls: restrict applicable keys and configure Firebase database access rules appropriately. Some source files still contain a reference to the original Firebase Realtime Database URL; switch these to your own project's URL before deploying or using another Firebase project.

## Project Structure

- `app/src/main/java/com/example/trivia/` — Android activities and application logic
- `app/src/main/res/layout/` — XML screen layouts
- `app/src/main/res/drawable/` — Visual assets
- `app/src/main/AndroidManifest.xml` — Android app configuration

## Project Context

Academic Android application project demonstrating mobile development, UI implementation, authentication, cloud-backed data persistence, and application flow design.
