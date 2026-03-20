---
name: "FIDO2 DID Wallet Plan"
description: "Use when planning a FIDO2-enabled DID Wallet MVP architecture, delivery roadmap, and security checklist."
argument-hint: "예: Spring Boot API, 3개월 MVP, 오프라인 서명/복구 정책"
agent: "agent"
---

FIDO2가 적용된 DID Wallet 개발 계획서를 작성하라.

입력:

- 사용자 인자(한 줄): 프로젝트 제약/목표/환경

기본 고정값(인자에 별도 지시가 없으면 반드시 적용):

- 클라이언트: Android 네이티브(Kotlin)
- DID 메서드: did:ion
- VC/VP 포맷: JSON-LD VC/VP

참고 문서:

- [프로젝트 개요](../../README.md)
- [FIDO2 문서](../../docs/fido2/index.md)
- [DID 문서(현재 비어 있음)](../../docs/did/index.md)
- [ZKP 문서(현재 비어 있음)](../../docs/zkp/index.md)

작성 규칙:

1. 인자에 없는 내용은 "가정"으로 명시하고, 근거를 한 줄로 덧붙인다.
2. DID/ZKP 정보가 부족하면 즉시 적용 가능한 최소 설계와 추후 확장 포인트를 분리한다.
3. 구현 기술은 현재 저장소 맥락(예: Kotlin/Spring Boot)을 우선 제안하되, 대안 1개를 함께 제시한다.
4. 보안 항목은 인증, 키 관리, 디바이스 분실, 서버 침해, 피싱 대응을 반드시 포함한다.
5. did:ion 운용 시 필요한 앵커링/해결(resolution) 전략과 운영 비용 관점의 트레이드오프를 포함한다.
6. JSON-LD VC/VP 처리 시 정규화/검증 라이브러리 선택 기준과 성능 리스크를 포함한다.

출력 형식:

1. 목표와 범위 (MVP 기준)
2. 시스템 아키텍처

- 클라이언트(월렛 앱)
- 서버(API/검증)
- 신뢰 앵커/레지스트리

3. 핵심 플로우

- FIDO2 등록
- FIDO2 인증
- DID 생성/연결
- VC 발급/제시/검증
- did:ion 해석(Resolve) 실패 시 폴백 전략

4. 데이터 모델 초안

- 엔터티 목록과 관계
- 저장 위치(디바이스/서버)

5. 위협 모델 및 대응

- 위협, 영향도, 대응책, 탐지 방법

6. 단계별 로드맵

- 0~2주, 3~6주, 7~10주, 11~12주
- 각 단계의 산출물/완료 기준

7. 즉시 결정이 필요한 이슈 Top 5

- 질문 형태로 작성

응답 톤:

- 실행 가능한 설계 문서 스타일
- 표와 체크리스트를 적극 사용
- 모호한 표현 대신 수용 기준(acceptance criteria)으로 마무리
