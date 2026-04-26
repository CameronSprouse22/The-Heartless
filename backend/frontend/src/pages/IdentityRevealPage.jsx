import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getIdentityReveal } from '../services/api';

// Inject keyframe animations once
const STYLE_ID = 'identity-reveal-styles';
const KEYFRAMES = `
  @keyframes ir-pulse {
    0%, 100% { opacity: 0.5; transform: scale(1); }
    50% { opacity: 0.8; transform: scale(1.04); }
  }
  @keyframes ir-drop {
    0% { opacity: 0; transform: scale(0.6) translateY(-20px); }
    60% { transform: scale(1.08) translateY(2px); }
    100% { opacity: 1; transform: scale(1) translateY(0); }
  }
  @keyframes ir-glow-traitor {
    0%, 100% { box-shadow: 0 0 10px 2px rgba(220,50,50,0.4); }
    50% { box-shadow: 0 0 22px 6px rgba(220,50,50,0.8); }
  }
  @keyframes ir-glow-faithful {
    0%, 100% { box-shadow: 0 0 10px 2px rgba(50,120,220,0.4); }
    50% { box-shadow: 0 0 22px 6px rgba(50,120,220,0.8); }
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

function PendingCard() {
  return (
    <div style={{
      width: '100%',
      aspectRatio: '2/3',
      borderRadius: '10px',
      background: '#1a1a2e',
      border: '1px solid #333',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      animation: 'ir-pulse 2s ease-in-out infinite',
      cursor: 'default',
    }}>
      <span style={{ fontSize: '2rem', color: '#555', userSelect: 'none' }}>?</span>
    </div>
  );
}

function RevealedCard({ player, isNew }) {
  const isTraitor = player.isTraitor;
  const isDead = player.isDead;

  const bg = isTraitor
    ? 'linear-gradient(160deg, #5c0a0a 0%, #8b0000 100%)'
    : 'linear-gradient(160deg, #0a2a5c 0%, #0d47a1 100%)';

  const glowAnim = isTraitor ? 'ir-glow-traitor 2.5s ease-in-out infinite' : 'ir-glow-faithful 2.5s ease-in-out infinite';

  return (
    <div style={{
      width: '100%',
      aspectRatio: '2/3',
      borderRadius: '10px',
      background: bg,
      border: `2px solid ${isTraitor ? '#cc3333' : '#2255cc'}`,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0.6rem 0.4rem',
      animation: isNew
        ? 'ir-drop 0.6s cubic-bezier(0.34,1.56,0.64,1) forwards, ' + glowAnim
        : glowAnim,
      cursor: 'default',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Role icon */}
      <div style={{ fontSize: '2rem', marginTop: '0.2rem' }}>
        {isTraitor ? '🗡️' : '⚔️'}
      </div>

      {/* Role label */}
      <div style={{
        fontSize: '0.8rem',
        fontWeight: 'bold',
        letterSpacing: '0.15em',
        color: isTraitor ? '#ff8a8a' : '#82b1ff',
        textAlign: 'center',
        textTransform: 'uppercase',
      }}>
        {isTraitor ? 'Traitor' : 'Faithful'}
      </div>

      {/* Player name */}
      <div style={{
        fontSize: '0.75rem',
        color: '#ddd',
        textAlign: 'center',
        wordBreak: 'break-word',
        lineHeight: '1.2',
        padding: '0 0.2rem',
      }}>
        {player.playerName}
        {isDead && <span style={{ display: 'block', fontSize: '0.65rem', color: '#888', marginTop: '2px' }}>☠️ Dead</span>}
      </div>
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
            height: '6px',
            background: '#222',
            borderRadius: '3px',
            overflow: 'hidden',
          }}>
            <div style={{
              height: '100%',
              width: totalPlayers > 0 ? `${(revealedPlayers.length / totalPlayers) * 100}%` : '0%',
              background: 'linear-gradient(90deg, #0d47a1, #8b0000)',
              transition: 'width 0.8s ease',
              borderRadius: '3px',
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
            <span style={{ color: '#ff8a8a' }}>🗡️ {traitorCount} Traitor{traitorCount !== 1 ? 's' : ''}</span>
            <span style={{ color: '#82b1ff' }}>⚔️ {faithfulCount} Faithful</span>
          </div>
        )}
      </div>

      {/* Card grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gap: '0.6rem',
        flex: 1,
      }}>
        {revealedPlayers.map((player, idx) => (
          <RevealedCard
            key={player.playerId || idx}
            player={player}
            isNew={newIndicesRef.current.has(idx)}
          />
        ))}
        {Array.from({ length: pendingCount }).map((_, i) => (
          <PendingCard key={`pending-${i}`} />
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
