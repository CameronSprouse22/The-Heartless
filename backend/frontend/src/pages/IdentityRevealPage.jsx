import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getIdentityReveal } from '../services/api';

// Inject keyframe animations once
const STYLE_ID = 'identity-reveal-styles';
const KEYFRAMES = `
  @keyframes ir-fade-in {
    0%   { opacity: 0; transform: translateY(6px); }
    100% { opacity: 1; transform: translateY(0); }
  }
  @keyframes ir-pending-pulse {
    0%, 100% { opacity: 0.25; }
    50%       { opacity: 0.65; }
  }
  @keyframes ir-role-flip {
    0%   { transform: rotateY(90deg) scaleX(0.6); opacity: 0; }
    60%  { transform: rotateY(-8deg) scaleX(1.05); opacity: 1; }
    100% { transform: rotateY(0deg)  scaleX(1);    opacity: 1; }
  }
  @keyframes ir-role-fade-in {
    0%   { opacity: 0; }
    100% { opacity: 1; }
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

// --- Death type badge ---
function DeathBadge({ deathType }) {
  const map = {
    MURDERED:         { label: '\u2620\uFE0F Murdered',   color: '#ef9a9a', bg: 'rgba(183,28,28,0.22)',  border: 'rgba(183,28,28,0.35)' },
    BANISHED:         { label: '\u2696\uFE0F Banished',   color: '#ce93d8', bg: 'rgba(74,20,140,0.22)',  border: 'rgba(74,20,140,0.35)' },
    PLAYER_BOOTED:    { label: '\uD83D\uDEAA Booted',     color: '#bdbdbd', bg: 'rgba(50,50,50,0.28)',   border: 'rgba(80,80,80,0.45)' },
    PLAYER_LEFT_GAME: { label: '\uD83D\uDEAA Left Game',  color: '#bdbdbd', bg: 'rgba(50,50,50,0.28)',   border: 'rgba(80,80,80,0.45)' },
  };
  const d = map[deathType];
  if (!d) return null;
  return (
    <span style={{
      fontSize: '0.7rem',
      color: d.color,
      background: d.bg,
      border: `1px solid ${d.border}`,
      borderRadius: '4px',
      padding: '0.1rem 0.4rem',
      marginLeft: '0.5rem',
      whiteSpace: 'nowrap',
      verticalAlign: 'middle',
    }}>
      {d.label}
    </span>
  );
}

// --- Static player row (appears instantly, no animation) ---
function StaticPlayerRow({ player }) {
  const isTraitor   = player.isTraitor;
  const roleColor   = isTraitor ? '#ef9a9a' : '#90caf9';
  const roleLabel   = isTraitor ? 'TRAITOR' : 'FAITHFUL';
  const roleIcon    = isTraitor ? '\uD83D\uDDE1\uFE0F' : '\uD83D\uDEE1\uFE0F';
  const rowBg       = isTraitor
    ? 'linear-gradient(90deg, rgba(90,10,10,0.38) 0%, rgba(15,10,10,0.15) 100%)'
    : 'linear-gradient(90deg, rgba(10,30,80,0.38) 0%, rgba(10,10,20,0.15) 100%)';
  const borderColor = isTraitor ? 'rgba(180,40,40,0.28)' : 'rgba(40,80,160,0.28)';

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0.65rem 1rem',
      borderBottom: `1px solid ${borderColor}`,
      background: rowBg,
      animation: 'ir-fade-in 0.35s ease forwards',
    }}>
      <span style={{ flex: 1, fontSize: '1rem', color: '#e0e0e0', fontWeight: 500 }}>
        {player.playerName}
        <DeathBadge deathType={player.deathType} />
      </span>
      <span style={{
        fontSize: '0.78rem',
        fontWeight: 'bold',
        color: roleColor,
        textTransform: 'uppercase',
        letterSpacing: '0.12em',
        marginLeft: '1rem',
        whiteSpace: 'nowrap',
      }}>
        {roleIcon} {roleLabel}
      </span>
    </div>
  );
}

// --- Banish reveal: static name, role cycles for 10s then lands ---
function BanishReveal({ pickedEntry, onDone }) {
  const isBanishBlocked = !!pickedEntry.isBanishBlocked;
  const isTraitor       = pickedEntry.isTraitor;

  const CYCLE_MS = 10000;
  const frameRef = useRef(null);
  const [displayRole, setDisplayRole] = useState('TRAITOR');
  const [phase, setPhase]             = useState('cycling'); // cycling | landing | done

  useEffect(() => {
    if (isBanishBlocked) {
      // No role to reveal – just wait briefly then finish
      const t = setTimeout(() => { setPhase('done'); onDone && onDone(); }, 600);
      return () => clearTimeout(t);
    }

    let idx = 0;
    const roles = ['TRAITOR', 'FAITHFUL'];
    const startTime = Date.now();

    const tick = () => {
      const elapsed = Date.now() - startTime;
      if (elapsed < CYCLE_MS) {
        idx = (idx + 1) % 2;
        setDisplayRole(roles[idx]);
        // Slow down near the end for suspense
        const progress = elapsed / CYCLE_MS;
        const interval = progress < 0.7 ? 180 : 180 + ((progress - 0.7) / 0.3) * 520;
        frameRef.current = setTimeout(tick, interval);
      } else {
        setDisplayRole(isTraitor ? 'TRAITOR' : 'FAITHFUL');
        setPhase('landing');
        setTimeout(() => { setPhase('done'); onDone && onDone(); }, 800);
      }
    };

    frameRef.current = setTimeout(tick, 180);
    return () => clearTimeout(frameRef.current);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const borderColor = isBanishBlocked ? '#757575' : '#f44336';
  const nameColor   = isBanishBlocked ? '#bdbdbd' : '#f44336';
  const glow        = isBanishBlocked
    ? '0 0 14px 4px rgba(100,100,100,0.35)'
    : '0 0 24px 8px rgba(244,67,54,0.45), 0 0 4px 2px #ff1744';

  const finalRole  = isTraitor ? 'TRAITOR' : 'FAITHFUL';
  const roleColor  = displayRole === 'TRAITOR' ? '#ef9a9a' : '#90caf9';
  const roleIcon   = displayRole === 'TRAITOR' ? '\uD83D\uDDE1\uFE0F' : '\uD83D\uDEE1\uFE0F';
  const finalColor = finalRole === 'TRAITOR' ? '#ef9a9a' : '#90caf9';
  const finalIcon  = finalRole === 'TRAITOR' ? '\uD83D\uDDE1\uFE0F' : '\uD83D\uDEE1\uFE0F';

  return (
    <div style={{ textAlign: 'center', padding: '1.5rem 1rem' }}>
      <p style={{
        color: '#888',
        fontSize: '0.78rem',
        marginBottom: '1rem',
        letterSpacing: '0.1em',
        textTransform: 'uppercase',
      }}>
        {isBanishBlocked ? '\uD83D\uDEE1\uFE0F Banishment Blocked' : '\u2696\uFE0F Banished by vote'}
      </p>
      <div style={{
        display: 'inline-block',
        background: '#111',
        border: `2px solid ${borderColor}`,
        borderRadius: '14px',
        padding: '1.2rem 2.5rem',
        minWidth: '220px',
        boxShadow: glow,
      }}>
        <span style={{
          display: 'block',
          fontSize: '2rem',
          fontWeight: 'bold',
          color: nameColor,
          letterSpacing: '0.04em',
          animation: 'ir-fade-in 0.4s ease forwards',
        }}>
          {isBanishBlocked ? 'Banish Blocked' : pickedEntry.playerName}
        </span>
        {!isBanishBlocked && phase === 'cycling' && (
          <div style={{
            marginTop: '0.5rem',
            animation: 'ir-role-fade-in 3s ease forwards',
          }}>
            <span style={{
              display: 'inline-block',
              fontSize: '0.85rem',
              fontWeight: 'bold',
              color: roleColor,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              animation: 'ir-role-flip 0.18s ease forwards',
            }} key={displayRole}>
              {roleIcon} {displayRole}
            </span>
          </div>
        )}
        {!isBanishBlocked && (phase === 'landing' || phase === 'done') && (
          <span style={{
            display: 'inline-block',
            marginTop: '0.5rem',
            fontSize: '0.85rem',
            fontWeight: 'bold',
            color: finalColor,
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            animation: 'ir-role-flip 0.6s ease forwards',
          }}>
            {finalIcon} {finalRole}
          </span>
        )}
        {isBanishBlocked && (
          <span style={{
            display: 'block',
            marginTop: '0.5rem',
            fontSize: '0.82rem',
            color: '#9e9e9e',
            fontStyle: 'italic',
            animation: 'ir-fade-in 0.5s ease forwards',
          }}>
            A power blocked the banishment
          </span>
        )}
      </div>
    </div>
  );
}
function IdentityRevealPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  const [staticPlayers, setStaticPlayers] = useState([]);
  const [slotEntry, setSlotEntry]         = useState(null);
  const [slotDone, setSlotDone]           = useState(false);
  const [revealComplete, setRevealComplete] = useState(false);
  const [error, setError]                   = useState('');
  const slotSetRef = useRef(false);

  useEffect(() => { injectStyles(); }, []);

  useEffect(() => {
    if (!playerCode) return;
    const poll = async () => {
      try {
        const data = await getIdentityReveal(gameCode, playerCode);
        const players = data.revealedPlayers || [];
        setStaticPlayers(players.filter(p => !p.isNewlyBanished));
        const slot = players.find(p => p.isNewlyBanished);
        if (slot && !slotSetRef.current) {
          slotSetRef.current = true;
          setSlotEntry(slot);
        }
        setRevealComplete(data.revealComplete || false);
      } catch (err) {
        setError(err.message || 'Failed to load identity reveal');
      }
    };
    poll();
    const interval = setInterval(poll, 1500);
    return () => clearInterval(interval);
  }, [gameCode, playerCode]);

  const canClose = revealComplete && slotDone;

  if (error) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#f88' }}>
        {error}
        {onClose && (
          <button
            onClick={onClose}
            style={{
              display: 'block',
              width: '100%',
              padding: '0.75rem',
              marginTop: '1rem',
              background: '#2e7d32',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              fontSize: '0.95rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Ready
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
      gap: '1rem',
    }}>
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <h2 style={{
          fontSize: '1.4rem',
          letterSpacing: '0.2em',
          textTransform: 'uppercase',
          color: '#ccc',
          margin: 0,
        }}>
          {'\uD83D\uDC41\uFE0F'} Identity Reveal
        </h2>
      </div>

      {/* Banishment reveal \u2014 always shown, pending until backend fires */}
      <div style={{
        border: '1px solid #1e1e2e',
        borderRadius: '10px',
        background: '#0d0d1a',
        overflow: 'hidden',
      }}>
        <div style={{
          padding: '0.45rem 1rem',
          borderBottom: '1px solid #1e1e2e',
          fontSize: '0.72rem',
          color: '#444',
          textTransform: 'uppercase',
          letterSpacing: '0.08em',
        }}>
          This Round's Banishment
        </div>
        {slotEntry ? (
          <BanishReveal
            pickedEntry={slotEntry}
            onDone={() => setSlotDone(true)}
          />
        ) : (
          <div style={{
            padding: '2rem 1rem',
            textAlign: 'center',
            color: '#333',
            fontSize: '0.88rem',
            animation: 'ir-pending-pulse 2s ease-in-out infinite',
          }}>
            {'\u23F3'} Determining banishment outcome{'\u2026'}
          </div>
        )}
      </div>

      {/* Static list of all other fallen players */}
      {staticPlayers.length > 0 && (
        <div style={{
          border: '1px solid #1e1e2e',
          borderRadius: '10px',
          overflow: 'hidden',
        }}>
          <div style={{
            padding: '0.45rem 1rem',
            borderBottom: '1px solid #1e1e2e',
            fontSize: '0.72rem',
            color: '#444',
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
          }}>
            All Fallen Players
          </div>
          {staticPlayers.map((player, idx) => (
            <StaticPlayerRow key={player.playerId || idx} player={player} />
          ))}
        </div>
      )}

      {/* Ready button */}
      <button
        onClick={onClose}
        disabled={!canClose || !onClose}
        style={{
          display: 'block',
          width: '100%',
          padding: '0.75rem',
          background: canClose ? '#2e7d32' : '#616161',
          color: canClose ? 'white' : '#9e9e9e',
          border: 'none',
          borderRadius: '6px',
          fontSize: '0.95rem',
          fontWeight: 600,
          cursor: canClose && onClose ? 'pointer' : 'not-allowed',
        }}
      >
        {canClose ? 'Ready' : 'Waiting...'}
      </button>
    </div>
  );
}

export default IdentityRevealPage;

