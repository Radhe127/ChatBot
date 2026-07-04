export default function Sidebar({ sessions, activeSessionId, onSelect, onNewChat, user, onLogout, onOpenProfile }) {
  return (
    <aside className="sidebar">
      <div className="brand-mark">
        <span className="pulse-dot" />
        <span>Assistant</span>
      </div>

      <button className="new-chat-btn" onClick={onNewChat}>
        + New chat
      </button>

      <div className="session-list">
        {sessions.length === 0 && (
          <p className="empty-hint">No conversations yet.</p>
        )}
        {sessions.map((s) => (
          <button
            key={s.id}
            className={`session-item ${s.id === activeSessionId ? 'active' : ''}`}
            onClick={() => onSelect(s.id)}
          >
            {s.title}
          </button>
        ))}
      </div>

      <div className="sidebar-footer">
        <button type="button" className="user-chip" onClick={onOpenProfile}>
          <div className="avatar">{user?.name?.[0]?.toUpperCase() || '?'}</div>
          <div className="user-meta">
            <span className="user-name">{user?.name}</span>
            <span className="user-email">{user?.email}</span>
          </div>
        </button>
        <button className="logout-btn" onClick={onLogout}>Log out</button>
      </div>
    </aside>
  );
}
