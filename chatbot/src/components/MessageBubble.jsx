export default function MessageBubble({ role, content }) {
  const isUser = role === 'user';
  return (
    <div className={`message-row ${isUser ? 'from-user' : 'from-assistant'}`}>
      <div className="message-avatar">{isUser ? 'You' : 'AI'}</div>
      <div className="message-bubble">{content}</div>
    </div>
  );
}
