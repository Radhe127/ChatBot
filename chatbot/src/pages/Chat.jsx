import { useEffect, useRef, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { chatApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import MessageBubble from '../components/MessageBubble';

export default function Chat() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const bottomRef = useRef(null);

  const loadSessions = useCallback(async () => {
    const { data } = await chatApi.listSessions();
    setSessions(data);
    return data;
  }, []);

  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSelectSession = async (sessionId) => {
    setActiveSessionId(sessionId);
    setLoadingMessages(true);
    try {
      const { data } = await chatApi.getMessages(sessionId);
      setMessages(data);
    } finally {
      setLoadingMessages(false);
    }
  };

  const handleNewChat = () => {
    setActiveSessionId(null);
    setMessages([]);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || sending) return;

    setInput('');
    setMessages((prev) => [...prev, { id: `temp-${Date.now()}`, role: 'user', content: text }]);
    setSending(true);

    try {
      const { data } = await chatApi.sendMessage(activeSessionId, text);
      setMessages((prev) => {
        const withoutTemp = prev.filter((m) => !String(m.id).startsWith('temp-'));
        return [...withoutTemp, data.userMessage, data.assistantMessage];
      });
      if (!activeSessionId) {
        setActiveSessionId(data.sessionId);
        await loadSessions();
      }
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { id: `err-${Date.now()}`, role: 'assistant', content: 'Something went wrong reaching the assistant. Please try again.' },
      ]);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="chat-layout">
      <Sidebar
        sessions={sessions}
        activeSessionId={activeSessionId}
        onSelect={handleSelectSession}
        onNewChat={handleNewChat}
        user={user}
        onLogout={handleLogout}
      />

      <main className="chat-main">
        <div className="message-area">
          {messages.length === 0 && !loadingMessages && (
            <div className="empty-state">
              <span className="pulse-dot large" />
              <h2>Start a conversation</h2>
              <p>Ask anything — your history is saved automatically.</p>
            </div>
          )}

          {loadingMessages && <div className="loading-message">Loading conversation…</div>}

          {messages.map((m) => (
            <MessageBubble key={m.id} role={m.role} content={m.content} />
          ))}

          {sending && (
            <div className="message-row from-assistant">
              <div className="message-avatar">AI</div>
              <div className="message-bubble typing">Thinking…</div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        <form onSubmit={handleSubmit} className="composer">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Message the assistant…"
            disabled={sending}
          />
          <button type="submit" disabled={!input.trim() || sending}>
            Send
          </button>
        </form>
      </main>
    </div>
  );
}
