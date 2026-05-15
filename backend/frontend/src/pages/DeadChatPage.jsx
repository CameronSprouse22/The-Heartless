import React from 'react';
import { useParams } from 'react-router-dom';
import ChatWindow from '../components/ChatWindow';

function DeadChatPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '0.4rem 1rem', background: '#1f2a2a', color: '#80CBC4', fontWeight: 'bold', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        Dead Players Chat
      </div>
      <div style={{ flex: 1 }}>
        <ChatWindow gameCode={gameCode} playerCode={playerCode} channel="dead" />
      </div>
    </div>
  );
}

export default DeadChatPage;
