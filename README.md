# SmartPay AI – Your Personal Finance Assistant

SmartPay AI is an Android-based personal finance management application designed to help users track their expenses, manage monthly budgets, and stay aware of upcoming financial commitments.

The main goal of SmartPay AI is to make personal finance management easier through transaction tracking, budget monitoring, and timely reminders.

## Problem Statement

Many people make daily UPI payments but find it difficult to understand their spending patterns, manage monthly budgets, and plan purchases around upcoming expenses.

SmartPay AI aims to solve this problem by providing a simple Android application to track transactions, manage budgets, and monitor upcoming financial commitments.

## Current Features

- **Transaction Tracking:** Helps users monitor payment transactions collected through supported payment notifications.
- **Monthly Budget Management:** Create and manage monthly budgets.
- **Budget Monitoring:** Track budget usage and payment status.
- **Budget Reminders:** Receive reminders for upcoming and due budget payments.
- **Payment Status Management:** Mark budget payments as Paid or Not Paid.
- **Notification History:** View previous budget notifications and payment actions.
- **Local Data Storage:** Stores financial records locally using Room Database (SQLite).
- **Privacy-Focused Design:** Core financial data is stored locally on the device.

## Future Enhancement – AI Finance Coach

The planned AI Finance Coach will help users make informed spending decisions using their financial information.

For example, a user could ask:

"Can I buy shoes worth ₹8,000 this month?"

The planned assistant will analyse:

- Available account balance (when provided or supported by an authorised data source)
- Remaining monthly budget
- Upcoming bills and financial commitments
- Estimated balance after the proposed purchase

It will then provide a Yes, No, or Wait recommendation with a simple explanation.

**Note:** The AI Finance Coach chatbot and purchase recommendation system are planned future enhancements and are not represented as currently implemented features.

## Technology Stack

- Java
- Android Studio
- XML
- Room Database
- SQLite
- Android Notifications
- ONNX Runtime for Android (dependency included)

## AI Models and APIs

The current application focuses on local financial data management. No trained AI model or external AI API is currently claimed as an implemented feature.

ONNX Runtime is included as a project dependency; its presence alone does not mean an AI model is integrated.

The AI Finance Coach is planned as a future enhancement.

## Data Privacy

SmartPay AI is designed around local data storage.

- Financial records are stored on the device using Room Database (SQLite).
- The core budget and transaction management features do not require a cloud database.
- Users should never enter real bank credentials, UPI PINs, passwords, or other sensitive financial information into the application.

## Download and Install APK

You can download the SmartPay AI Android application directly from the GitHub Releases page.

### Download APK

https://github.com/V-Sudhakar-87/SmartPay-AI/releases/tag/v1.0.0

### Installation Steps

1. Open the download link on your Android device.
2. Download the latest SmartPay AI APK.
3. Open the downloaded APK file.
4. If prompted, allow installation from this source.
5. Tap Install.
6. Open SmartPay AI and start exploring the application.

**Minimum Android Version:** Android 8.0 (Oreo) or above.

**Build Type:** Debug APK – Demonstration and testing purposes.

## How to Run Using Android Studio

You can also run the application directly from Android Studio.

### Prerequisites

- Android Studio
- Android SDK
- Android device or Android Emulator
- Java 11 compatible build environment

### Steps

1. Clone or download this repository.
2. Open Android Studio.
3. Select **Open** and choose the SmartPay AI project folder.
4. Wait for Gradle sync to complete.
5. Connect an Android device using USB debugging or start an Android Emulator.
6. Select the target device.
7. Click the **Run** button in Android Studio.
8. The application will be installed and launched on the selected device or emulator.

### Android Compatibility

- Minimum supported Android version: Android 8.0 (Oreo).
- Target SDK: Android 16 (API 36).
- The application has been tested using Android Studio Emulator environments.
- Device-specific behaviour may vary depending on Android version and manufacturer.

## Demo Video

**YouTube Demo:** https://youtu.be/0m0_9fhNgu8

The demo video demonstrates the application's working features and user interface.

## Project Information

**Project Name:** SmartPay AI

**Tagline:** Your Money, Your Control.

**Platform:** Android

**Project Category:** AI for Finance – Personal Finance Management

