# Chat Server & Frontend

## 소개
실시간 1:1 채팅 서버와 React 클라이언트로 구성된 프로젝트입니다.

## 주요 기능
- REST API로 채팅방/메시지 CRUD
- WebSocket(STOMP)로 실시간 메시지 브로드캐스트
- MySQL + Spring Data JPA
- Vite + React 기반 프런트엔드

## 사용 방법
```bash
git clone https://github.com/사용자명/chat-server.git
cd chat-server/chat-server
./gradlew bootRun    # 백엔드 실행
cd ../chat-web
npm install
npm run dev          # 프런트 개발 서버 실행
