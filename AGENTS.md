# AGENTS.md

이 문서는 코드만으로 파악하기 어려운 "현재 기준 맥락"만 정리합니다.
새 에이전트는 작업 시작 전에 먼저 이 문서를 확인하세요.

## 1) 목적
- 신규 에이전트가 빠르게 현재 제품 의도와 주의점을 파악하도록 돕는다.
- 과거 변경 이력보다 "지금의 동작 원칙"을 우선한다.

## 2) 현재 아키텍처
- 설정 화면: `MainActivity`
- 접근성 서비스: `AutoClickAccessibilityService`
- 오버레이: `OverlayController`, `DragTouchListener`
- 자동 탭 실행: `AutoClickEngine`
- 설정/좌표 저장: `SettingsRepository` (DataStore)

## 3) 현재 제품/UX 결정사항
- 상단 UI는 시스템 기본 ActionBar를 사용한다.
- 설정 화면 하단의 "접근성 설정으로 이동" 버튼은 유지한다.
- 오버레이 클릭 지점은 숫자 대신 십자선(크로스헤어) 스타일을 사용한다.
- 투명도는 "시작/중지 버튼이 더 투명, 클릭 지점이 덜 투명" 원칙을 유지한다.

## 4) 회귀 위험 포인트 (중요)
- API 36 환경에서 상태바가 흰색으로 보이는 이슈가 있었다.
- 현재는 `MainActivity.ensureStatusBarScrim()`로 상태바 높이만큼 상단 scrim 뷰를 추가해 해결한다.
- `statusBarColor`만으로는 동일 환경에서 재발할 수 있으니 관련 로직 제거/변경 시 반드시 실제 기기(또는 동일 API 에뮬레이터)에서 확인한다.

- ActionBar가 본문을 가리는 이슈가 있었다.
- `fixActionBarOverlapIfNeeded()`가 상단 패딩을 보정하므로, 인셋/액션바 처리 변경 시 함께 검증한다.

- 접근성 설정 이동은 기기별 편차가 있다.
- 먼저 `ACCESSIBILITY_DETAILS_SETTINGS` + `EXTRA_COMPONENT_NAME`을 시도하고, 실패 시 `ACTION_ACCESSIBILITY_SETTINGS`로 폴백한다.

## 5) 오버레이 저장 정책
- 오버레이 위치는 드래그 종료 시 DataStore에 저장된다.
- 키: `control_x/y`, `point_1_x/y`, `point_2_x/y`, `point_3_x/y`
- 초기 위치:
  - 컨트롤 버튼: `(16dp, 24dp)`
  - 포인트: 화면 중앙 기준 배치(2번은 우측, 3번은 하단)
- 화면 밖 좌표는 `DisplayBoundsHelper.clampToBounds()`로 보정한다.

## 6) 빌드/검증 메모
- `:app:assembleDebug`를 기준으로 동작 검증한다.
- 현재 환경에서는 Kotlin daemon 경고가 간헐적으로 발생하지만 fallback 컴파일로 빌드가 성공하는 패턴이 있다.

## 7) 유지 규칙
- 앞으로 작업할 때마다 이 문서를 함께 갱신한다.
- 특히 아래 변경이 있으면 반드시 반영한다:
  1. 사용자 요구사항/UX 원칙 변경
  2. 회귀 위험이 큰 우회/폴백 로직 변경
  3. 오버레이 동작/저장 정책 변경
  4. 새 에이전트가 오해하기 쉬운 제약 추가

마지막 업데이트: 2026-03-18
