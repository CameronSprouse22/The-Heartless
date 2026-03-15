import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';
import ChatWindow from '../components/ChatWindow';

function DeadChatPage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const playerCode = localStorage.getItem('playerCode');
  const playerName = localStorage.getItem('playerName') || '';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div style={{ padding: '0.5rem', background: '#607D8B', color: 'white', display: 'flex', justifyContent: 'space-between' }}>
        <button onClick={() => navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)} style={{ background: 'transparent', color: 'white', border: 'none', cursor: 'pointer' }}>← Back</button>
        <span>Dead Players Chat</span>
        <span></span>
      </div>
      <div style={{ flex: 1 }}>
        <ChatWindow gameCode={gameCode} playerCode={playerCode} channel="dead" />
      </div>
    </div>
  );
}

export default DeadChatPage;
