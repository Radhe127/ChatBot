import { useState } from 'react';
import './App.css';

function App() {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userMessage = { role: 'user', content: input };
    setMessages(prev => [...prev, userMessage]);

    const messageToSend = input;
    setInput('');
    setLoading(true);

    try {
      const encodeMessage = encodeURIComponent(messageToSend);
      const response = await fetch(`/api/${encodeMessage}`);
      const botResponseText = await response.text();

      setMessages(prev => [
        ...prev,
        { role: 'assistant', content: botResponseText }
      ]);
    } catch (error) {
      console.error('API error', error);
      setMessages(prev => [
        ...prev,
        { role: 'assistant', content: 'API is not responding!!' }
      ]);
    }

    setLoading(false);
  };

  return (
    <div className="message-container">
      <h1>Assistance...</h1>

      <div className="message-area">
        {messages.map((msg, index) => (
          <div key={index} className={msg.role}>
            <strong>
              {msg.role === 'user' ? 'You: ' : 'Bot: '}
            </strong>
            {msg.content}
          </div>
        ))}

        {loading && (
          <div className="loading-message">Processing...</div>
        )}
      </div>

      <form onSubmit={handleSubmit} className="form-area">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type your text..."
          disabled={loading}
        />
        <button
          type="submit"
          disabled={!input.trim() || loading}
        >
          Send
        </button>
      </form>
    </div>
  );
}

export default App;