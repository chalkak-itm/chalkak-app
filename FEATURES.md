# Chalkak 앱 기능 명세서

> **앱 이름**: Chalkak (찰칵)
> 
> **컨셉**: 비전 객체 인식을 활용한 실생활 영어 단어 학습 애플리케이션

---

## 📸 1. 핵심 학습 기능

### 1.1 Magic Adventure (사진 촬영 & 학습)
**설명**: 실생활 물체를 촬영하여 AI로 자동 감지하고, 영어 단어를 학습하는 핵심 기능

#### 주요 기능:
- **카메라 촬영**: 
  - 실시간 카메라로 물체 촬영
  - QuickSnap 센서 지원 (설정에서 활성화 가능)
  - 촬영 후 즉시 객체 감지 실행

- **갤러리 업로드**: 
  - 기존 사진에서 이미지 선택
  - 선택한 이미지에서 객체 감지

- **자동 객체 감지**:
  - TensorFlow Lite 모델로 실시간 객체 감지
  - 최대 2개 객체 동시 감지
  - 신뢰도 30% 이상 객체만 표시
  - 감지된 객체에 빨간색 Bounding Box 표시

#### 사용자 플로우:
```
1. "Magic Adventure" 화면 진입
2. "Take Photo" 또는 "Upload" 선택
3. 이미지 선택/촬영
4. AI 객체 감지 실행
5. 감지 결과 화면으로 이동
```

---

### 1.2 Detection Result (감지 결과 & 단어 학습)
**설명**: 감지된 객체의 영어 단어와 의미, 예문을 학습하는 화면

#### 주요 기능:
- **다중 객체 표시**:
  - 감지된 모든 객체를 버튼으로 나열
  - 클릭 시 해당 단어 상세 정보 표시
  - 이미지 위 Bounding Box와 연동

- **단어 상세 정보**:
  - 영어 단어 (소문자 정규화)
  - 한국어 의미
  - 예문 3개 (영어 + 한국어 번역)

- **음성 기능**:
  - TTS (Text-to-Speech): 단어 및 예문 발음 듣기
  - STT (Speech-to-Text): 사용자 발음 녹음 및 인식
  - 발음 정확도 피드백

- **데이터 자동 생성**:
  - 새로운 단어 감지 시 → Firebase Functions 호출 → GPT API로 의미/예문 생성
  - 생성된 데이터 → Firestore 및 Room DB에 저장
  - 기존 학습한 단어는 로컬 DB에서 즉시 로드

#### 데이터 흐름:
```
감지된 단어 확인
  ↓
Room DB에 존재?
  ↓ YES: Room DB에서 로드
  ↓ NO: Firebase Functions → GPT API 호출
    ↓
    의미 + 예문 3개 생성
    ↓
    Firestore & Room DB 저장
    ↓
    화면에 표시
```

---

### 1.3 Manual Object Input (수동 단어 입력)
**설명**: AI가 감지하지 못한 경우, 사용자가 직접 단어를 입력하는 기능

#### 주요 기능:
- **텍스트 입력**: 영어 단어 직접 입력
- **유효성 검사**: 
  - 영어 알파벳 + 공백만 허용
  - 빈 값 입력 방지
- **자동 정규화**: 입력한 단어를 소문자로 변환
- **신뢰도 100%**: 사용자 입력은 100% 신뢰도로 처리
- **Detection Result로 연결**: 입력 후 자동으로 단어 학습 화면으로 이동

#### 사용자 플로우:
```
1. 객체 감지 실패 시 "수동 입력" 선택
2. 영어 단어 입력
3. "확인" 클릭
4. Detection Result 화면으로 이동
```

---

## 🏠 2. 홈 화면 (Home Fragment)

### 2.1 학습 현황 대시보드
**설명**: 사용자의 학습 상태를 한눈에 파악할 수 있는 대시보드

#### 주요 정보:
- **연속 학습 일수**: 
  - 오늘까지 연속으로 학습한 날짜 계산
  - 하루라도 놓치면 0으로 리셋
  - 캘린더 기반 정확한 일수 계산

- **최근 7일 활동**:
  - 최근 7일 동안 학습 여부를 점(dot)으로 표시
  - 학습한 날: 파란색 점
  - 학습 안 한 날: 회색 점

- **총 학습 단어 수**:
  - 지금까지 누적 학습한 단어 개수
  - Room DB에 저장된 DetectedObject 개수

### 2.2 학습 캘린더
**설명**: 월간 학습 기록을 시각적으로 표시하는 캘린더

#### 주요 기능:
- **날짜별 학습 상태**:
  - 학습한 날: 파란색 아이콘 표시
  - 퀴즈 완료한 날: 별(Star) 아이콘 표시
  - 학습하지 않은 날: 빨간색 X 아이콘 표시

- **날짜 클릭**:
  - 클릭 시 해당 날짜 학습한 단어 목록 팝업 표시
  - 단어, 의미, 이미지 썸네일 표시

- **제약 조건**:
  - 최대 과거 1년까지 표시
  - 미래 날짜 선택 불가

### 2.3 빠른 학습 시작
- **"Magic Adventure" 버튼**: 카메라로 바로 이동하여 학습 시작

---

## 📖 3. 학습 기록 (Log Fragment)

### 3.1 날짜별 학습 기록
**설명**: 학습한 모든 단어를 날짜별로 그룹화하여 표시

#### 주요 기능:
- **섹션 헤더**: 날짜별로 구분 (예: "2024년 12월 11일")
- **GridLayout**: 2열 그리드로 카드 형식 표시
- **카드 정보**:
  - 촬영한 이미지 (최적화된 크기로 로딩)
  - 학습한 영어 단어 목록
  - 총 단어 개수

### 3.2 상세 단어 정보
- **카드 클릭 시**:
  - 해당 사진에서 학습한 모든 단어 표시
  - 단어, 의미, 예문 상세 정보
  - TTS로 발음 듣기 가능

### 3.3 성능 최적화
- **이미지 최적화**: 
  - GridLayout 크기에 맞춰 이미지 리사이징
  - 메모리 효율적 로딩
  - ViewHolder 재활용 시 리소스 정리

- **RecyclerView**: 
  - ViewHolder 패턴으로 메모리 효율성 향상
  - GridSpacing 최적화

---

## 📝 4. 퀴즈 기능 (Quiz Fragment)

### 4.1 Spaced Repetition 알고리즘
**설명**: 과학적 학습 알고리즘을 적용한 복습 시스템

#### 알고리즘 원리:
- **문제 선택 기준**: 
  - `lastStudied` 날짜가 가장 오래된 단어 우선
  - 최대 20개 단어까지 선택

- **정답 처리**:
  - `lastStudied` 날짜를 사진의 `createdAt`으로 업데이트
  - → 해당 단어는 복습 우선순위에서 뒤로 밀림

- **오답 처리**:
  - 같은 세션 내에서 큐의 끝에 다시 추가
  - → 곧바로 다시 출제되어 재학습

### 4.2 퀴즈 UI
**설명**: 4지선다 객관식 퀴즈

#### 구성 요소:
- **진행 상황**: "문제 X / 총 Y"
- **문제**: 한국어 의미 또는 영어 예문 제시
- **선택지**: 4개의 영어 단어 보기
- **정답/오답 피드백**: 
  - 정답: 초록색 하이라이트
  - 오답: 빨간색 하이라이트 + 정답 표시

### 4.3 학습 통계
**설명**: 퀴즈 화면에서 보여주는 학습 성취 지표

#### 통계 항목:
- **연속 학습 일수**: 홈 화면과 동일
- **최근 7일 활동**: 점(dot) 표시
- **복습률 (Review Rate)**:
  - 분모: 최근 7일간 촬영한 총 사진 수
  - 분자: 최근 7일간 복습한 사진 수 (lastStudied >= 7일 전)
  - 퍼센트 계산: (복습한 사진 / 전체 사진) × 100

---

## ⚙️ 5. 설정 (Setting Fragment)

### 5.1 사용자 정보 관리
- **닉네임 표시 및 수정**:
  - 현재 닉네임 표시
  - 편집 버튼 클릭 → 다이얼로그로 수정
  - Firestore 및 SharedPreferences 업데이트

- **이메일 표시**: Google 계정 이메일 표시 (읽기 전용)

### 5.2 학습 설정
- **QuickSnap 센서**:
  - ON/OFF 토글
  - 활성화 시 빠른 촬영 센서 작동
  - SharedPreferences에 저장

- **알림 설정**:
  - ON/OFF 토글
  - 활성화 시 학습 리마인더 알림 스케줄링
  - Android 13+ 알림 권한 요청
  - NotificationScheduler로 정확한 시간 알림 설정

### 5.3 데이터 관리
- **로그아웃**:
  - Firebase Auth 로그아웃
  - SharedPreferences 초기화
  - LoginActivity로 이동

- **데이터 초기화**:
  - Room DB의 모든 테이블 삭제 (PhotoLog, DetectedObject, ExampleSentence)
  - 로컬 저장된 이미지 파일 삭제
  - 학습 기록 완전 리셋

---

## 🔐 6. 인증 (Login Activity)

### 6.1 Google Sign-In
**설명**: Credential Manager를 활용한 최신 Google 로그인

#### 기능:
- **One-Tap Sign-In**: 원클릭 간편 로그인
- **Firebase Authentication**: Google 토큰으로 Firebase 인증
- **FCM Token**: 로그인 성공 시 FCM 토큰 생성 및 저장
- **자동 로그인**: 이미 로그인 상태면 자동으로 MainActivity 이동

#### 사용자 데이터 저장:
- **Firestore**: `users/{uid}` 컬렉션에 저장
  - email
  - nickname
  - fcmToken
  - stats (totalWordCount 등)
  - lastStudiedAt

- **SharedPreferences**: 로컬 캐싱
  - nickname
  - email

---

## 🔄 7. 데이터 동기화

### 7.1 Firebase Sync
**설명**: 앱 시작 시 Firestore에서 모든 단어 데이터를 로컬 DB로 동기화

#### 동기화 프로세스:
```
앱 시작 (MainActivity.onCreate)
  ↓
syncWordsFromFirebase() 호출
  ↓
Firestore에서 모든 단어 가져오기
  ↓
"firebase_sync" 더미 PhotoLog 생성/확인
  ↓
각 단어를 DetectedObject & ExampleSentence로 변환
  ↓
Room DB에 저장 (중복 체크)
  ↓
동기화 완료
```

#### 더미 PhotoLog 역할:
- `localImagePath = "firebase_sync"`로 구분
- Firestore에서 가져온 단어들의 부모 역할
- 실제 촬영 사진과 분리하여 관리

### 7.2 양방향 동기화
- **로컬 → 클라우드**: 
  - 새로운 단어 학습 시 → Firestore에 자동 저장
  
- **클라우드 → 로컬**: 
  - 앱 시작 시 → Firestore에서 전체 동기화

---

## 🎨 8. UI/UX 특징

### 8.1 Navigation
- **Bottom Navigation**: 
  - Home, Log, Magic Adventure, Quiz, Setting
  - 현재 화면 하이라이트 표시
  - Fragment 전환 시 애니메이션

- **Back Button 처리**:
  - 특정 Fragment에서 뒤로가기 시 Home으로 이동
  - Home에서 뒤로가기 2번 연속 클릭 시 앱 종료

### 8.2 WindowInsets 처리
- **Edge-to-Edge UI**: 
  - 상태바, 네비게이션바 영역까지 확장
  - WindowInsetsHelper로 적절한 패딩 적용

### 8.3 반응형 레이아웃
- **ConstraintLayout**: 다양한 화면 크기 대응
- **RecyclerView**: 효율적인 리스트 표시
- **GridLayout**: 2열 그리드 최적화

---

## 🔔 9. 알림 시스템

### 9.1 학습 리마인더
**설명**: 정해진 시간에 학습을 유도하는 푸시 알림

#### 기능:
- **정확한 시간 알림**: AlarmManager의 setExactAndAllowWhileIdle 사용
- **매일 반복**: 매일 같은 시간에 알림 발송
- **권한 관리**: Android 13+ NOTIFICATION 권한 체크
- **설정 연동**: 설정 화면에서 ON/OFF 가능

#### 구현:
- `NotificationScheduler`: 알림 스케줄링
- `NotificationReceiver`: BroadcastReceiver로 알림 수신
- `NotificationHelper`: 실제 알림 표시

---

## 📊 10. 데이터 구조

### 10.1 Room Database 엔티티

#### PhotoLog (사진 기록)
```kotlin
- photoId: Long (Primary Key)
- localImagePath: String (로컬 파일 경로)
- createdAt: Long (촬영 시간 타임스탬프)
```

#### DetectedObject (감지된 객체/단어)
```kotlin
- objectId: Long (Primary Key)
- parentPhotoId: Long (외래키)
- englishWord: String (영어 단어)
- koreanMeaning: String (한국어 의미)
- createdAt: Long (생성 시간)
- lastStudied: Long (마지막 학습 시간)
```

#### ExampleSentence (예문)
```kotlin
- exampleId: Long (Primary Key)
- parentObjectId: Long (외래키)
- englishSentence: String (영어 예문)
- koreanTranslation: String (한국어 번역)
- createdAt: Long (생성 시간)
```

### 10.2 Firestore Collections

#### users/{uid}
```javascript
{
  email: String,
  nickname: String,
  fcmToken: String,
  stats: {
    totalWordCount: Number
  },
  lastStudiedAt: Timestamp
}
```

#### users/{uid}/studyLog/{word}
```javascript
{
  lastStudied: Timestamp
}
```

#### words/{word}
```javascript
{
  originalWord: String,
  meaning: String,
  examples: [
    {
      sentence: String,
      translation: String
    }
  ]
}
```

---

## 🌟 11. 주요 차별화 기능

### 11.1 실생활 연계 학습
- 단순 단어장이 아닌, 실제 물체를 촬영하여 학습
- 시각적 기억과 언어 학습의 결합
- 생활 속 영어 단어 자연스럽게 습득

### 11.2 AI 자동 학습 콘텐츠 생성
- GPT API로 단어 의미 및 예문 자동 생성
- 사용자가 콘텐츠를 직접 입력할 필요 없음
- 모든 단어에 대해 3개의 고품질 예문 제공

### 11.3 과학적 복습 알고리즘
- Spaced Repetition 알고리즘 적용
- 장기 기억으로 전환하는 최적의 복습 주기
- 개인별 학습 패턴에 맞춘 맞춤형 퀴즈

### 11.4 음성 학습 지원
- TTS로 정확한 발음 학습
- STT로 사용자 발음 평가
- 듣기, 말하기, 읽기, 쓰기 종합 학습

### 11.5 멀티 디바이스 동기화
- Firestore 클라우드 백업
- 여러 기기에서 동일한 학습 기록 유지
- 오프라인 학습 후 자동 동기화

---

## 🚀 12. 향후 확장 가능성

### 제안 기능:
1. **그룹 학습**: 친구와 학습 기록 공유 및 경쟁
2. **배지 시스템**: 학습 성취도에 따른 보상 시스템
3. **주제별 학습**: 음식, 가구, 동물 등 카테고리별 학습
4. **AI 난이도 조절**: 사용자 실력에 맞춘 문제 출제
5. **오프라인 모드 강화**: 더 많은 기능 오프라인 지원
6. **다국어 지원**: 영어 외 다른 언어 학습 확장

---

## 📱 사용자 시나리오 예시

### 시나리오 1: 처음 사용하는 사용자
```
1. Google 계정으로 로그인
2. "Magic Adventure" 클릭
3. 카메라로 책상 위 사물 촬영
4. AI가 "book", "pen" 감지
5. "book" 클릭 → 의미, 예문 학습
6. TTS로 발음 듣기
7. 학습 완료 → Home 화면에 1개 단어 기록
```

### 시나리오 2: 복습하는 사용자
```
1. 앱 실행 → Home 화면
2. 연속 학습 7일 달성 확인
3. "Quiz" 클릭
4. Spaced Repetition 알고리즘으로 선별된 20개 문제
5. 4지선다 퀴즈 풀이
6. 오답은 다시 출제되어 재학습
7. 복습률 85% 달성
```

### 시나리오 3: 기록 확인
```
1. "Log" 화면 이동
2. 날짜별로 학습한 단어 확인
3. 지난주 월요일 카드 클릭
4. 그날 학습한 5개 단어 상세 정보 확인
5. 단어 발음 다시 듣기
```

---

## 🎯 핵심 가치

**Chalkak**은 단순히 영어 단어를 외우는 앱이 아닙니다.

1. **실생활 연계**: 일상 속 물체를 통해 자연스럽게 학습
2. **AI 자동화**: GPT가 학습 콘텐츠를 자동 생성
3. **과학적 복습**: Spaced Repetition으로 효과적인 장기 기억
4. **멀티모달 학습**: 시각(이미지) + 청각(TTS) + 운동(STT) 종합
5. **지속적 동기부여**: 연속 학습 일수, 캘린더, 통계로 성취감 제공

**"찰칵 한 번에 영어 단어 마스터!"**
