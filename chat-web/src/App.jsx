import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export default function App() {
    // ---- 1) 상태는 최상단에 전부 선언 ----
    const USER_ID = 1; // v1: 로그인 대신 고정 유저
    const [stomp, setStomp] = useState(null);
    const [connected, setConnected] = useState(false);
    const subRef = useRef(null); // 현재 구독을 기억해두었다가 방 바뀔 때 해제

    const [chats, setChats] = useState([]);
    const [selectedChatId, setSelectedChatId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState("");
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");

    // ---- 2) STOMP 연결(최초 1회) ----
    useEffect(() => {
        const c = new Client({
            webSocketFactory: () => new SockJS("/ws"), // vite proxy가 8083으로 중계
            reconnectDelay: 2000,
            debug: () => {},
            onConnect: () => setConnected(true),
            onStompError: () => setConnected(false),
            onWebSocketClose: () => setConnected(false),
        });

        c.activate();
        setStomp(c);

        // 언마운트 시 연결 해제
        return () => {
            try { c.deactivate(); } catch {}
        };
    }, []);

    // ---- 3) 방이 선택될 때마다 해당 /topic 구독 ----
    useEffect(() => {
        if (!stomp || !connected || !selectedChatId) return;

        // 이전 구독이 있으면 해제
        if (subRef.current) {
            try { subRef.current.unsubscribe(); } catch {}
            subRef.current = null;
        }

        const sub = stomp.subscribe(`/topic/chats/${selectedChatId}`, (frame) => {
            try {
                const body = JSON.parse(frame.body);
                // 새 메시지를 목록 맨 위에 추가 (서버는 DESC 기준으로 응답)
                setMessages((prev) => [body, ...prev]);
            } catch {}
        });

        subRef.current = sub;
        return () => {
            try { sub.unsubscribe(); } catch {}
            subRef.current = null;
        };
    }, [stomp, connected, selectedChatId]);

    // ---- 4) 채팅방 목록 불러오기 ----
    const loadChats = async () => {
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
    };

    // ---- 5) 메시지 목록 불러오기 ----
    const loadMessages = async (chatId) => {
        try {
            setErr("");
            setLoading(true);
            setSelectedChatId(chatId);
            const res = await fetch(
                `/api/messages?chatId=${chatId}&page=0&size=50`,
                { headers: { "X-USER-ID": String(USER_ID) } }
            );
            if (!res.ok) throw new Error(`메시지 목록 실패 ${res.status}`);
            const data = await res.json();
            setMessages(data.content ?? []);
        } catch (e) {
            setErr(String(e));
        } finally {
            setLoading(false);
        }
    };

    // ---- 6) 메시지 보내기 ----
    const sendMessage = async (e) => {
        e.preventDefault();
        if (!selectedChatId || !text.trim()) return;
        try {
            setErr("");
            const res = await fetch(`/api/messages`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-USER-ID": String(USER_ID),
                },
                body: JSON.stringify({ chatId: selectedChatId, content: text }),
            });
            if (!res.ok) throw new Error(`전송 실패 ${res.status}`);
            const msg = await res.json();
            setMessages((prev) => [msg, ...prev]); // 낙관적 갱신 + 서버도 방송함
            setText("");
        } catch (e) {
            setErr(String(e));
        }
    };

    // ---- 7) 최초 1회 채팅방 목록 ----
    useEffect(() => { loadChats(); }, []);

    return (
        <div style={styles.app}>
            <header style={styles.header}>
                <h1 style={{ margin: 0 }}>Mini Chat</h1>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 12, color: connected ? "#2b6" : "#b00" }}>
            {connected ? "WS 연결됨" : "WS 연결안됨"}
          </span>
                    <button onClick={loadChats} style={styles.refreshBtn}>⟳ 새로고침</button>
                </div>
            </header>

            <div style={styles.main}>
                {/* 왼쪽: 채팅방 목록 */}
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

                {/* 오른쪽: 메시지 영역 */}
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
