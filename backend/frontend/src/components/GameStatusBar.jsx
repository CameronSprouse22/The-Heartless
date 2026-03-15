import React from 'react';

function GameStatusBar({ gameStatus, round, currentTask, playerName }) {
  return (
    <div style={{
      background: '#333',
      color: 'white',
      padding: '0.5rem 1rem',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: '0.9rem',
    }}>
      <span>{playerName || 'Player'}</span>
      <span>Status: {gameStatus || 'Unknown'}</span>
      <span>Round: {round >= 0 ? round : '-'}</span>
      {currentTask && <span>Task: {currentTask}</span>}
    </div>
  );
}

export default GameStatusBar;
