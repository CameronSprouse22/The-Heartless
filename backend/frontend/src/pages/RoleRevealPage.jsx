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
    0%, 100% { text-shadow: 0 0 14px rgba(200,60,60,0.35), 0 0 30px rgba(180,40,40,0.15); }
    50%       { text-shadow: 0 0 22px rgba(220,80,80,0.55), 0 0 50px rgba(180,40,40,0.25); }
  }
  @keyframes rr-role-glow-faithful {
    0%, 100% { text-shadow: 0 0 14px rgba(80,130,220,0.35), 0 0 30px rgba(60,100,200,0.15); }
    50%       { text-shadow: 0 0 22px rgba(100,150,240,0.55), 0 0 50px rgba(60,100,200,0.25); }
  }
  @keyframes rr-badge-in {
    0%   { opacity: 0; transform: scale(0.5) translateY(20px); }
    70%  { transform: scale(1.12) translateY(-4px); }
    100% { opacity: 1; transform: scale(1) translateY(0); }
  }
  @keyframes rr-confirm-pulse {
    0%, 100% { box-shadow: 0 0 8px 2px rgba(255,255,255,0.15); }
    50%       { box-shadow: 0 0 18px 5px rgba(255,255,255,0.25); }
  }
  @keyframes rr-suspense-fade-out {
    0%   { opacity: 1; transform: translateY(0) scale(1); }
    100% { opacity: 0; transform: translateY(-28px) scale(0.95); }
  }
  @keyframes rr-reveal-fade-in {
    0%   { opacity: 0; transform: translateY(28px) scale(0.97); }
    100% { opacity: 1; transform: translateY(0) scale(1); }
  }
  @keyframes rr-particle-float {
    0%   { transform: translateY(0) scale(1); opacity: 0.7; }
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
  // Mostly white sparks with a very faint color tint
  const color = isTraitor ? 'rgba(240,210,210,0.9)' : 'rgba(210,220,240,0.9)';
  const glowColor = isTraitor ? 'rgba(200,80,80,0.3)' : 'rgba(80,120,210,0.3)';
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
            width: `${3 + Math.random() * 5}px`,
            height: `${3 + Math.random() * 5}px`,
            borderRadius: '50%',
            background: color,
            animation: `rr-particle-float ${0.8 + Math.random() * 1.2}s ease-out ${Math.random() * 0.4}s forwards`,
            boxShadow: `0 0 6px 3px ${glowColor}`,
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
  const phaseRef = useRef('suspense');   // mirror of phase, readable inside poll closure
  const fadeTimerRef = useRef(null);
  const revealTimerRef = useRef(null);
  const pollRef = useRef(null);

  useEffect(() => {
    injectStyles();
  }, []);

  // Initial fetch then poll — trigger reveal inside callback to avoid timer cancellation bug
  useEffect(() => {
    if (!playerCode) return;

    const triggerReveal = () => {
      if (phaseRef.current !== 'suspense') return;
      clearTimeout(fadeTimerRef.current);
      clearTimeout(revealTimerRef.current);
      phaseRef.current = 'revealed';
      setFadingOut(false);
      setPhase('revealed');
      setShowParticles(true);
      setTimeout(() => setShowParticles(false), 2000);
    };

    const poll = async () => {
      try {
        const data = await getRoleReveal(gameCode, playerCode);
        setRoleData(data);
        if (data.myConfirmed) setConfirmed(true);
        const msLeft = data.eventEndTime ? data.eventEndTime - Date.now() : Infinity;

        // If ≤5s remain and still in suspense, skip the animation and reveal now
        if (phaseRef.current === 'suspense' && msLeft <= 5000) {
          triggerReveal();
          return;
        }

        // Schedule the reveal exactly once, on first successful data load
        if (!revealScheduled.current) {
          revealScheduled.current = true;
          fadeTimerRef.current = setTimeout(() => setFadingOut(true), 13500);
          revealTimerRef.current = setTimeout(triggerReveal, 15000);
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

  // Mostly monochrome — accents are subtle tints, not vibrant colors
  const accentColor  = isTraitor ? '#d97070' : '#7099cc';
  const accentGlow   = isTraitor ? 'rgba(180,60,60,0.18)' : 'rgba(60,100,200,0.18)';
  const borderColor  = isTraitor ? 'rgba(180,70,70,0.35)' : 'rgba(70,110,200,0.35)';
  const roleLabel    = isTraitor ? 'THE HEARTLESS' : 'THE FAITHFUL';
  const roleIcon     = isTraitor ? '🗡️' : '🛡️';
  const roleSubtitle = isTraitor
    ? 'Deceive. Divide. Survive.'
    : 'Seek the truth. Protect the innocent.';
  const glowAnim     = isTraitor ? 'rr-role-glow-traitor' : 'rr-role-glow-faithful';

  return (
    <div style={{
      minHeight: '100%',
      background: 'radial-gradient(ellipse at 50% 40%, #131313 0%, #060606 60%, #000 100%)',
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
            color: '#999',
            animation: 'rr-suspense-text 2s ease-out forwards',
          }}>
            {roleData ? 'Your part has been decided...' : 'Awaiting the reckoning...'}
          </p>
          <div style={{ marginTop: '2rem', display: 'flex', gap: '6px', justifyContent: 'center' }}>
            {[0, 1, 2].map((i) => (
              <div key={i} style={{
                width: '8px', height: '8px', borderRadius: '50%',
                background: '#555',
                animation: `rr-pulse-bg 1s ease-in-out ${i * 0.3}s infinite`,
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
            border: `1px solid ${borderColor}`,
            borderRadius: '16px',
            padding: '2rem 1.5rem',
            background: 'linear-gradient(160deg, rgba(28,28,28,0.95) 0%, rgba(10,10,10,0.98) 100%)',
            boxShadow: `0 0 30px ${accentGlow}, inset 0 1px 0 rgba(255,255,255,0.06)`,
            backdropFilter: 'blur(4px)',
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
              color: '#e8e8e8',
              letterSpacing: '0.15em',
              textTransform: 'uppercase',
              margin: '0 0 0.5rem 0',
              animation: `${glowAnim} 3s ease-in-out infinite`,
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
            <span style={{ fontWeight: 'bold', color: '#ccc' }}>
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
                background: 'linear-gradient(135deg, #1a1a1a 0%, #222 100%)',
                color: '#ddd',
                border: `1px solid ${borderColor}`,
                borderRadius: '10px',
                cursor: 'pointer',
                animation: `rr-confirm-pulse 2s ease-in-out infinite`,
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
              color: '#bbb',
              border: '1px solid rgba(255,255,255,0.15)',
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
