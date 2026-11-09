#!/bin/bash
# LLM 중계 시작 — 클러스터에 k8s/llm-relay.yaml 을 올리고 127.0.0.1:31434 로 port-forward 한다.
# 앱(context.xml llm/ollamaUrl)은 http://localhost:31434/api/generate 를 그대로 부른다. docs/tracks/LLM.md § 8
#
# 사용법: tools/llm/start-relay.sh        중계 시작 (Ctrl+C 로 port-forward 만 끝낸다. 파드는 남는다)
#         tools/llm/start-relay.sh down   클러스터의 중계 파드를 지운다
#
# 먼저 할 것: kubeconfig(~/.kube/config) 가 자기 학번 context 를 가리키고,
#             클러스터 API 터널이 열려 있어야 한다 (README Build & Run).
#   ssh -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -L 36443:220.149.119.234:6443 studXX@220.149.119.234
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PORT="${RELAY_PORT:-31434}"

kubectl --request-timeout=10s get --raw=/readyz > /dev/null 2>&1 || {
	echo "클러스터 API 에 닿지 않는다 — 터널(localhost:36443)과 context($(kubectl config current-context 2>/dev/null))를 확인한다."
	exit 1
}

if [ "${1:-}" = "down" ]; then
	kubectl delete -f "$ROOT/k8s/llm-relay.yaml" --ignore-not-found
	exit $?
fi

kubectl apply -f "$ROOT/k8s/llm-relay.yaml" || exit 1
kubectl rollout status deployment/llm-relay --timeout=120s || exit 1

echo "[start-relay] 127.0.0.1:$PORT → llm-relay → 수업 Ollama — 확인: tools/llm/check-server.sh"
exec kubectl port-forward --address 127.0.0.1 deployment/llm-relay "$PORT:31434"
