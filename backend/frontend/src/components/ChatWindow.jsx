import React, { useState, useEffect, useCallback, useRef } from 'react';
import { getChatMessages, sendChatMessage } from '../services/api';

function ChatWindow({ gameCode, playerCode, channel, recipientId, cardImageUrl, onReady, readyDone, onMessageSent }) {
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  // Use a ref for lastTimestamp so loadMessages stays stable (no interval churn)
  const lastTimestampRef = useRef(null);
  const inputRef = useRef(null);

  const loadMessages = useCallback(async () => {
    try {
      const result = await getChatMessages(gameCode, playerCode, channel, lastTimestampRef.current);
      if (result.messages && result.messages.length > 0) {
        setMessages(prev => {
          const existingIds = new Set(prev.map(m => m.messageId));
          const newMsgs = result.messages.filter(m => !existingIds.has(m.messageId));
          if (newMsgs.length === 0) return prev;
          const next = [...prev, ...newMsgs];
          lastTimestampRef.current = next[next.length - 1].timestamp;
          return next;
        });
      }
    } catch (err) {
      // Silently ignore polling errors
    }
  }, [gameCode, playerCode, channel]);

  useEffect(() => {
    loadMessages();
    const interval = setInterval(loadMessages, 2000);
    return () => clearInterval(interval);
  }, [loadMessages]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    setError('');
    try {
      await sendChatMessage(gameCode, playerCode, channel, text.trim(), recipientId);
      setText('');
      await loadMessages();
      if (onMessageSent && recipientId) onMessageSent(recipientId);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      backgroundImage: cardImageUrl ? `url(${cardImageUrl})` : 'none',
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      background: cardImageUrl ? undefined : '#1a1a1a',
    }}>
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '0.5rem',
        background: cardImageUrl ? 'rgba(0,0,0,0.65)' : 'transparent',
      }}>
        {messages.map((msg, i) => (
          <div key={msg.messageId || i} style={{
            marginBottom: '0.25rem',
            padding: '0.25rem 0.5rem',
            background: 'rgba(255,255,255,0.08)',
            borderRadius: '4px',
            fontSize: '0.9rem',
            color: '#e0e0e0',
          }}>
            <strong>{msg.senderName}:</strong> {msg.text}
          </div>
        ))}
        {messages.length === 0 && <p style={{ color: '#999', textAlign: 'center' }}>No messages yet</p>}
      </div>

      <form onSubmit={handleSend} style={{
        display: 'flex',
        alignItems: 'center',
        padding: '0.5rem',
        borderTop: '1px solid #333',
        background: '#111',
        gap: '0.4rem',
      }}>
        {onReady && (
          <button
            type="button"
            onClick={onReady}
            disabled={readyDone}
            style={{
              padding: '0.45rem 0.9rem',
              background: readyDone ? '#2e7d32' : '#43a047',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              fontSize: '0.85rem',
              fontWeight: 700,
              cursor: readyDone ? 'default' : 'pointer',
              whiteSpace: 'nowrap',
              opacity: readyDone ? 0.8 : 1,
            }}
          >
            {readyDone ? '✓ Ready' : 'Ready'}
          </button>
        )}
        <input
          ref={inputRef}
          type="text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Type a message..."
          maxLength={500}
          style={{ flex: 1, padding: '0.5rem', background: '#2a2a2a', color: '#e0e0e0', border: '1px solid #444', borderRadius: '4px' }}
        />
        <button type="submit" style={{ padding: '0.5rem 1rem', background: '#1565C0', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Send</button>
      </form>

      {error && <p style={{ color: 'red', padding: '0.25rem 0.5rem', fontSize: '0.8rem' }}>{error}</p>}
    </div>
  );
}

export default ChatWindow;
