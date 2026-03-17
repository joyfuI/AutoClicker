# AutoClicker
오토마우스 안드로이드 앱

이 프로젝트는 **OpenAI Codex**로 제작 되었습니다.
설정 화면 UI는 **Jetpack Compose + Material 3** 기반입니다.

## 주요 기능
- 터치 지점 개수 설정 (1~3개)
- 터치 간격 설정 (10~5000ms)
- 화면 오버레이로 시작/중지 버튼 및 터치 지점 드래그 이동
- 오버레이 위치/설정값 DataStore 자동 저장
- 설정 화면에서 접근성 설정 화면으로 빠른 이동

## 요구 사항
- Android `minSdk 26`, `targetSdk 36`
- JDK 11 이상
- Android Studio 최신 버전 권장

## 빌드
프로젝트 루트에서:

```bash
./gradlew :app:assembleDebug
```

Windows(PowerShell):

```powershell
.\gradlew.bat :app:assembleDebug
```

## 사용 방법
1. 앱 실행 후 터치 지점 개수/터치 간격을 설정합니다.
2. `접근성 설정으로 이동` 버튼을 눌러 접근성 설정에서 `오토클리커`를 활성화합니다.
3. 오버레이가 나타나면 시작 버튼으로 자동 탭을 실행합니다.
4. 오버레이 버튼/지점을 드래그해 원하는 위치로 옮길 수 있습니다.

## 프로젝트 구조
- `app/src/main/java/com/joyfui/autoclicker/MainActivity.kt`: 설정 화면
- `app/src/main/java/com/joyfui/autoclicker/service/AutoClickAccessibilityService.kt`: 접근성 서비스 진입점
- `app/src/main/java/com/joyfui/autoclicker/overlay/`: 오버레이 UI/드래그 처리
- `app/src/main/java/com/joyfui/autoclicker/click/AutoClickEngine.kt`: 자동 탭 루프 실행
- `app/src/main/java/com/joyfui/autoclicker/data/`: 설정/좌표 저장 모델 및 저장소

## 참고
- 기기/OS 버전에 따라 접근성 설정 상세 화면 직접 이동 지원 여부가 다를 수 있어, 앱에서 자동 폴백 처리합니다.
