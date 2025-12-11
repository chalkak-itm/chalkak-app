# Chalkak 기술 스택 문서

> **프로젝트명**: Chalkak - 비전 객체 인식을 활용한 영어 단어 학습 애플리케이션
> 
> **플랫폼**: Android Mobile Application

---

## 📱 1. 핵심 플랫폼 및 언어

### Android
- **Target SDK**: 36
- **Min SDK**: 33
- **Compile SDK**: 36
- **Language**: Kotlin 2.0.21
- **Build System**: Gradle 8.13.1 (Kotlin DSL)

### 개발 환경
- **Android Gradle Plugin (AGP)**: 8.13.1
- **Kotlin**: 2.0.21
- **Java Version**: 11 (sourceCompatibility & targetCompatibility)

---

## 🎯 2. 주요 기술 스택

### 2.1 아키텍처 & UI
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
  - `ui/`: Activity, Fragment (View)
  - `domain/`: Business Logic
  - `data/`: Repository Pattern (Local & Remote)

- **UI Components**:
  - ViewBinding (활성화)
  - Material Design Components 1.13.0
  - AndroidX ConstraintLayout 2.2.1
  - AndroidX AppCompat 1.7.1
  - AndroidX Core KTX 1.17.0
  - AndroidX Activity 1.11.0

### 2.2 데이터베이스 (로컬 저장소)
- **Room Database**: 2.6.1
  - `room-runtime`: 로컬 데이터 저장
  - `room-ktx`: Kotlin Extensions & Coroutines 지원
  - `room-compiler`: Annotation Processing (KAPT)
  
- **주요 엔티티**:
  - `PhotoLog`: 사진 촬영 기록
  - `DetectedObject`: 감지된 객체 정보
  - `ExampleSentence`: 예문 데이터

- **DAO**:
  - `PhotoLogDao`
  - `DetectedObjectDao`
  - `ExampleSentenceDao`

### 2.3 머신러닝 & AI

#### TensorFlow Lite
- **TensorFlow Lite Task Vision**: 0.4.2
- **TensorFlow Lite Support**: 0.4.2
- **모델**: `1.tflite` (assets 폴더에 위치)
- **기능**: 실시간 객체 감지 (Object Detection)
  - 최대 감지 객체 수: 2개
  - 최소 신뢰도 임계값: 0.3 (30%)
  - 스레드 수: 4

#### OpenAI GPT API (Cloud Function 통해 호출)
- **모델**: GPT-3.5-turbo
- **용도**: 
  - 영어 단어의 한국어 의미 생성
  - 예문 3개 자동 생성 (영어 + 한국어 번역)
  - Temperature: 0.7
  - Max Tokens: 500

---

## 🔥 3. Firebase 서비스 (Backend as a Service)

### 3.1 Firebase Core
- **Firebase BOM**: 34.5.0
- **Firebase Analytics**: 사용자 행동 분석

### 3.2 Firebase Authentication
- **Firebase Auth**: 사용자 인증 관리
- **Google Sign-In**: 
  - **Credential Manager** (최신):
    - `androidx.credentials:credentials`: 1.3.0
    - `androidx.credentials:credentials-play-services-auth`: 1.3.0
    - `com.google.android.libraries.identity.googleid`: 1.1.1
  - GoogleAuthProvider를 통한 소셜 로그인

### 3.3 Firebase Firestore
- **Cloud Firestore**: NoSQL 클라우드 데이터베이스
- **Collections**:
  - `users`: 사용자 정보 (email, nickname, fcmToken, stats, lastStudiedAt)
  - `users/{uid}/studyLog`: 학습 기록 (lastStudied timestamp)
  - `words`: 단어 데이터 (originalWord, meaning, examples)

### 3.4 Firebase Cloud Functions
- **런타임**: Node.js 22
- **리전**: asia-northeast3 (Seoul)
- **Dependencies**:
  - `firebase-admin`: ^12.6.0
  - `firebase-functions`: ^6.0.1 (v2)
  - `openai`: ^6.9.1

- **Functions**:
  - `getWordData`: GPT API를 호출하여 단어 데이터 생성

### 3.5 Firebase Cloud Messaging (FCM)
- **Firebase Messaging**: 푸시 알림 기능
- **목적**: 학습 리마인더 알림

---

## 🛠️ 4. 핵심 기능별 기술

### 4.1 객체 감지 (Object Detection)
- **TensorFlow Lite**
- **Helper Class**: `ObjectDetectionHelper.kt`
- **처리 과정**:
  1. 이미지를 ARGB_8888 포맷으로 변환
  2. TensorImage로 변환
  3. ObjectDetector로 감지 수행
  4. Bounding Box 그리기 (빨간색, 6px stroke)

### 4.2 음성 인식 (Speech Recognition)
- **Android SpeechRecognizer API**
- **Helper Class**: 
  - `SpeechRecognitionHelper.kt`
  - `SpeechRecognitionManager.kt`
- **Permission**: `RECORD_AUDIO`
- **지원 언어**: 영어 (en-US)
- **기능**: 
  - 실시간 음성 인식
  - 부분 결과(Partial Results) 지원
  - 신뢰도 점수(Confidence Scores) 제공

### 4.3 음성 합성 (Text-to-Speech)
- **Android TextToSpeech API**
- **Helper Class**: `TtsHelper.kt`
- **지원 언어**: 영어 (en-US)
- **용도**: 단어 및 예문 발음

### 4.4 알림 시스템
- **AlarmManager**: 정확한 시간 알림 (Exact Alarm)
- **BroadcastReceiver**: `NotificationReceiver.kt`
- **Helper Classes**:
  - `NotificationHelper.kt`
  - `NotificationScheduler.kt`
- **Permissions**:
  - `POST_NOTIFICATIONS`
  - `SCHEDULE_EXACT_ALARM`
  - `USE_EXACT_ALARM`

### 4.5 캘린더 UI
- **Material CalendarView**: 1.9.0 (com.applandeo)
- **기능**: 
  - 학습 날짜 하이라이트
  - 클릭 이벤트 처리
  - 날짜 범위 제한 (1년)

### 4.6 이미지 처리
- **FileProvider**: 카메라 촬영 이미지 공유
- **Helper Classes**:
  - `ImageLoaderHelper.kt`: 이미지 로딩
  - `ImagePickerHelper.kt`: 갤러리/카메라 선택
- **Permission**: 파일 접근 권한

### 4.7 센서 기능
- **QuickSnap Sensor**: `QuickSnapSensorHelper.kt`
- 빠른 촬영 기능 지원

---

## 🔄 5. 비동기 처리

### Kotlin Coroutines
- **kotlinx-coroutines-play-services**: 1.7.3
- **용도**: 
  - Firebase 비동기 작업 처리
  - Room Database 쿼리
  - Network 요청
  - 백그라운드 작업

---

## 🌐 6. 네트워크 & 권한

### 네트워크
- **Permissions**:
  - `INTERNET`: 인터넷 연결
  - `ACCESS_NETWORK_STATE`: 네트워크 상태 확인

### 기타 권한
- `RECORD_AUDIO`: 음성 인식
- `POST_NOTIFICATIONS`: 알림 표시
- `SCHEDULE_EXACT_ALARM`: 정확한 알림 스케줄링
- `USE_EXACT_ALARM`: 정확한 알람 사용

---

## 📦 7. Gradle Plugins

1. **Android Application Plugin** (AGP)
2. **Kotlin Android Plugin**
3. **Google Services Plugin**: 4.4.4 (Firebase 연동)
4. **Kotlin Parcelize**: Parcelable 자동 구현
5. **Kotlin KAPT**: Annotation Processing (Room)

---

## 🗂️ 8. 프로젝트 구조

```
app/src/main/java/com/example/chalkak/
├── base/                    # 기본 클래스
│   └── BaseFragment.kt
├── data/                    # 데이터 레이어
│   ├── local/              # Room DB
│   │   ├── AppDatabase.kt
│   │   ├── PhotoLog.kt
│   │   ├── PhotoLogDao.kt
│   │   ├── DetectedObject.kt
│   │   ├── DetectedObjectDao.kt
│   │   ├── ExampleSentence.kt
│   │   └── ExampleSentenceDao.kt
│   └── remote/             # Firebase
│       └── firestore/
│           ├── FirestoreRepository.kt
│           └── WordDTO.kt
├── domain/                 # 비즈니스 로직
│   ├── detection/         # 객체 감지
│   │   └── ml/
│   │       └── ObjectDetectionHelper.kt
│   ├── speech/            # 음성 처리
│   │   ├── SpeechRecognitionHelper.kt
│   │   ├── SpeechRecognitionManager.kt
│   │   └── TtsHelper.kt
│   ├── notification/      # 알림
│   │   ├── NotificationHelper.kt
│   │   ├── NotificationScheduler.kt
│   │   └── NotificationReceiver.kt
│   ├── quiz/              # 퀴즈 로직
│   └── preferences/       # 사용자 설정
│       └── UserPreferencesHelper.kt
├── ui/                     # UI 레이어
│   ├── activity/          # Activity
│   │   ├── LoginActivity.kt
│   │   ├── MainActivity.kt
│   │   ├── DetectionResultActivity.kt
│   │   ├── ImagePreviewActivity.kt
│   │   └── ObjectInputActivity.kt
│   └── fragment/          # Fragment
│       ├── HomeFragment.kt
│       ├── LogFragment.kt
│       ├── QuizFragment.kt
│       ├── SettingFragment.kt
│       └── ObjectInputFragment.kt
└── util/                   # 유틸리티
    ├── ImageLoaderHelper.kt
    ├── ImagePickerHelper.kt
    ├── QuickSnapSensorHelper.kt
    ├── ToastHelper.kt
    └── WordDataLoaderHelper.kt
```

---

## 📚 9. 외부 API 및 서비스

### 9.1 OpenAI API
- **Endpoint**: Firebase Cloud Functions를 통해 프록시
- **API Key**: Firebase Functions 환경 변수에 저장
- **사용 목적**: 
  - 영어 단어의 한국어 뜻 자동 생성
  - 예문 자동 생성 (3개)

### 9.2 Google Sign-In API
- **Credential Manager API** (최신 방식)
- **목적**: 간편한 소셜 로그인

### 9.3 Firebase Services API
- Authentication API
- Firestore API
- Cloud Functions API
- Cloud Messaging API

---

## 🧪 10. 테스트

### Unit Test
- **JUnit**: 4.13.2

### Instrumentation Test
- **AndroidX JUnit**: 1.3.0
- **Espresso Core**: 3.7.0
- **Test Runner**: AndroidJUnitRunner

---

## 🔐 11. 보안 & 데이터 관리

### 데이터 백업
- `android:allowBackup="true"`
- Data Extraction Rules 정의
- Full Backup Content 정의

### 인증
- Firebase Authentication 통해 사용자 관리
- UID 기반 데이터 격리
- FCM Token 관리

### 데이터베이스
- Room: 로컬 SQLite 데이터베이스 (암호화 미적용)
- Firestore: 클라우드 NoSQL (Firebase 보안 규칙 적용 가능)

---

## 📊 12. 데이터 흐름

### 단어 학습 플로우
1. **카메라 촬영** → TensorFlow Lite로 객체 감지
2. **감지된 단어** → Room DB에 저장
3. **단어 데이터 없을 시** → Firebase Functions 호출 → GPT API로 의미/예문 생성
4. **생성된 데이터** → Firestore & Room DB에 저장
5. **앱 시작 시** → Firestore에서 모든 단어 동기화 → Room DB에 merge

### 인증 플로우
1. **Google Sign-In** (Credential Manager)
2. **Firebase Authentication**으로 토큰 교환
3. **FCM Token** 생성 및 Firestore 저장
4. **SharedPreferences**에 사용자 정보 캐싱

---

## 💡 13. 주요 설계 특징

### 하이브리드 데이터 저장
- **Room DB**: 오프라인 접근, 빠른 쿼리
- **Firestore**: 클라우드 백업, 다중 기기 동기화
- **전략**: "firebase_sync" 더미 PhotoLog를 통해 동기화된 단어 추적

### Repository Pattern
- `FirestoreRepository`: Firebase 작업 캡슐화
- `AppDatabase`: Room DB 싱글톤
- `WordDataLoaderHelper`: Room → Firestore 폴백 로직

### Helper Pattern
- 반복되는 로직을 Helper 클래스로 분리
- 재사용성 및 테스트 용이성 증가

---

## 🚀 14. 배포 & 빌드

### Build Types
- **Debug**: Minify 비활성화
- **Release**: ProGuard 설정 가능 (현재 비활성화)

### Version
- **versionCode**: 1
- **versionName**: "1.0"

### Application ID
- `com.example.chalkak`

---

## 📝 요약

**Chalkak**은 TensorFlow Lite를 활용한 객체 감지, OpenAI GPT를 통한 자동 학습 콘텐츠 생성, Firebase를 통한 클라우드 동기화를 결합한 현대적인 Android 학습 애플리케이션입니다.

### 핵심 기술:
- ✅ **AI/ML**: TensorFlow Lite (객체 감지), GPT-3.5 (콘텐츠 생성)
- ✅ **Backend**: Firebase (Auth, Firestore, Functions, FCM)
- ✅ **Database**: Room (로컬) + Firestore (클라우드)
- ✅ **Language**: Kotlin + Coroutines
- ✅ **Speech**: Android SpeechRecognizer + TTS
- ✅ **UI**: Material Design + ViewBinding + Custom Calendar
