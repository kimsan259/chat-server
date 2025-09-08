// src/App.jsx
import React, { useEffect, useState } from "react";
import SockJS from "sockjs-client/dist/sockjs.min.js"; // 브라우저용 번들

// 스타일 정의 그대로 사용
const styles = {
    app: { fontFamily: "system-ui, Arial", height: "100vh", display: "flex", flexDirection: "column" },
    header: { display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 16px", borderBottom: "1px solid #eee" },
    refreshBtn: { padding: "6px 10px", borderRadius: 6, border: "1px solid #ddd", cursor: "pointer", background: "#fff" },
    main: { flex: 1, display: "grid", gridTemplateColumns: "280px 1fr", minHeight: 0 },
    sidebar: { borderRight: "1px solid #eee", overflow: "auto" },
    sidebarTitle: { padding: 12, fontWeight: 700, borderBottom: "1px solid #f0f0f0" },
    chatItem: { width: "100%", textAlign: "left", padding: 12, border: "none", borderBottom: "1px solid #f7f7f7", background: "#fff", cursor: "pointer" },
    chatItemActive: { background: "#f5f8ff" },
    chatTitle: { fontWeight: 600, marginBottom: 4 },
    preview: { fontSize: 12, color: "#666" },
    section: { display: "flex", flexDirection: "column", minWidth: 0 },
    roomHeader: { padding: 12, fontWeight: 700, borderBottom: "1px solid #f0f0f0" },
    messageList: { flex: 1, overflow: "auto", display: "flex", flexDirection: "column", gap: 8, padding: 12 },
    msgRow: { display: "flex" },
    msgBubble: { background: "#f6f6f6", borderRadius: 12, padding: 10, maxWidth: 600 },
    msgMeta: { fontSize: 12, color: "#777", marginBottom: 4 },
    inputBar: { display: "flex", gap: 8, padding: 12, borderTop: "1px solid #eee" },
    input: { flex: 1, padding: "10px 12px", borderRadius: 8, border: "1px solid #ddd" },
    sendBtn: { padding: "10px 14px", borderRadius: 8, border: "1px solid #2b6", background: "#2b6", color: "#fff", cursor: "pointer" },
    empty: { padding: 16, color: "#666" },
    error: { padding: 12, color: "#b00", borderTop: "1px solid #f0d0d0", background: "#fff5f5" },
};

export default function App() {
    // 시드 데이터 기준 사용자 ID (로그인 시스템 대체)
    const USER_ID = 1;

    // SockJS 인스턴스와 연결 상태
    const [socket, setSocket] = useState(null);
    const [connected, setConnected] = useState(false);

    // UI 상태들
    const [chats, setChats] = useState([]);
    const [selectedChatId, setSelectedChatId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState("");
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");

    /**
     * 최초 1회: SockJS로 서버의 /ws-handler 엔드포인트에 연결
     * - 쿼리스트링으로 userId 전달 (Handshake 인터셉터가 읽음)
     * - onopen: 연결 성공시 상태 업데이트
     * - onmessage: 서버에서 브로드캐스트하는 메시지를 수신
     * - onclose: 연결 종료시 상태 초기화
     */
    useEffect(() => {
        const sock = new SockJS(`/ws-handler?userId=${USER_ID}`);
        sock.onopen = () => {
            setConnected(true);
        };
        sock.onmessage = (event) => {
            try {
                const msg = JSON.parse(event.data);
                // 최신순으로 추가 (서버가 최신순으로 보내므로 맨 앞에 삽입)
                setMessages((prev) => [msg, ...prev]);
            } catch {
                // JSON 파싱 실패 시 무시
            }
        };
        sock.onclose = () => {
            setConnected(false);
        };
        setSocket(sock);

        // 컴포넌트 언마운트 시 연결 종료
        return () => {
            sock.close();
        };
    }, []);

    /** 채팅방 목록 조회 (REST) */
    async function loadChats() {
        try {
            setErr("");
            const res = await fetch(`/api/chats?page=0&size=20`, {
                headers: { "X-USER-ID": String(USER_ID) },
            });
            if (!res.ok) throw new Error(`채팅방 목록 실패 ${res.status}`);
            const data = await res.json();
            setChats(data.content ?? []);
        } catch (e) {
            setErr(String(e));
        }
    }

    /** 선택한 방의 메시지 목록 조회 (REST) */
    async function loadMessages(chatId) {
        try {
            setErr("");
            setLoading(true);
            setSelectedChatId(chatId);
            const res = await fetch(`/api/messages?chatId=${chatId}&page=0&size=50`, {
                headers: { "X-USER-ID": String(USER_ID) },
            });
            if (!res.ok) throw new Error(`메시지 목록 실패 ${res.status}`);
            const data = await res.json();
            setMessages(data.content ?? []);
        } catch (e) {
            setErr(String(e));
        } finally {
            setLoading(false);
        }
    }

    /** 메시지 전송: WebSocket을 통해 JSON 전송 */
    async function sendMessage(e) {
        e.preventDefault();
        // 방 선택, 입력값, WebSocket 연결 상태 확인
        if (!selectedChatId || !text.trim() || !socket || socket.readyState !== 1) return;
        try {
            setErr("");
            // 서버가 {chatId, content} JSON을 받으면 저장 후 브로드캐스트
            socket.send(JSON.stringify({ chatId: selectedChatId, content: text }));
            setText(""); // 입력창 비움 (낙관적 업데이트 없음)
        } catch (e) {
            setErr(String(e));
        }
    }

    // 최초에 한 번 채팅방 목록 불러오기
    useEffect(() => {
        loadChats();
    }, []);

    return (
        <div style={styles.app}>
            {/* 상단 헤더: 앱 제목, 연결 상태, 새로고침 */}
            <header style={styles.header}>
                <h1 style={{ margin: 0 }}>Mini Chat</h1>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 12, color: connected ? "#2b6" : "#b00" }}>
            {connected ? "WS 연결됨" : "WS 연결안됨"}
          </span>
                    <button onClick={loadChats} style={styles.refreshBtn}>⟳ 새로고침</button>
                </div>
            </header>

            {/* 메인 영역: 왼쪽(채팅방 목록) + 오른쪽(메시지 영역) */}
            <div style={styles.main}>
                {/* 채팅방 목록 */}
                <aside style={styles.sidebar}>
                    <div style={styles.sidebarTitle}>내 채팅방</div>
                    {chats.length === 0 && <div style={styles.empty}>방이 없습니다</div>}
                    {chats.map((c) => (
                        <button
                            key={c.chatId}
                            style={{
                                ...styles.chatItem,
                                ...(selectedChatId === c.chatId ? styles.chatItemActive : {}),
                            }}
                            onClick={() => loadMessages(c.chatId)}
                        >
                            <div style={styles.chatTitle}>
                                {c.type === "DIRECT" ? "1:1" : "그룹"} · #{c.chatId}
                            </div>
                            <div style={styles.preview}>{c.lastMessagePreview ?? ""}</div>
                        </button>
                    ))}
                </aside>

                {/* 메시지 영역 */}
                <section style={styles.section}>
                    {!selectedChatId ? (
                        <div style={styles.empty}>왼쪽에서 방을 선택하세요</div>
                    ) : (
                        <>
                            <div style={styles.roomHeader}>채팅방 #{selectedChatId}</div>
                            <div style={styles.messageList}>
                                {loading && <div style={styles.empty}>불러오는 중...</div>}
                                {!loading && messages.length === 0 && (
                                    <div style={styles.empty}>메시지가 없습니다</div>
                                )}
                                {/* 메시지 출력 */}
                                {messages.map((m) => (
                                    <div key={m.id} style={styles.msgRow}>
                                        <div style={styles.msgBubble}>
                                            <div style={styles.msgMeta}>
                                                보낸이:{m.senderId} · {new Date(m.createdAt).toLocaleString()}
                                            </div>
                                            <div>{m.content}</div>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* 메시지 입력 폼 */}
                            <form onSubmit={sendMessage} style={styles.inputBar}>
                                <input
                                    value={text}
                                    onChange={(e) => setText(e.target.value)}
                                    placeholder="메시지를 입력하세요"
                                    style={styles.input}
                                />
                                <button style={styles.sendBtn}>보내기</button>
                            </form>
                        </>
                    )}
                </section>
            </div>

            {/* 에러 메시지 표시 */}
            {err && <div style={styles.error}>⚠ {err}</div>}
        </div>
    );
}
