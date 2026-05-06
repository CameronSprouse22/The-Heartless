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
  @keyframes ir-flip {
    0%   { transform: perspective(400px) rotateY(90deg); opacity: 0; }
    60%  { transform: perspective(400px) rotateY(8deg);  opacity: 1; }
    100% { transform: perspective(400px) rotateY(0deg);  opacity: 1; }
  }
`;

function injectStyles() {
  let el = document.getElementById(STYLE_ID);
  if (!el) {
    el = document.createElement('style');
    el.id = STYLE_ID;
    document.head.appendChild(el);
  }
  el.textContent = KEYFRAMES;
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

function RevealedRow({ player, isNew, isLast }) {
  // During shuffle phase, display the opposite role to start, then alternate
  const [displayTraitor, setDisplayTraitor] = useState(!player.isTraitor);
  const [flipKey, setFlipKey]               = useState(0);
  const [flipDuration, setFlipDuration]     = useState(1000);
  const [shuffling, setShuffling]           = useState(isNew);

  useEffect(() => {
    if (!isNew) return;

    // Faithful: 5–8 flips, Traitor: 7–10 flips, random durations trending slower, capped at 1400ms
    const [minFlips, maxFlips] = player.isTraitor ? [7, 10] : [5, 8];
    const TOTAL_FLIPS = minFlips + Math.floor(Math.random() * (maxFlips - minFlips + 1));
    const MAX_MS = 1400;
    const durations = Array.from({ length: TOTAL_FLIPS }, (_, i) => {
      const progress = i / (TOTAL_FLIPS - 1);          // 0 → 1
      const minMs = 60  + Math.round(progress * 400);  // 60ms → 460ms
      const maxMs = Math.min(150 + Math.round(progress * 900), MAX_MS); // 150ms → 1050ms (capped)
      return minMs + Math.round(Math.random() * (maxMs - minMs));
    });

    let flips = 0;
    let timeoutId;
    let currentRole = !player.isTraitor;

    function doFlip() {
      currentRole = !currentRole;
      flips++;
      const isLast = flips >= TOTAL_FLIPS;
      setFlipDuration(durations[flips - 1]);
      setDisplayTraitor(isLast ? player.isTraitor : currentRole);
      setFlipKey(k => k + 1);
      if (isLast) {
        setShuffling(false);
      } else {
        timeoutId = setTimeout(doFlip, durations[flips]);
      }
    }

    timeoutId = setTimeout(doFlip, durations[0]);
    return () => clearTimeout(timeoutId);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const showTraitor  = shuffling ? displayTraitor : player.isTraitor;
  const roleColor    = showTraitor ? '#ff6b6b' : '#82b1ff';
  const roleLabel    = showTraitor ? 'TRAITOR' : 'FAITHFUL';
  const roleIcon     = showTraitor ? '🗡️' : '🛡️';
  const borderColor  = showTraitor ? 'rgba(200,50,50,0.5)' : 'rgba(50,100,200,0.4)';

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      padding: '0.65rem 1rem',
      borderBottom: isLast ? 'none' : `1px solid ${borderColor}`,
      animation: (!shuffling && isNew) ? 'ir-line-in 0.45s cubic-bezier(0.22,1,0.36,1) forwards' : 'none',
      background: showTraitor
        ? 'linear-gradient(90deg, rgba(90,10,10,0.35) 0%, transparent 80%)'
        : 'linear-gradient(90deg, rgba(10,30,80,0.35) 0%, transparent 80%)',
      transition: 'background 0.15s ease, border-color 0.15s ease',
    }}>
      {/* Player name */}
      <span style={{
        flex: 1,
        fontSize: '1rem',
        color: '#e0e0e0',
        fontWeight: '500',
        letterSpacing: '0.04em',
        padding: '0.1rem 0.3rem',
      }}>
        {player.playerName}
      </span>

      {/* Role badge — hidden while shuffling, fades in on final reveal */}
      <span
        key={flipKey}
        style={{
          fontSize: '0.8rem',
          fontWeight: 'bold',
          color: roleColor,
          textTransform: 'uppercase',
          letterSpacing: '0.15em',
          marginLeft: '1rem',
          whiteSpace: 'nowrap',
          display: 'inline-block',
          visibility: shuffling ? 'hidden' : 'visible',
          animationName: !shuffling && isNew ? 'ir-role-fade' : 'none',
          animationDuration: '0.3s',
          animationFillMode: 'both',
          animationTimingFunction: 'ease',
        }}
      >
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
        </div>

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
            isLast={idx === revealedPlayers.length - 1 && pendingCount === 0}
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
