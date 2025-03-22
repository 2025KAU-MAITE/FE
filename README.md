# FE

## 코드 스타일 가이드라인

### 기본 린터 설정

ktlint를 사용하여 기본적인 코드 스타일을 검사합니다.
복잡한 규칙은 제외하고 기본 설정만 사용합니다.

### 네이밍 컨벤션

클래스: PascalCase (예: UserProfile, MainActivity)
함수 및 변수: camelCase (예: getUserData(), userName)
상수: UPPER_SNAKE_CASE (예: MAX_COUNT, API_URL)
레이아웃 XML: snake_case (예: activity_main.xml, item_user.xml)

### 간단한 코드 구성

함수는 가능한 한 짧게 작성합니다.
긴 코드 블록은 여러 함수로 분리합니다.
한 줄에 너무 많은 코드를 작성하지 않습니다.

## 기본 프로젝트 구조

### 패키지 구성

기능별로 패키지를 구성합니다 (예: ui, model, network)
관련 파일들은 같은 패키지에 배치합니다.

### 기본 아키텍처

MVVM 패턴을 간단하게 적용합니다.
ViewModel과 LiveData를 사용하여 UI 상태를 관리합니다.

### 간단한 테스트 전략

핵심 기능에 대한 기본적인 단위 테스트를 작성합니다.
복잡한 테스트 프레임워크 없이 JUnit을 사용합니다.

## 유용한 라이브러리

### 권장 라이브러리

Retrofit: 네트워크 통신
Glide 또는 Coil: 이미지 로딩
Kotlin Coroutines: 비동기 작업

## 버전 관리 간소화

### 브랜치 관리

main: 최종 코드
develop: 개발 중인 코드
feature/: 새로운 기능 개발