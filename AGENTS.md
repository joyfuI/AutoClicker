# AGENTS.md

이 문서는 코드만으로 파악하기 어려운 현재 기준 맥락만 정리합니다.
과거 이력보다 현재 동작 원칙과 회귀 포인트를 우선합니다.

## 1) 목적
- 신규 에이전트가 현재 제품 의도와 주의점을 빠르게 파악하도록 돕는다.
- 작업 판단에 필요한 결정사항만 남긴다.

## 2) 현재 아키텍처
- 설정 화면: `MainActivity` (`ComponentActivity` + Jetpack Compose + Material 3)
- 접근성 서비스: `AutoClickAccessibilityService`
- 오버레이: `OverlayController`, `DragTouchListener`
- 자동 탭 실행: `AutoClickEngine`
- 설정/좌표 저장: `SettingsRepository` (DataStore)
- 설정 UI는 Compose 코드로만 구성된다.

## 3) 현재 제품/UX 결정사항
- 상단 UI는 Compose `TopAppBar`를 사용한다.
- 설정 화면 하단의 "접근성 설정으로 이동" 버튼을 유지한다.
- 오버레이 클릭 지점은 숫자 대신 십자선(크로스헤어) 스타일을 사용한다.
- 투명도 원칙: 시작/중지 버튼이 더 투명, 클릭 지점이 덜 투명.
- UI 표준은 Compose Material 3 기준으로 유지한다.
- Compose 상태 수집은 `collectAsState`를 사용한다.

## 4) 회귀 위험 포인트 (중요)
- API 36 환경에서 상태바가 흰색으로 보이는 이슈가 있었다.
- 현재 `MainActivity.ensureStatusBarScrim()`로 상태바 높이의 scrim 뷰를 추가해 완화한다.
- `statusBarColor`만으로는 재발 가능성이 있어 관련 로직 변경 시 실제 기기(또는 동일 API 에뮬레이터)에서 확인한다.
- `Scaffold`와 시스템 인셋(`WindowInsets`)을 함께 사용하므로 상단 여백/겹침 변경 시 실제 기기에서 재검증한다.
- 접근성 설정 이동은 기기별 편차가 있어, `ACCESSIBILITY_DETAILS_SETTINGS` + `EXTRA_COMPONENT_NAME` 실패 시 `ACTION_ACCESSIBILITY_SETTINGS`로 폴백한다.

## 5) 오버레이 저장 정책
- 오버레이 위치는 드래그 종료 시 DataStore에 저장된다.
- 키: `control_x/y`, `point_1_x/y`, `point_2_x/y`, `point_3_x/y`
- 초기 위치:
  - 컨트롤 버튼: `(16dp, 24dp)`
  - 포인트: 화면 중앙 기준 배치(2번은 우측, 3번은 하단)
- 화면 밖 좌표는 `DisplayBoundsHelper.clampToBounds()`로 보정한다.

## 6) 빌드/검증 메모
- 기본 검증: `:app:assembleDebug`
- 현재 환경에서는 Kotlin daemon 경고가 간헐적으로 발생할 수 있으나 fallback 컴파일로 성공하는 패턴이 있다.
- 현재 프로젝트는 테스트 소스/테스트 의존성을 제거한 상태다.
  - 테스트 재도입 시 `build.gradle.kts`의 test/androidTest 설정을 함께 복구한다.
- 런처 아이콘은 Android 적응형 아이콘 표준 구조를 사용한다.
  - `mipmap-anydpi-v26`의 `ic_launcher*.xml`
  - `mipmap-*`의 `ic_launcher.webp`, `ic_launcher_round.webp`, `ic_launcher_foreground.webp`
  - `values/ic_launcher_background.xml`

## 7) 유지 규칙
- 앞으로 작업할 때마다 이 문서를 함께 갱신한다.
- 아래 변경이 있으면 반드시 반영한다.
  1. 사용자 요구사항/UX 원칙 변경
  2. 회귀 위험이 큰 우회/폴백 로직 변경
  3. 오버레이 동작/저장 정책 변경
  4. 새 에이전트가 오해하기 쉬운 제약 추가
