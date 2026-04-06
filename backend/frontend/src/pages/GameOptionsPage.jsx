import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';

export default function GameOptionsPage() {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();

  const buttonStyle = {
    width: '100%',
    padding: '0.75rem',
    marginBottom: '0.5rem',
    fontSize: '1rem',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    background: '#4CAF50',
    color: 'white',
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* Header */}
      <div style={{
        padding: '0.75rem 1rem',
        background: '#37474f',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
      }}>
        <button
          onClick={() => navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)}
          style={{ background: 'transparent', color: 'white', border: 'none', cursor: 'pointer', fontSize: '1.1rem' }}
        >
          ← Back
        </button>
        <span style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>Game Options</span>
      </div>

      <div style={{ padding: '1.25rem', maxWidth: '420px', margin: '0 auto', width: '100%' }}>
        <button
          onClick={() => navigate(`/notifications/${gameCode}/${encodeURIComponent(playerName)}`)}
          style={buttonStyle}
        >
          🔔 Notifications
        </button>
      </div>
    </div>
  );
}
