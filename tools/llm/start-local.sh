#!/bin/bash
# 점검용 로컬 LLM 시작 — USB 소형 모델 서버(8081) + Ollama 형식 중계(31434). docs/tracks/LLM.md § 11
# 사용법: tools/llm/start-local.sh      (Ctrl+C 로 둘 다 종료)
#   LLM_USB=/Volumes/KM__JY/LLM  USB 의 LLM 폴더 (기본값)
#   USB_PORT=8081                USB 모델 서버 포트 — Tomcat 8080 과 겹치지 않게
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LLM_USB="${LLM_USB:-/Volumes/KM__JY/LLM}"
USB_PORT="${USB_PORT:-8081}"

[ -x "$LLM_USB/scripts/run-mac.sh" ] || { echo "USB 의 LLM 폴더를 찾지 못했다: $LLM_USB (LLM_USB 로 지정)"; exit 1; }

PORT="$USB_PORT" "$LLM_USB/scripts/run-mac.sh" > /dev/null 2>&1 &
USB_PID=$!
trap 'kill $USB_PID 2>/dev/null' EXIT

echo "[start-local] USB 모델 로딩 중 (최대 60초) — 127.0.0.1:$USB_PORT"
for _ in $(seq 1 60); do
	curl -sf -m 2 "http://127.0.0.1:$USB_PORT/health" > /dev/null && break
	kill -0 $USB_PID 2>/dev/null || { echo "USB 모델 서버가 시작되지 못했다. $LLM_USB/README.md 의 처음 쓰는 맥 절차를 확인한다."; exit 1; }
	sleep 1
done
curl -sf -m 2 "http://127.0.0.1:$USB_PORT/health" > /dev/null || { echo "USB 모델 서버 응답 없음"; exit 1; }

echo "[start-local] 중계 시작 — 앱은 http://localhost:31434/api/generate 로 부른다"
UPSTREAM="http://127.0.0.1:$USB_PORT/v1" \
	java -cp "$ROOT/WebContents/WEB-INF/lib/gson-2.15.0.jar" "$ROOT/tools/llm/OllamaBridge.java"
