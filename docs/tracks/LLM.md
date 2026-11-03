# LLM API (LLM 브랜치 전용)

> 소유: LLM API 세션 · 상태: ⬜ 착수 전 · 최종 갱신 2026-11-03
> 이 문서는 `zyonge_LLMIntegration` 전용이다. 여기 산출물은 `main` 으로 가지 않는다 — PM이 정리해 `main` 에서 다시 구현한다.
> 세션 규칙은 `src/main/java/shop/llm/CLAUDE.md`, 파일 소유 범위는 루트 `CLAUDE.md` § 3 을 따른다.

---

## 1. 범위

이 브랜치는 **LLM API 모델 이식 · 기능 로직 · JSP 연결** 만 개발하는 특수목적 브랜치다.

| 한다 | 하지 않는다 |
|---|---|
| LLM API 클라이언트 (`shop/llm/`) | ERD · CRUD 변경 |
| LLM 기능의 Service · Servlet · DTO (`Llm*`) | DDL · 샘플 데이터 변경 |
| LLM 화면 · 팝업 JSP, 기존 화면에 진입점 연결 | CSS · 목업 · 디자인 토큰 |
| 키 · 모델 설정 (`context.xml` `<Environment>`) | LLM과 무관한 배선 수정 |

기반 구현(ERD · DB · JSP · UI)은 샌드박스 `a835e26` 시점 그대로이며 이 브랜치에서는 읽기만 한다.

## 2. 작업 위치

| 구분 | 경로 |
|---|---|
| API 계층 | `src/main/java/shop/llm/` — 클라이언트 · 프롬프트 · JSON 처리 · 설정 읽기 |
| 업무 계층 | `shop/service/Llm*Service.java` · `shop/controller/Llm*Servlet.java` · `shop/dto/Llm*DTO.java` |
| 화면 | `WebContents/llm/*.jsp` (라우팅 페이지) · 기존 화면 안 `<dialog>` (팝업) |
| 스크립트 | `WebContents/js/llm.js` (비동기 호출이 필요할 때만) |
| 설정 | `META-INF/context.xml` `<Environment>` (git 제외) · 템플릿 `context.xml.example` |

호출 흐름: `JSP → Llm*Servlet → Llm*Service → shop.llm 클라이언트 → LLM API`. DB가 필요하면 Service가 기존 DAO를 읽기 전용으로 쓴다.

## 3. 현재 상태

| 항목 | 상태 |
|---|---|
| 공급자 · 모델 | ⬜ 미정 (STATUS L1) |
| 기능 목록 · 화면 위치 | ⬜ 미정 (STATUS L2) |
| `shop/llm/` 클라이언트 | ⬜ 착수 전 |
| `context.xml.example` `<Environment>` 자리 | ⬜ 착수 전 |
| JSP 연결 | ⬜ 착수 전 |

## 4. 기존 파일 수정 기록

LLM 진입점을 붙이려고 기반 구현 파일을 고친 경우만 적는다. `main` 재구현 때 이 표가 이식 목록이 된다.

| 파일 | 수정 내용 | 커밋 |
|---|---|---|
| — | — | — |

## 5. 요청 · 미결

| 대상 | 내용 | 상태 |
|---|---|---|
| PM | 공급자 · 모델 · 키 발급 주체 (L1) | 결정 대기 |
| PM | 기능 목록과 붙일 화면 · 역할 (L2) | 결정 대기 |
| PM | 호출 비용 · 횟수 제한 방식 (L3) | 결정 대기 |
