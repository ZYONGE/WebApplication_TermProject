#!/bin/bash
# LLM 연결 확인 — context.xml 의 llm/ollamaUrl 과 같은 주소에 Ollama 형식으로 묻는다.
# tools/llm/start-relay.sh 로 k8s 중계를 띄운 뒤 학교 서버 연결을 이 스크립트로 확인한다. docs/tracks/LLM.md § 8
# 사용법: tools/llm/check-server.sh [생성 주소]   (기본 http://localhost:31434/api/generate)
set -u
URL="${1:-http://localhost:31434/api/generate}"
BASE="${URL%/api/generate}"
MODEL="${LLM_MODEL:-gemma3:12b}"
FAIL=0

echo "[1/3] 모델 목록: $BASE/api/tags"
curl -sf -m 5 "$BASE/api/tags" || { echo "FAIL: 연결할 수 없다 (서버 · 중계가 떠 있는지 확인)"; exit 1; }
echo

echo "[2/3] 생성 요청 (최대 180초)"
RESP="$(curl -s -m 180 -w '\n%{http_code}' "$URL" -H 'Content-Type: application/json' \
	-d "{\"model\":\"$MODEL\",\"prompt\":\"'준비 완료' 라고만 답해라.\",\"stream\":false}")"
CODE="${RESP##*$'\n'}"; BODY="${RESP%$'\n'*}"
echo "HTTP $CODE  $BODY"
[ "$CODE" = 200 ] && printf '%s' "$BODY" | grep -q '"response"' || { echo "FAIL: response 필드 없음"; FAIL=1; }

echo "[3/3] JSON 형식 요청 (format: json)"
RESP="$(curl -s -m 180 -w '\n%{http_code}' "$URL" -H 'Content-Type: application/json' \
	-d "{\"model\":\"$MODEL\",\"prompt\":\"{\\\"ok\\\": true} 를 그대로 JSON 으로 답해라.\",\"format\":\"json\",\"stream\":false}")"
CODE="${RESP##*$'\n'}"; BODY="${RESP%$'\n'*}"
echo "HTTP $CODE  $BODY"
[ "$CODE" = 200 ] && printf '%s' "$BODY" | grep -q 'ok' || { echo "FAIL: JSON 응답 아님"; FAIL=1; }

[ $FAIL = 0 ] && echo "PASS" || { echo "FAIL"; exit 1; }
