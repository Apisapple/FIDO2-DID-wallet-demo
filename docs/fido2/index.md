# FIDO2(Fast Identity Online 2)

FIDO 얼라이언스와 W3C가 협력하여 개발한 개방형 인증 표준으로, 비밀번호 없이 생체 인식(지문, 얼굴)이나 PIN, 보안 키(USB, NFC)를 사용하여 Web, App에 안전하게 로그인하는 기술.
Passkeys 기술의 기반이며, 피싱 방지가 가능하고 공개키 암호화 방식을 사용해 서버가 비밀번호를 저장하지 않아 보안성이 높음.

## 핵심 특징 및 구성 요소

- 패스워드리스(Passwordless): 비밀번호 입력이나 기억이 필요 없는 편리한 인증 환경 제공.
- W3C WebAuthn & CTAP: 웹 브라우저 내장 인증(WebAuthn)과 외부 인증자(CTAP, 보안 키/ 모바일)를 결합하여 데스크톱과 모바일 환경 모두에 동작
- 보안성 강화: 기기 내에 인증 정보가 바인딩되어 있으며, 서버에는 공개 키만 저장되어 탈취되어도 사용이 불가능.
- 플랫폼 지원: Windows, Apple, Android, 주요 브라우저에서 기본적으로 지원.

## FIDO1과 FIDO2의 차이점

|                                         | FIDO1 | FIDO2 |
| --------------------------------------- | ----- | ----- |
| UAF(Universal Authentication Framework) | O     | O     |
| U2F(Universal 2nd Factor)               | O     | O     |
| WebAuthn(Web Authentication)            | X     | O     |
| CTAP(Client to Authenticator Protocol)  | X     | O     |

UAF : 패스워드가 없는 인증을 제공하는 프로토콜

U2F : 기존 패스워드 기반 인증에 보안 요소를 추가한 프로토콜

## [WebAuthn](./webauthn.md)

Web Authentication Woriking Group에서 개발한 웹 인증 표준으로 웹, 네이티브 애플리케이션에서 사용자의 인증 정보를 안전하게 관리하고 패스워드 없는 인증을 구현할 수 있도록 지원. iOS와 Android 같은 플랫폼에서 제공하는 생체 인증 API를 사용해 WebAuthn을 따르는 인증을 구현할 수 있음
