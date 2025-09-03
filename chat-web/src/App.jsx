import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs.min.js"; // 브라우저용 번들 사용

export default function App() {
    // 상태 선언
    const USER_ID = 1; // 시드 데이터 기준으로 1=alice
    const [stomp, setStomp] = useState(null);
    const [connected, setConnected] = useState(false);
    const subRef = useRef(null);
    const [chats, setChats] = useState([]);
    const [selectedChatId, setSelectedChatId] = useState(null);
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState("");
    const [loading, setLoading] = useState(false);
    const [err, setErr] = useState("");

    // STOMP 연결(한 번만)
    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS("/ws"),
            reconnectDelay: 2000,
            onConnect: () => setConnected(true),
            onStompError: () => setConnected(false),
            onWebSocketClose: () => setConnected(false),
        });
        client.activate();
        setStomp(client);
        return () => {
            try { client.deactivate(); } catch {}
        };
    }, []);

    // 방 선택 시 구독 관리
    useEffect(() => {
        if (!stomp || !connected || !selectedChatId) return;
        // 기존 구독 해제
        if (subRef.current) {
            try { subRef.current.unsubscribe(); } catch {}
            subRef.current = null;
        }
        // 새 주제 구독
        const sub = stomp.subscribe(`/topic/chats/${selectedChatId}`, frame => {
            try {
                const body = JSON.parse(frame.body);
                setMessages(prev => [body, ...prev]);
            } catch { /* ignore */ }
        });
        subRef.current = sub;
        // 정리
        return () => {
            try { sub.unsubscribe(); } catch {}
            subRef.current = null;
        };
    }, [stomp, connected, selectedChatId]);

    // 채팅방 목록 불러오기
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

    // 메시지 목록 불러오기
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

    // 메시지 전송
    async function sendMessage(e) {
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
            // 낙관적 갱신: 바로 목록 갱신
            setMessages(prev => [msg, ...prev]);
            setText("");
        } catch (e) {
            setErr(String(e));
        }
    }

    // 최초 방 목록
    useEffect(() => { loadChats(); }, []);

    return (
        <div style={styles.app}>
            {/* UI 내용은 기존과 동일 */}
            {/* ... */}
        </div>
    );
}

// styles 객체는 기존과 동일하므로 생략
