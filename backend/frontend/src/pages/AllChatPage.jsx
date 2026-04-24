import React from 'react';
import { useParams } from 'react-router-dom';
import ChatWindow from '../components/ChatWindow';

function AllChatPage() {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '0.4rem 1rem', background: '#1e2a38', color: '#90CAF9', fontWeight: 'bold', fontSize: '0.85rem' }}>All Chat</div>
      <div style={{ flex: 1 }}>
        <ChatWindow gameCode={gameCode} playerCode={playerCode} channel="all" />
      </div>
    </div>
  );
}

export default AllChatPage;
