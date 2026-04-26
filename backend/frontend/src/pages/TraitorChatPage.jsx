import React from 'react';
import { useParams } from 'react-router-dom';
import ChatWindow from '../components/ChatWindow';

function TraitorChatPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '0.4rem 1rem', background: '#3a1a1a', color: '#EF9A9A', fontWeight: 'bold', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        {onClose && (
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#EF9A9A', cursor: 'pointer', fontSize: '1rem', padding: 0 }}>← Back</button>
        )}
        Traitor Chat
      </div>
      <div style={{ flex: 1 }}>
        <ChatWindow gameCode={gameCode} playerCode={playerCode} channel="traitors" />
      </div>
    </div>
  );
}

export default TraitorChatPage;
