<img width="163" height="230" alt="캐릭터" src="https://github.com/user-attachments/assets/ae5a0d99-86fd-4aaf-880a-b2dd64a7b3d2" />


# Chalkak

> Camera-based English learning app for children, powered by TensorFlow Lite object recognition

`Chalkak` is an Android English learning application designed for young children.  
The app uses the device camera to recognize real-world objects, converts detected objects into English vocabulary, and helps children learn words and example sentences through review, quiz, speech, and repetition features.

Instead of learning English from a fixed word list, children can learn from the objects around them.  
They take a photo, discover the English word for what they see, listen to pronunciation, review example sentences, and continue learning through personalized practice.

<br />

## 🖼️ Preview

> Add the app icon, main screen, and learning screen screenshots here.

| App Icon | Main Page | Learning Page |
| --- | --- | --- |
|<img width="163" height="230" alt="캐릭터" src="https://github.com/user-attachments/assets/ae5a0d99-86fd-4aaf-880a-b2dd64a7b3d2" /> | <img width="161" height="337" alt="찰칵 캡쳐" src="https://github.com/user-attachments/assets/c8cdab56-d27e-4f32-811e-4645377b5da8" /> | <img width="161" height="337" alt="찰칵 캡쳐 2" src="https://github.com/user-attachments/assets/65966f80-5782-4611-a2ca-a502af3a8d52" />  |



<br />

## 📌 Project Overview

`Chalkak` was built as an Android Studio project using Kotlin.  
It combines on-device object detection, local learning history, Firebase synchronization, and AI-generated word data to create a child-friendly English learning experience.

The core idea is simple:

```text
Take a photo
  ↓
Detect objects with TensorFlow Lite
  ↓
Convert detected objects into English vocabulary
  ↓
Generate Korean meanings and example sentences
  ↓
Save words locally and remotely
  ↓
Review, listen, speak, and solve quizzes
```

This project is one of the most complete projects in the repository because it includes not only UI screens, but also machine learning inference, local database design, Firebase integration, cloud functions, authentication, speech features, and study progression logic.

<br />

## 🎯 Target Users

`Chalkak` is designed for children who are beginning to learn English.

The app focuses on:

- Learning from familiar real-world objects
- Reducing the difficulty of memorizing isolated vocabulary
- Connecting visual recognition with English words
- Supporting repeated review through quizzes and study logs
- Making English learning feel interactive and playful

<br />

## ✨ Key Features

### 📷 Camera-based object recognition

- Capture or upload an image
- Run object detection with a TensorFlow Lite model
- Display detected objects with bounding boxes
- Convert detected objects into English learning items

### 🧠 TensorFlow Lite vision model

- Uses a TensorFlow Lite model stored in the Android assets directory
- Model file: `app/src/main/assets/1.tflite`
- Uses TensorFlow Lite Task Vision and TensorFlow Lite Support libraries
- Runs object detection directly on the Android device

### 🔤 Word learning

- Saves detected English words
- Provides Korean meanings
- Generates example sentences for each word
- Stores study state and last studied time
- Supports word review based on learning history

### 🗣️ Speech learning

- Text-to-Speech support for pronunciation practice
- Speech recognition support for speaking practice
- Helps children connect reading, listening, and speaking

### 🧩 Quiz and review

- Quiz screen for reviewing learned words
- Question generation logic for practice
- Spaced repetition manager for study progression
- Correct/incorrect feedback UI

### 🔐 Firebase authentication

- Firebase Authentication integration
- Google Sign-In support through Credential Manager
- Login entry point through `LoginActivity`

### ☁️ Firebase cloud integration

- Firebase Firestore for remote word data synchronization
- Firebase Functions for AI-powered word data generation
- Firebase Messaging dependency included for notification-related features
- Hybrid local/cloud data architecture

### 💾 Local database

- Room Database for local storage
- Stores captured photo logs, detected objects, and example sentences
- Allows the app to keep learning data on the device
- Supports synchronization with Firebase Firestore

<br />

## 🛠️ Tech Stack

| Area | Technology |
| --- | --- |
| Platform | Android |
| Language | Kotlin |
| IDE | Android Studio |
| UI | XML Layouts, ViewBinding, Material Components |
| ML / Vision | TensorFlow Lite Task Vision, TensorFlow Lite Support |
| Local Database | Room Database |
| Cloud Database | Firebase Firestore |
| Authentication | Firebase Authentication, Google Credential Manager |
| Cloud Functions | Firebase Functions |
| AI Integration | OpenAI API through Firebase Functions |
| Async | Kotlin Coroutines, Coroutine Play Services |
| Speech | Android Speech Recognition, Text-to-Speech |
| Notifications | Firebase Messaging, Android notification permissions |
| Build System | Gradle Kotlin DSL |

<br />

## 🏗️ Architecture

`Chalkak` uses a layered Android structure.

```text
app/src/main/java/com/example/chalkak/
├── base/                 # Shared base classes
├── data/                 # Local and remote data sources
│   ├── local/            # Room database, DAO, models
│   └── remote/firestore/ # Firestore models and repository
├── domain/               # Core app logic
│   ├── detection/        # Object detection and bounding box logic
│   ├── notification/     # Notification-related logic
│   ├── preferences/      # User/app preference logic
│   ├── quiz/             # Quiz generation and spaced repetition
│   └── speech/           # STT and TTS helpers
├── ui/                   # Activities, fragments, dialogs, navigation
└── util/                 # Image, toast, sensor, and word data helpers
```

<br />

## 📱 Main Android Screens

The app is organized around activities and fragments.

### Activities

```text
LoginActivity
MainActivity
ImagePreviewActivity
DetectionResultActivity
ObjectInputActivity
```

### Fragments

```text
HomeFragment
DetectionResultFragment
LogFragment
MagicAdventureFragment
ObjectInputFragment
QuizFragment
QuizQuestionFragment
SettingFragment
```

### Main layout resources

```text
activity_main.xml
activity_login.xml
activity_camera_capture.xml
activity_detection_result.xml
fragment_home.xml
fragment_quiz.xml
fragment_quiz_question.xml
fragment_log.xml
fragment_setting.xml
```

<br />

## 🤖 Object Detection Flow

```text
Camera / Image Upload
  ↓
Image Preview
  ↓
TensorFlow Lite Object Detection
  ↓
Bounding Box Conversion
  ↓
Detected Object List
  ↓
Word Data Request
  ↓
Save to Room and Firestore
```

Related files and modules:

```text
app/src/main/assets/1.tflite
domain/detection/ml/
domain/detection/BoundingBoxHelper.kt
domain/detection/DetectionResultConverter.kt
domain/detection/DetectionResultItem.kt
```

<br />

## 🔥 Firebase Integration

This project uses Firebase both on the Android side and through Firebase Cloud Functions.

### Firebase features used

- Firebase Authentication
- Firebase Firestore
- Firebase Functions
- Firebase Analytics
- Firebase Messaging

### Firebase Functions

The `functions/` directory contains a Node.js Firebase Functions project.

```text
functions/
├── index.js
├── package.json
├── package-lock.json
└── .eslintrc.js
```

The main callable function is responsible for generating word learning data.

```text
getWordData(word)
  ↓
OpenAI API request
  ↓
Korean meaning
  ↓
Example sentences
  ↓
Return structured learning data to Android app
```

The function is deployed to the `asia-northeast3` region and uses the OpenAI API key from environment variables.

<br />

## 🧠 AI Word Data Generation

When the app detects or receives an English word, it can request additional learning content through Firebase Functions.

The cloud function asks OpenAI to return:

- Original English word
- Korean meaning
- Example sentences
- Korean translations for example sentences

This allows the app to turn a detected object into a richer learning item instead of only showing a single word.

<br />

## 💾 Database Design

`Chalkak` uses a hybrid data architecture.

```text
Local DB: Room Database
Cloud DB: Firebase Firestore
External AI: Firebase Functions + OpenAI API
```

### Room Database tables

| Table | Purpose |
| --- | --- |
| `photo_logs` | Stores captured photo records |
| `detected_objects` | Stores detected English words and metadata |
| `example_sentences` | Stores generated example sentences for each word |

### Local database files

```text
data/local/AppDatabase.kt
data/local/RoomDao.kt
data/local/RoomModels.kt
```

### Firestore files

```text
data/remote/firestore/FirestoreModels.kt
data/remote/firestore/FirestoreRepository.kt
```

For more details, see:

```text
database_architecture.md
```

<br />

## 🗣️ Speech Features

The project includes both speech recognition and text-to-speech helpers.

```text
domain/speech/SpeechRecognitionHelper.kt
domain/speech/SpeechRecognitionManager.kt
domain/speech/TtsHelper.kt
```

These features support pronunciation and speaking practice so children can learn vocabulary through listening and speaking, not only reading.

<br />

## 🧩 Quiz and Review

The quiz system helps children review words after detection and study.

```text
domain/quiz/QuizQuestion.kt
domain/quiz/QuizQuestionGenerator.kt
domain/quiz/SpacedRepetitionManager.kt
```

The app can generate quiz questions and manage review progression based on study history.

<br />

## 📂 Project Structure

```text
chalkak-app/
├── app/
│   ├── src/main/assets/
│   │   └── 1.tflite
│   ├── src/main/java/com/example/chalkak/
│   │   ├── base/
│   │   ├── data/
│   │   ├── domain/
│   │   ├── ui/
│   │   └── util/
│   ├── src/main/res/
│   │   ├── drawable/
│   │   ├── layout/
│   │   ├── menu/
│   │   ├── mipmap-*/
│   │   ├── values/
│   │   └── xml/
│   ├── build.gradle.kts
│   └── google-services.json
├── functions/
│   ├── index.js
│   └── package.json
├── database_architecture.md
├── firebase.json
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

<br />

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/chalkak-itm/chalkak-app.git
cd chalkak-app
```

### 2. Open in Android Studio

Open the project root directory in Android Studio.

```text
File > Open > chalkak-app
```

### 3. Configure Firebase

The project uses Firebase services.  
To run the app with your own Firebase project, configure:

```text
app/google-services.json
.firebaserc
firebase.json
```

You also need to enable the required Firebase services:

- Authentication
- Firestore
- Functions
- Analytics
- Messaging, if notification features are used

### 4. Configure OpenAI API key for Firebase Functions

The OpenAI API key should be configured as an environment variable for Firebase Functions.

```text
OPENAI_API_KEY=your_openai_api_key
```

### 5. Build and run

Run the Android app from Android Studio on an emulator or physical Android device.

Minimum SDK:

```text
minSdk 33
```

Target SDK:

```text
targetSdk 36
```

<br />

## ☁️ Firebase Functions Commands

Move into the functions directory.

```bash
cd functions
```

Install dependencies.

```bash
npm install
```

Run Firebase emulators.

```bash
npm run serve
```

Deploy functions.

```bash
npm run deploy
```

View logs.

```bash
npm run logs
```

<br />

## 🔐 Required Permissions

The app uses Android permissions for network, speech, notification, and file-sharing features.

```xml
INTERNET
ACCESS_NETWORK_STATE
RECORD_AUDIO
POST_NOTIFICATIONS
SCHEDULE_EXACT_ALARM
USE_EXACT_ALARM
```

The app also uses `FileProvider` for safe image file access.

<br />

## 👥 Contributors

- Jeong-yun Lee
- Seungwoo Baik
- Sim Suyong

<br />

## 🧠 What We Learned

Through this project, we practiced:

- Building a Kotlin Android app with Android Studio
- Running TensorFlow Lite object detection on mobile
- Designing a child-friendly English learning flow
- Managing local learning data with Room Database
- Synchronizing data with Firebase Firestore
- Using Firebase Functions as a secure backend layer
- Connecting OpenAI-generated word data to an Android app
- Implementing STT, TTS, quizzes, and spaced repetition
- Structuring an Android project into data, domain, and UI layers

<br />

## 🔧 Future Improvements

- Improve object detection accuracy with a stronger or custom-trained model
- Add more child-friendly animations and sound feedback
- Improve the learning page with progress indicators
- Add parent/teacher dashboard features
- Add more quiz types and review modes
- Improve offline-first synchronization
- Add unit and UI tests
- Prepare a release build and distribution guide

<br />

## ✅ Summary

> Chalkak is a camera-based English learning Android app for children that combines TensorFlow Lite object recognition, Firebase, Room Database, speech features, quizzes, and AI-generated example sentences to turn real-world objects into interactive English learning materials.
