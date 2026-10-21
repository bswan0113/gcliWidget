# gcliWidget

Gemini API와 JavaFX로 만든 데스크탑 캘린더 위젯입니다. 자연어로 일정을 추가·완료·삭제·복사하고, 화면에서 일정을 옮기거나 수정할 수 있습니다. 2025년 제작했습니다.

[설치 파일](https://github.com/bswan0113/gcliWidget/releases) · [사용법](https://velog.io/@bswan0113/GCiWidget-Gemini-기반-데스크탑-캘린더-위젯) · [2.1 변경 기록](https://velog.io/@bswan0113/GCiWidget-2.1V)

## 동작

자연어 입력 → 일정 컨텍스트와 함께 Gemini API 호출 → JSON 응답 해석 → 일정 반영

2.1에서는 기존 일정을 텍스트로 전달하고, 수정·삭제할 일정의 날짜와 제목을 컨텍스트에서 찾도록 프롬프트를 바꿨습니다.

## 코드

소스는 `gcliwidget/src/main/java/com/example/`에 있습니다.

| 파일 | 역할 |
| --- | --- |
| `App.java` | 입력 처리와 화면 연결 |
| `GeminiService.java` | API 요청과 응답 해석 |
| `CalendarDataManager.java` | 일정 데이터와 컨텍스트 생성 |
| `CalendarView.java` | 캘린더 화면 |
| `NotificationService.java` | 일정 알림 |

Java 21 · JavaFX 21 · Gson · Maven

## 빌드

JDK 21과 Maven이 필요합니다.

```sh
cd gcliwidget
mvn package
```

앱 사용에는 Gemini API 키가 필요합니다. 현재 소스는 제작 당시의 `gemini-1.5-flash` 모델을 지정하고 있으므로, 실행 환경에 맞는 모델 확인이 필요합니다.
