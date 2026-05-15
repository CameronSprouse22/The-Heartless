import React from 'react';
import { useNavigate } from 'react-router-dom';

function formatTime(ms) {
  if (!ms || ms <= 0) return '0:00';
  const totalSec = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSec / 60);
  const seconds = totalSec % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function GameStatusBar({ gameStatus, round, playerName, playersRemaining, eventType, timeLeftMs, backPath, tabs, activeTab, onTabChange }) {
  const navigate = useNavigate();

  return (
    <div style={{
      background: '#111',
      color: 'white',
      padding: '0.35rem 0.75rem',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: '0.85rem',
      borderBottom: '1px solid #333',
      flexShrink: 0,
      gap: '0.5rem',
    }}>
      {/* Left: tabs OR back button OR player name */}
      <div style={{ display: 'flex', gap: '4px', alignItems: 'center', flexShrink: 0 }}>
        {tabs ? (
          tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => onTabChange && onTabChange(tab.id)}
              style={{
                padding: '0.2rem 0.55rem',
                background: activeTab === tab.id ? tab.color : `${tab.color}55`,
                color: 'white',
                border: `1.5px solid ${tab.color}`,
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '0.72rem',
                fontWeight: activeTab === tab.id ? 700 : 400,
                opacity: activeTab === tab.id ? 1 : 0.75,
                letterSpacing: '0.02em',
                transition: 'background 0.12s, opacity 0.12s',
              }}
            >{tab.label}</button>
          ))
        ) : backPath ? (
          <button
            onClick={() => navigate(backPath)}
            style={{ background: 'transparent', color: 'white', border: 'none', cursor: 'pointer', fontSize: '0.9rem' }}
          >← Menu</button>
        ) : (
          <span style={{ fontWeight: 'bold' }}>{playerName || 'Player'}</span>
        )}
      </div>

      {/* Centre: event name + countdown */}
      <div style={{ textAlign: 'center', flex: 1 }}>
        {eventType && <div style={{ fontWeight: 'bold', fontSize: '0.9rem' }}>{eventType}</div>}
        {timeLeftMs > 0 && (
          <div style={{
            fontFamily: 'monospace',
            fontSize: '0.85rem',
            color: timeLeftMs < 60000 ? '#FF5252' : '#A5D6A7',
          }}>⏱ {formatTime(timeLeftMs)}</div>
        )}
        {gameStatus && !eventType && <div>{gameStatus}</div>}
      </div>

      {/* Right: round + players */}
      <div style={{ textAlign: 'right', minWidth: '80px' }}>
        <div>Round: {round >= 0 ? round : '-'}</div>
        {playersRemaining != null && <div>Players: {playersRemaining}</div>}
      </div>
    </div>
  );
}

export default GameStatusBar;
