import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ChatWindow from '../components/ChatWindow';

function TraitorChatPage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const playerName = localStorage.getItem('playerName') || '';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div style={{ padding: '0.5rem', background: '#8b0000', color: 'white', display: 'flex', justifyContent: 'space-between' }}>
        <button onClick={() => navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)} style={{ background: 'transparent', color: 'white', border: 'none', cursor: 'pointer' }}>← Back</button>
        <span>Traitor Chat</span>
        <span></span>
      </div>
      <div style={{ flex: 1 }}>
        <ChatWindow gameCode={gameCode} playerCode={playerCode} channel="traitors" />
      </div>
    </div>
  );
}

export default TraitorChatPage;
