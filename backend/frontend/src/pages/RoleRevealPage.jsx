import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getRoleReveal, confirmRoleReveal } from '../services/api';

// ─── Inject keyframe animations once ──────────────────────────────────────────
const STYLE_ID = 'role-reveal-styles';
const KEYFRAMES = `
  @keyframes rr-pulse-bg {
    0%, 100% { opacity: 0.7; }
    50%       { opacity: 1; }
  }
  @keyframes rr-suspense-text {
    0%   { opacity: 0; letter-spacing: 0.6em; }
    60%  { opacity: 1; letter-spacing: 0.3em; }
    100% { opacity: 1; letter-spacing: 0.25em; }
  }
  @keyframes rr-card-flip {
    0%   { transform: perspective(600px) rotateY(90deg) scale(0.8); opacity: 0; }
    50%  { transform: perspective(600px) rotateY(-8deg) scale(1.05); opacity: 1; }
    100% { transform: perspective(600px) rotateY(0deg) scale(1); opacity: 1; }
  }
  @keyframes rr-role-glow-traitor {
    0%, 100% { text-shadow: 0 0 20px rgba(220,30,30,0.7), 0 0 60px rgba(255,0,0,0.4); }
    50%       { text-shadow: 0 0 40px rgba(255,50,50,1), 0 0 100px rgba(200,0,0,0.7); }
  }
  @keyframes rr-role-glow-faithful {
    0%, 100% { text-shadow: 0 0 20px rgba(60,120,255,0.7), 0 0 60px rgba(30,80,220,0.4); }
    50%       { text-shadow: 0 0 40px rgba(100,170,255,1), 0 0 100px rgba(60,120,255,0.7); }
  }
  @keyframes rr-badge-in {
    0%   { opacity: 0; transform: scale(0.5) translateY(20px); }
    70%  { transform: scale(1.12) translateY(-4px); }
    100% { opacity: 1; transform: scale(1) translateY(0); }
  }
  @keyframes rr-confirm-pulse {
    0%, 100% { box-shadow: 0 0 10px 2px currentColor; }
    50%       { box-shadow: 0 0 24px 8px currentColor; }
  }
  @keyframes rr-suspense-fade-out {
    0%   { opacity: 1; transform: translateY(0) scale(1); }
    100% { opacity: 0; transform: translateY(-28px) scale(0.95); }
  }
  @keyframes rr-reveal-fade-in {
    0%   { opacity: 0; transform: translateY(28px) scale(0.97); }
    100% { opacity: 1; transform: translateY(0) scale(1); }
  }
  @keyframes rr-suspense-color {
    0%   { color: #cc2222; text-shadow: 0 0 12px rgba(200,30,30,0.6); }
    30%  { color: #aa2299; text-shadow: 0 0 12px rgba(170,30,150,0.6); }
    60%  { color: #4455cc; text-shadow: 0 0 12px rgba(60,80,210,0.6); }
    100% { color: #60a5fa; text-shadow: 0 0 16px rgba(96,165,250,0.7); }
  }
  @keyframes rr-dot-color {
    0%   { background: #cc2222; box-shadow: 0 0 6px 2px rgba(200,30,30,0.6); }
    30%  { background: #aa2299; box-shadow: 0 0 6px 2px rgba(170,30,150,0.6); }
    60%  { background: #4455cc; box-shadow: 0 0 6px 2px rgba(60,80,210,0.6); }
    100% { background: #60a5fa; box-shadow: 0 0 6px 2px rgba(96,165,250,0.7); }
  }
  @keyframes rr-particle-float {
    0%   { transform: translateY(0) scale(1); opacity: 0.8; }
    100% { transform: translateY(-60px) scale(0); opacity: 0; }
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

// ─── Particle burst on reveal ──────────────────────────────────────────────────
function ParticleBurst({ isTraitor }) {
  const color = isTraitor ? '#ff3333' : '#60a5fa';
  const particles = Array.from({ length: 12 }, (_, i) => i);
  return (
    <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', overflow: 'hidden' }}>
      {particles.map((i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            left: `${30 + Math.random() * 40}%`,
            top: `${40 + Math.random() * 30}%`,
            width: `${4 + Math.random() * 6}px`,
            height: `${4 + Math.random() * 6}px`,
            borderRadius: '50%',
            background: color,
            animation: `rr-particle-float ${0.8 + Math.random() * 1.2}s ease-out ${Math.random() * 0.4}s forwards`,
            boxShadow: `0 0 6px 2px ${color}`,
          }}
        />
      ))}
    </div>
  );
}

// ─── Main page ─────────────────────────────────────────────────────────────────
export default function RoleRevealPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  const [phase, setPhase] = useState('suspense'); // 'suspense' | 'revealed' | 'confirmed'
  const [roleData, setRoleData] = useState(null);  // { isTraitor, myRole, confirmedCount, totalPlayers, myConfirmed }
  const [confirmed, setConfirmed] = useState(false);
  const [confirmError, setConfirmError] = useState('');
  const [showParticles, setShowParticles] = useState(false);
  const [fadingOut, setFadingOut] = useState(false);
  const revealScheduled = useRef(false);
  const pollRef = useRef(null);

  useEffect(() => {
    injectStyles();
  }, []);

  // Initial fetch then poll — trigger reveal inside callback to avoid timer cancellation bug
  useEffect(() => {
    if (!playerCode) return;

    const poll = async () => {
      try {
        const data = await getRoleReveal(gameCode, playerCode);
        setRoleData(data);
        if (data.myConfirmed) setConfirmed(true);
        // Schedule the reveal exactly once, on first successful data load
        if (!revealScheduled.current) {
          revealScheduled.current = true;
          // At 13.5s: fade out the suspense text
          setTimeout(() => setFadingOut(true), 13500);
          // At 15s: flip to revealed phase with card fade-in
          setTimeout(() => {
            setPhase('revealed');
            setShowParticles(true);
            setTimeout(() => setShowParticles(false), 2000);
          }, 15000);
        }
      } catch {
        // silently ignore — server may not have started the event yet
      }
    };

    poll();
    pollRef.current = setInterval(poll, 2000);
    return () => clearInterval(pollRef.current);
  }, [gameCode, playerCode]);

  const handleConfirm = async () => {
    if (confirmed) return;
    try {
      await confirmRoleReveal(gameCode, playerCode);
      setConfirmed(true);
      setPhase('confirmed');
    } catch (err) {
      setConfirmError(err.message || 'Failed to confirm');
    }
  };

  // ── Derived display values ──────────────────────────────────────────────────
  const isTraitor = roleData?.isTraitor ?? false;
  const confirmedCount = roleData?.confirmedCount ?? 0;
  const totalPlayers = roleData?.totalPlayers ?? 0;

  const bg = isTraitor
    ? 'radial-gradient(ellipse at 50% 40%, #2d0000 0%, #0a0008 60%, #000 100%)'
    : 'radial-gradient(ellipse at 50% 40%, #000d2e 0%, #001040 40%, #000 100%)';

  const accentColor  = isTraitor ? '#ff3333' : '#60a5fa';
  const accentGlow   = isTraitor ? 'rgba(255,50,50,0.5)' : 'rgba(96,165,250,0.5)';
  const borderColor  = isTraitor ? 'rgba(200,30,30,0.6)' : 'rgba(60,120,255,0.6)';
  const roleLabel    = isTraitor ? 'THE HEARTLESS' : 'THE FAITHFUL';
  const roleIcon     = isTraitor ? '🗡️' : '🛡️';
  const roleSubtitle = isTraitor
    ? 'Deceive. Divide. Survive.'
    : 'Seek the truth. Protect the innocent.';
  const glowAnim     = isTraitor ? 'rr-role-glow-traitor' : 'rr-role-glow-faithful';

  return (
    <div style={{
      minHeight: '100%',
      background: bg,
      color: 'white',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2rem 1.5rem',
      position: 'relative',
      overflow: 'hidden',
      animation: 'rr-pulse-bg 4s ease-in-out infinite',
    }}>

      {/* Atmospheric vignette overlay */}
      <div style={{
        position: 'absolute',
        inset: 0,
        background: 'radial-gradient(ellipse at center, transparent 40%, rgba(0,0,0,0.7) 100%)',
        pointerEvents: 'none',
      }} />

      {showParticles && <ParticleBurst isTraitor={isTraitor} />}

      {/* ── Suspense phase ── */}
      {phase === 'suspense' && (
        <div style={{
          textAlign: 'center',
          zIndex: 1,
          animation: fadingOut ? 'rr-suspense-fade-out 1.5s ease-in forwards' : undefined,
        }}>
          <p style={{
            fontSize: '1rem',
            letterSpacing: '0.25em',
            textTransform: 'uppercase',
            animation: 'rr-suspense-text 2s ease-out forwards, rr-suspense-color 15s linear forwards',
          }}>
            {roleData ? 'Your fate is sealed...' : 'Awaiting the reckoning...'}
          </p>
          <div style={{ marginTop: '2rem', display: 'flex', gap: '6px', justifyContent: 'center' }}>
            {[0, 1, 2].map((i) => (
              <div key={i} style={{
                width: '8px', height: '8px', borderRadius: '50%',
                animation: `rr-dot-color 15s linear forwards, rr-pulse-bg 1s ease-in-out ${i * 0.3}s infinite`,
              }} />
            ))}
          </div>
        </div>
      )}

      {/* ── Revealed / Confirmed phase ── */}
      {(phase === 'revealed' || phase === 'confirmed') && roleData && (
        <div style={{
          zIndex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '1.5rem',
          width: '100%',
          maxWidth: '360px',
          animation: phase === 'revealed' ? 'rr-reveal-fade-in 0.9s ease-out forwards' : undefined,
        }}>

          {/* Role card */}
          <div style={{
            width: '100%',
            border: `2px solid ${borderColor}`,
            borderRadius: '16px',
            padding: '2rem 1.5rem',
            background: isTraitor
              ? 'linear-gradient(160deg, rgba(80,5,5,0.5) 0%, rgba(20,0,0,0.9) 100%)'
              : 'linear-gradient(160deg, rgba(5,20,80,0.5) 0%, rgba(0,5,30,0.9) 100%)',
            boxShadow: `0 0 40px ${accentGlow}, inset 0 1px 0 ${borderColor}`,
            textAlign: 'center',
            animation: 'rr-card-flip 0.7s cubic-bezier(0.22,1,0.36,1) forwards',
          }}>
            <p style={{
              fontSize: '0.75rem',
              color: '#888',
              letterSpacing: '0.3em',
              textTransform: 'uppercase',
              marginBottom: '1rem',
            }}>
              Your Role
            </p>

            {/* Icon */}
            <div style={{
              fontSize: '3.5rem',
              marginBottom: '0.75rem',
              animation: `rr-badge-in 0.6s cubic-bezier(0.22,1,0.36,1) 0.15s both`,
            }}>
              {roleIcon}
            </div>

            {/* Role name */}
            <h1 style={{
              fontSize: '2rem',
              fontWeight: '900',
              color: accentColor,
              letterSpacing: '0.15em',
              textTransform: 'uppercase',
              margin: '0 0 0.5rem 0',
              animation: `${glowAnim} 2.5s ease-in-out infinite`,
            }}>
              {roleLabel}
            </h1>

            {/* Subtitle */}
            <p style={{
              fontSize: '0.85rem',
              color: '#aaa',
              fontStyle: 'italic',
              letterSpacing: '0.06em',
              margin: 0,
            }}>
              {roleSubtitle}
            </p>

            {/* Flavour text */}
            <div style={{
              marginTop: '1.25rem',
              padding: '0.75rem 1rem',
              background: 'rgba(255,255,255,0.04)',
              borderRadius: '8px',
              fontSize: '0.8rem',
              color: '#bbb',
              lineHeight: '1.5',
            }}>
              {isTraitor
                ? 'The Heartless conspire in shadow. Only you know the truth. Keep your identity secret — eliminate the Faithful before they banish you.'
                : 'You stand with the Faithful. Root out the Heartless who hide among you before they tear your alliance apart.'}
            </div>
          </div>

          {/* Confirmation progress */}
          <div style={{
            width: '100%',
            background: 'rgba(255,255,255,0.05)',
            borderRadius: '8px',
            padding: '0.6rem 1rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.85rem',
            color: '#aaa',
            border: '1px solid rgba(255,255,255,0.08)',
          }}>
            <span>Players confirmed</span>
            <span style={{ fontWeight: 'bold', color: accentColor }}>
              {confirmedCount} / {totalPlayers}
            </span>
          </div>

          {/* Confirm button */}
          {!confirmed ? (
            <button
              onClick={handleConfirm}
              style={{
                width: '100%',
                padding: '1rem',
                fontSize: '1.05rem',
                fontWeight: 'bold',
                letterSpacing: '0.1em',
                textTransform: 'uppercase',
                background: isTraitor
                  ? 'linear-gradient(135deg, #7f0000 0%, #cc0000 100%)'
                  : 'linear-gradient(135deg, #0d3080 0%, #1565c0 100%)',
                color: accentColor,
                border: `1px solid ${accentColor}`,
                borderRadius: '10px',
                cursor: 'pointer',
                animation: `rr-confirm-pulse 2s ease-in-out infinite`,
                animationTimingFunction: 'ease-in-out',
              }}
            >
              {isTraitor ? '⚔️  I am the Heartless' : '🛡️  I am Faithful'}
            </button>
          ) : (
            <div style={{
              width: '100%',
              padding: '1rem',
              fontSize: '1rem',
              fontWeight: 'bold',
              textAlign: 'center',
              color: accentColor,
              border: `1px solid ${accentColor}`,
              borderRadius: '10px',
              background: 'rgba(255,255,255,0.03)',
              letterSpacing: '0.08em',
            }}>
              ✓ Confirmed — awaiting others...
            </div>
          )}

          {confirmError && (
            <p style={{ color: '#f88', fontSize: '0.85rem', margin: 0 }}>{confirmError}</p>
          )}

          {onClose && (
            <button
              onClick={onClose}
              style={{
                width: '100%',
                padding: '0.6rem',
                background: 'transparent',
                color: '#666',
                border: '1px solid #333',
                borderRadius: '6px',
                fontSize: '0.85rem',
                cursor: 'pointer',
              }}
            >
              ← Back to Menu
            </button>
          )}
        </div>
      )}
    </div>
  );
}
