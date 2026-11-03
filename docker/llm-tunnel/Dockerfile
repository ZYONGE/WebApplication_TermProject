# LLM 서버 접속 터널 이미지 — 포트 포워딩(socat)
FROM alpine:3.20

# 학교 IP 를 얻는 방법이 정해지면 필요한 패키지를 여기에 더한다 (docs/tracks/LLM.md § 8)
RUN apk add --no-cache socat

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/bin/sh", "/entrypoint.sh"]
