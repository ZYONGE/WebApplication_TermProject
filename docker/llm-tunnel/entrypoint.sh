#!/bin/sh
# 1) 학교 IP 확보  2) 0.0.0.0:31434 → Ollama 서버 포워딩
set -eu

: "${OLLAMA_HOST:?.env 에 OLLAMA_HOST 를 채운다}"
: "${OLLAMA_PORT:?.env 에 OLLAMA_PORT 를 채운다}"

# 1) 학교 IP 확보 — 방법 미정 (docs/tracks/LLM.md § 8, STATUS L4)
#    정해지면 여기서 준비하고, 포워딩 대상을 그 경로로 바꾼다.
#    지금은 이 PC 의 IP 그대로 나간다 — 학교망 밖에서는 서버가 응답하지 않는다.
echo "[llm-tunnel] 학교 IP 경로 미설정 — 직접 포워딩만 한다"

# 2) 포워딩
echo "[llm-tunnel] 0.0.0.0:31434 → ${OLLAMA_HOST}:${OLLAMA_PORT}"
exec socat TCP-LISTEN:31434,fork,reuseaddr "TCP:${OLLAMA_HOST}:${OLLAMA_PORT}"
