// src/App.jsx
import React, { useEffect, useState } from "react";
import SockJS from 'sockjs-client/dist/sockjs.min.js';

// 스타일 정의 (필요시 조정)
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
    error: { padding: 12, color: "#b00", borderTop: "1px solid #f0d0d0", background: "#fff5f5" }
};

export default function App() {
    // 임시 로그인: 사용자 ID 고정
    const USER_ID = 1;

    // WebSocket 상태
    const [socket, setSocket] = useState(null);
    const [connected, setConnected] = useState(false);

    // 화면 상태
    const [chats, setChats] = useState([]);
    const [selectedChatId, setSelectedChatId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState("");
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");

    /**
     * 1) 컴포넌트 마운트 시 SockJS 연결 생성
     *   - userId를 쿼리스트링으로 전달: 서버 HandshakeInterceptor가 세션 속성에 저장.
     */
    useEffect(() => {
        const sock = new SockJS(`/ws-handler?userId=${USER_ID}`);
        sock.onopen = () => setConnected(true);
        sock.onmessage = (event) => {
            try {
                const msg = JSON.parse(event.data);
                // 최신 메시지를 맨 앞에 추가
                setMessages((prev) => [msg, ...prev]);
            } catch {
                // 파싱 실패 시 무시
            }
        };
        sock.onclose = () => setConnected(false);
        setSocket(sock);

        // 언마운트 시 소켓 닫기
        return () => {
            sock.close();
        };
    }, []);

    /** 채팅방 목록을 불러오는 REST API */
    async function loadChats() {
        try {
            setErr("");
            const res = await fetch(`/api/chats?page=0&size=20`, {
                headers: { "X-USER-ID": String(USER_ID) }
            });
            if (!res.ok) throw new Error(`채팅방 목록 실패 ${res.status}`);
            const data = await res.json();
            setChats(data.content ?? []);
        } catch (e) {
            setErr(String(e));
        }
    }

    /** 선택한 채팅방의 메시지 목록을 불러오는 REST API */
    async function loadMessages(chatId) {
        try {
            setErr("");
            setLoading(true);
            setSelectedChatId(chatId);
            const res = await fetch(`/api/messages?chatId=${chatId}&page=0&size=50`, {
                headers: { "X-USER-ID": String(USER_ID) }
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

    /** 메시지 전송: SockJS로 JSON 전달 */
    async function sendMessage(e) {
        e.preventDefault();
        if (!selectedChatId || !text.trim() || !socket || socket.readyState !== 1) return;
        try {
            setErr("");
            socket.send(JSON.stringify({ chatId: selectedChatId, content: text }));
            setText("");
        } catch (e) {
            setErr(String(e));
        }
    }

    // 초기 로딩 시 채팅방 목록 가져오기
    useEffect(() => {
        loadChats();
    }, []);

    return (
        <div style={styles.app}>
            {/* 헤더: 앱 제목 + 연결 상태 + 새로고침 버튼 */}
            <header style={styles.header}>
                <h1 style={{ margin: 0 }}>Mini Chat</h1>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 12, color: connected ? "#2b6" : "#b00" }}>
            {connected ? "WS 연결됨" : "WS 연결안됨"}
          </span>
                    <button onClick={loadChats} style={styles.refreshBtn}>⟳ 새로고침</button>
                </div>
            </header>

            {/* 본문: 왼쪽 채팅방 목록 / 오른쪽 메시지 영역 */}
            <div style={styles.main}>
                <aside style={styles.sidebar}>
                    <div style={styles.sidebarTitle}>내 채팅방</div>
                    {chats.length === 0 && <div style={styles.empty}>방이 없습니다</div>}
                    {chats.map((c) => (
                        <button
                            key={c.chatId}
                            style={{
                                ...styles.chatItem,
                                ...(selectedChatId === c.chatId ? styles.chatItemActive : {})
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
            {err && <div style={styles.error}>⚠ {err}</div>}
        </div>
    );
}
