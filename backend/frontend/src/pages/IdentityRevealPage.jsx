import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getIdentityReveal } from '../services/api';

// Inject keyframe animations once
const STYLE_ID = 'identity-reveal-styles';
const KEYFRAMES = `
  @keyframes ir-line-in {
    0%   { opacity: 0; transform: translateX(-24px); }
    100% { opacity: 1; transform: translateX(0); }
  }
  @keyframes ir-role-fade {
    0%   { opacity: 0; letter-spacing: 0.4em; }
    100% { opacity: 1; letter-spacing: 0.15em; }
  }
  @keyframes ir-pending-pulse {
    0%, 100% { opacity: 0.3; }
    50%       { opacity: 0.6; }
  }
`;

function injectStyles() {
  if (!document.getElementById(STYLE_ID)) {
    const el = document.createElement('style');
    el.id = STYLE_ID;
    el.textContent = KEYFRAMES;
    document.head.appendChild(el);
  }
}

function PendingRow() {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      padding: '0.65rem 1rem',
      borderBottom: '1px solid #1e1e2e',
      animation: 'ir-pending-pulse 2s ease-in-out infinite',
    }}>
      <div style={{
        flex: 1,
        height: '12px',
        borderRadius: '4px',
        background: '#2a2a3e',
        outline: '1px solid #333',
      }} />
      <div style={{
        width: '80px',
        height: '12px',
        borderRadius: '4px',
        background: '#2a2a3e',
        marginLeft: '1rem',
        outline: '1px solid #333',
      }} />
    </div>
  );
}

function RevealedRow({ player, isNew }) {
  const isTraitor = player.isTraitor;
  const roleColor = isTraitor ? '#ff6b6b' : '#82b1ff';
  const roleLabel = isTraitor ? 'TRAITOR' : 'FAITHFUL';
  const roleIcon  = isTraitor ? '🗡️' : '🛡️';
  const borderColor = isTraitor ? 'rgba(200,50,50,0.5)' : 'rgba(50,100,200,0.4)';

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      padding: '0.65rem 1rem',
      borderBottom: `1px solid ${borderColor}`,
      animation: isNew ? 'ir-line-in 0.45s cubic-bezier(0.22,1,0.36,1) forwards' : 'none',
      background: isNew
        ? isTraitor
          ? 'linear-gradient(90deg, rgba(90,10,10,0.35) 0%, transparent 80%)'
          : 'linear-gradient(90deg, rgba(10,30,80,0.35) 0%, transparent 80%)'
        : 'transparent',
    }}>
      {/* Player name */}
      <span style={{
        flex: 1,
        fontSize: '1rem',
        color: '#e0e0e0',
        fontWeight: '500',
        letterSpacing: '0.04em',
        outline: isNew ? `1px solid ${borderColor}` : 'none',
        borderRadius: '3px',
        padding: '0.1rem 0.3rem',
        transition: 'outline 0.6s ease',
      }}>
        {player.playerName}
      </span>

      {/* Role badge — fades in with a slight delay */}
      <span style={{
        fontSize: '0.8rem',
        fontWeight: 'bold',
        color: roleColor,
        textTransform: 'uppercase',
        letterSpacing: '0.15em',
        marginLeft: '1rem',
        animation: isNew ? 'ir-role-fade 0.7s ease 0.35s both' : 'none',
        whiteSpace: 'nowrap',
      }}>
        {roleIcon} {roleLabel}
      </span>
    </div>
  );
}

function IdentityRevealPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  const [revealedPlayers, setRevealedPlayers] = useState([]);
  const [totalPlayers, setTotalPlayers] = useState(0);
  const [revealComplete, setRevealComplete] = useState(false);
  const [error, setError] = useState('');
  const prevCountRef = useRef(0);
  const newIndicesRef = useRef(new Set());

  useEffect(() => {
    injectStyles();
  }, []);

  useEffect(() => {
    if (!playerCode) return;

    const poll = async () => {
      try {
        const data = await getIdentityReveal(gameCode, playerCode);
        const players = data.revealedPlayers || [];
        const prevCount = prevCountRef.current;
        if (players.length > prevCount) {
          const newSet = new Set();
          for (let i = prevCount; i < players.length; i++) newSet.add(i);
          newIndicesRef.current = newSet;
          prevCountRef.current = players.length;
        }
        setRevealedPlayers(players);
        setTotalPlayers(data.totalPlayers || 0);
        setRevealComplete(data.revealComplete || false);
      } catch (err) {
        setError(err.message || 'Failed to load identity reveal');
      }
    };

    poll();
    const interval = setInterval(poll, 1500);
    return () => clearInterval(interval);
  }, [gameCode, playerCode]);

  const pendingCount = Math.max(0, totalPlayers - revealedPlayers.length);
  const traitorCount = revealedPlayers.filter(p => p.isTraitor).length;
  const faithfulCount = revealedPlayers.filter(p => !p.isTraitor).length;

  if (error) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#f88' }}>
        {error}
        {onClose && (
          <button onClick={onClose} style={closeButtonStyle}>
            ← Back to Menu
          </button>
        )}
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100%',
      background: '#0a0a14',
      color: 'white',
      padding: '1rem',
      display: 'flex',
      flexDirection: 'column',
    }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: '1rem' }}>
        <h2 style={{
          fontSize: '1.4rem',
          letterSpacing: '0.2em',
          textTransform: 'uppercase',
          color: '#ccc',
          margin: 0,
        }}>
          👁️ Identity Reveal
        </h2>

        {/* Progress bar */}
        <div style={{ margin: '0.6rem auto', maxWidth: '280px' }}>
          <div style={{
            height: '4px',
            background: '#222',
            borderRadius: '2px',
            overflow: 'hidden',
          }}>
            <div style={{
              height: '100%',
              width: totalPlayers > 0 ? `${(revealedPlayers.length / totalPlayers) * 100}%` : '0%',
              background: 'linear-gradient(90deg, #0d47a1, #8b0000)',
              transition: 'width 0.8s ease',
              borderRadius: '2px',
            }} />
          </div>
          <p style={{ fontSize: '0.8rem', color: '#888', marginTop: '0.3rem' }}>
            {revealedPlayers.length} of {totalPlayers} revealed
            {revealComplete && <span style={{ color: '#aaa', marginLeft: '0.5rem' }}>· Complete</span>}
          </p>
        </div>

        {/* Tally */}
        {revealedPlayers.length > 0 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '1.5rem', fontSize: '0.85rem' }}>
            <span style={{ color: '#ff6b6b' }}>🗡️ {traitorCount} Traitor{traitorCount !== 1 ? 's' : ''}</span>
            <span style={{ color: '#82b1ff' }}>🛡️ {faithfulCount} Faithful</span>
          </div>
        )}
      </div>

      {/* Player list */}
      <div style={{
        flex: 1,
        border: '1px solid #1e1e2e',
        borderRadius: '8px',
        overflow: 'hidden',
      }}>
        {revealedPlayers.map((player, idx) => (
          <RevealedRow
            key={player.playerId || idx}
            player={player}
            isNew={newIndicesRef.current.has(idx)}
          />
        ))}
        {Array.from({ length: pendingCount }).map((_, i) => (
          <PendingRow key={`pending-${i}`} />
        ))}
      </div>

      {/* Close button */}
      {onClose && (
        <button onClick={onClose} style={{ ...closeButtonStyle, marginTop: '1.5rem' }}>
          ← Back to Menu
        </button>
      )}
    </div>
  );
}

const closeButtonStyle = {
  display: 'block',
  width: '100%',
  padding: '0.75rem',
  background: '#263238',
  color: '#ccc',
  border: '1px solid #444',
  borderRadius: '6px',
  fontSize: '0.95rem',
  cursor: 'pointer',
};

export default IdentityRevealPage;
