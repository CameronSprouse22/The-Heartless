import React from 'react';
import { useParams } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';

function ActionsPage() {
  const { gameCode } = useParams();
  const playerCode = localStorage.getItem('playerCode');

  return (
    <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center' }}>
      <GameStatusBar gameCode={gameCode} playerCode={playerCode} />
      <h2>Actions</h2>
      <p style={{ color: '#888', marginTop: '2rem' }}>Coming Soon</p>
    </div>
  );
}

export default ActionsPage;
