import React, { useRef } from 'react';
import { useNavigate } from 'react-router-dom';

function TestDashboardPage() {
  const navigate = useNavigate();
  const players = JSON.parse(localStorage.getItem('testPlayers') || '[]');
  const gameCode = localStorage.getItem('gameCode');
  const linkRefs = useRef([]);

  if (!players.length || !gameCode) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>No test game found. <button onClick={() => navigate('/')}>Go back</button></p>
      </div>
    );
  }

  const menuUrl = (playerName) =>
    `/menu/${gameCode}/${encodeURIComponent(playerName)}`;

  const handleOpenAll = () => {
    for (let i = 1; i < linkRefs.current.length; i++) {
      if (linkRefs.current[i]) linkRefs.current[i].click();
    }
    if (linkRefs.current[0]) linkRefs.current[0].click();
  };

  const handleOpenOne = (player, isFirst) => {
    if (isFirst) {
      window.location.href = menuUrl(player.name);
    } else {
      window.open(menuUrl(player.name), '_blank');
    }
  };

  return (
    <div style={{ padding: '1.5rem', maxWidth: '540px', margin: '0 auto' }}>
      <h1>Test Dashboard</h1>
      <p style={{ color: '#555', marginBottom: '0.5rem' }}>
        Game: <strong>{gameCode}</strong>
      </p>

      {/* Popup permission callout */}
      <div style={{
        background: '#FFF3CD',
        border: '1px solid #FFC107',
        borderRadius: '6px',
        padding: '0.75rem 1rem',
        marginBottom: '1.25rem',
        fontSize: '0.85rem',
      }}>
        <strong>⚠ Chrome blocks bulk popups by default.</strong><br />
        <span style={{ color: '#555' }}>
          For "Open All" to work: click the popup-blocked icon in the address bar → <em>Always allow pop-ups from localhost</em>.
          <br />Otherwise, use the individual <strong>Open</strong> buttons below (one click per player, always works).
        </span>
      </div>

      <button
        onClick={handleOpenAll}
        style={{
          width: '100%',
          padding: '0.75rem',
          background: '#4CAF50',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          cursor: 'pointer',
          fontWeight: 'bold',
          fontSize: '1rem',
          marginBottom: '1rem',
        }}
      >
        Open All Players (requires popup permission)
      </button>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
        {players.map((player, i) => (
          <div
            key={player.playerCode}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '0.6rem 1rem',
              background: '#f5f5f5',
              borderRadius: '6px',
              border: '1px solid #ddd',
            }}
          >
            {/* Hidden anchor used by "Open All" */}
            <a
              ref={(el) => (linkRefs.current[i] = el)}
              href={menuUrl(player.name)}
              target={i === 0 ? '_self' : '_blank'}
              rel="noopener noreferrer"
              style={{ display: 'none' }}
            />

            <span>
              <strong>{player.name}</strong>{' '}
              <span style={{ color: '#888', fontSize: '0.8rem' }}>{player.email}</span>
            </span>

            <button
              onClick={() => handleOpenOne(player, i === 0)}
              style={{
                padding: '0.3rem 0.75rem',
                background: '#2196F3',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '0.85rem',
                whiteSpace: 'nowrap',
              }}
            >
              {i === 0 ? 'Open (this tab)' : 'Open →'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default TestDashboardPage;
