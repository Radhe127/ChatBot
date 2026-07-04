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
  const [profileOpen, setProfileOpen] = useState(false);
  const [attachments, setAttachments] = useState([]);
  const bottomRef = useRef(null);
  const fileInputRef = useRef(null);

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
    setAttachments([]);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleOpenProfile = () => {
    setProfileOpen(true);
  };

  const handleCloseProfile = () => {
    setProfileOpen(false);
  };

  const handleOpenFilePicker = () => {
    fileInputRef.current?.click();
  };

  const handleAttachmentChange = (event) => {
    const selectedFiles = Array.from(event.target.files || []);
    setAttachments((prev) => [...prev, ...selectedFiles].slice(0, 5));
    event.target.value = '';
  };

  const handleRemoveAttachment = (fileName) => {
    setAttachments((prev) => prev.filter((file) => file.name !== fileName));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if ((!text && attachments.length === 0) || sending) return;

    setInput('');
    const optimisticText = text || 'Uploaded attachments';
    setMessages((prev) => [...prev, { id: `temp-${Date.now()}`, role: 'user', content: optimisticText }]);
    setSending(true);

    try {
      const { data } = await chatApi.sendMessage(activeSessionId, text || optimisticText, attachments);
      setMessages((prev) => {
        const withoutTemp = prev.filter((m) => !String(m.id).startsWith('temp-'));
        return [...withoutTemp, data.userMessage, data.assistantMessage];
      });
      setAttachments([]);
      if (!activeSessionId) {
        setActiveSessionId(data.sessionId);
        await loadSessions();
      }
      } catch {
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
        onOpenProfile={handleOpenProfile}
      />

      <main className="chat-main">
        <div className="message-area">
          {messages.length === 0 && !loadingMessages && (
            <div className="empty-state">
              <span className="pulse-dot large" />
              <h2>Start a conversation</h2>
              <p>Ask anything, or attach an image or PDF and get a calm, useful response.</p>
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
            ref={fileInputRef}
            type="file"
            accept="image/*,.pdf"
            multiple
            onChange={handleAttachmentChange}
            className="composer-file-input"
          />
          {attachments.length > 0 && (
            <div className="attachment-list">
              {attachments.map((file) => (
                <button type="button" key={file.name} className="attachment-chip" onClick={() => handleRemoveAttachment(file.name)}>
                  <span>{file.type.startsWith('image/') ? 'Image' : 'PDF'}</span>
                  <strong>{file.name}</strong>
                  <span>×</span>
                </button>
              ))}
            </div>
          )}
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Message the assistant or attach a resume, image, or PDF…"
            disabled={sending}
          />
          <button type="button" className="attach-btn" onClick={handleOpenFilePicker} disabled={sending}>
            Attach
          </button>
          <button type="submit" disabled={(!input.trim() && attachments.length === 0) || sending}>
            Send
          </button>
        </form>
      </main>

      {profileOpen && (
        <div className="profile-overlay" onClick={handleCloseProfile} role="presentation">
          <div className="profile-card" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label="Profile details">
            <div className="profile-card-header">
              <div>
                <div className="profile-kicker">Your profile</div>
                <h2>{user?.name}</h2>
              </div>
              <button type="button" className="profile-close" onClick={handleCloseProfile} aria-label="Close profile">
                ×
              </button>
            </div>

            <div className="profile-summary">
              <div className="profile-avatar large">{user?.name?.[0]?.toUpperCase() || '?'}</div>
              <div>
                <div className="profile-label">Email</div>
                <div className="profile-value">{user?.email}</div>
                <div className="profile-label profile-spaced">User ID</div>
                <div className="profile-value profile-mono">{user?.userId}</div>
              </div>
            </div>

            <div className="profile-note">
              This profile is loaded from the signed-in account stored on this device.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
