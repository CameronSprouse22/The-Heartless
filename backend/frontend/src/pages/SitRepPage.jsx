import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { getSitRep, markEventReady } from '../services/api';

// ── Life-status helpers ─────────────────────────────────────────────────────

const STATUS_LABELS = {
  ALIVE:                  'Alive',
  MARKED_FOR_DEATH:       'Marked',
  MARKED_FOR_MURDER:      'Marked',
  MURDERED:               'Murdered',
  MARKED_FOR_BANISHMENT:  'Marked',
  BANISHED:               'Banished',
  WON_AS_FAITHFUL:        'Won',
  WON_AS_TRAITOR:         'Won',
  WON_AS_SOLE_TRAITOR:    'Won',
  PLAYER_LEFT_GAME:       'Left',
  PLAYER_BOOTED:          'Booted',
};

const STATUS_COLORS = {
  ALIVE:                  { bg: '#e8f5e9', text: '#2e7d32', border: '#a5d6a7' },
  MARKED_FOR_DEATH:       { bg: '#fff8e1', text: '#f57f17', border: '#ffe082' },
  MARKED_FOR_MURDER:      { bg: '#fff8e1', text: '#f57f17', border: '#ffe082' },
  MURDERED:               { bg: '#ffebee', text: '#c62828', border: '#ef9a9a' },
  MARKED_FOR_BANISHMENT:  { bg: '#fff8e1', text: '#f57f17', border: '#ffe082' },
  BANISHED:               { bg: '#f3e5f5', text: '#6a1b9a', border: '#ce93d8' },
  WON_AS_FAITHFUL:        { bg: '#e3f2fd', text: '#1565c0', border: '#90caf9' },
  WON_AS_TRAITOR:         { bg: '#fbe9e7', text: '#bf360c', border: '#ffab91' },
  WON_AS_SOLE_TRAITOR:    { bg: '#fbe9e7', text: '#bf360c', border: '#ffab91' },
  PLAYER_LEFT_GAME:       { bg: '#f5f5f5', text: '#757575', border: '#bdbdbd' },
  PLAYER_BOOTED:          { bg: '#f5f5f5', text: '#757575', border: '#bdbdbd' },
};

function LifeBadge({ lifeStatus }) {
  const label  = STATUS_LABELS[lifeStatus]  ?? lifeStatus;
  const colors = STATUS_COLORS[lifeStatus]  ?? { bg: '#eee', text: '#333', border: '#ccc' };
  return (
    <span style={{
      padding: '2px 8px',
      borderRadius: '12px',
      fontSize: '0.78rem',
      fontWeight: 700,
      background: colors.bg,
      color: colors.text,
      border: `1px solid ${colors.border}`,
      whiteSpace: 'nowrap',
    }}>
      {label}
    </span>
  );
}

// ── Section card ─────────────────────────────────────────────────────────────

function SectionCard({ title, icon, color, children, empty }) {
  return (
    <div style={{
      marginBottom: '1rem',
      borderRadius: '8px',
      border: `1px solid ${color}33`,
      overflow: 'hidden',
    }}>
      <div style={{
        padding: '0.5rem 0.75rem',
        background: `${color}1a`,
        borderBottom: `1px solid ${color}33`,
        fontWeight: 700,
        fontSize: '0.9rem',
        color,
      }}>
        {icon} {title}
      </div>
      <div style={{ padding: '0.5rem 0.75rem' }}>
        {empty
          ? <span style={{ color: '#888', fontStyle: 'italic', fontSize: '0.85rem' }}>None</span>
          : children}
      </div>
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function SitRepPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  const [data, setData]         = useState(null);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState('');
  const [confirming, setConfirming] = useState(false);
  const [confirmError, setConfirmError] = useState('');

  const handleConfirm = async () => {
    if (!playerCode || !gameCode) return;
    setConfirming(true);
    setConfirmError('');
    try {
      await markEventReady(gameCode, playerCode);
      await load();
    } catch (err) {
      setConfirmError(err.message || 'Failed to confirm');
    } finally {
      setConfirming(false);
    }
  };

  const load = useCallback(async () => {
    if (!playerCode || !gameCode) return;
    try {
      const result = await getSitRep(gameCode, playerCode);
      setData(result);
      setError('');
    } catch (err) {
      setError(err.message || 'Failed to load situation report');
    } finally {
      setLoading(false);
    }
  }, [gameCode, playerCode]);

  useEffect(() => {
    load();
    const interval = setInterval(load, 5000);
    return () => clearInterval(interval);
  }, [load]);

  if (loading) {
    return <div style={{ padding: '2rem', textAlign: 'center', color: '#aaa' }}>Loading…</div>;
  }

  if (error) {
    return <div style={{ padding: '2rem', textAlign: 'center', color: '#ef5350' }}>{error}</div>;
  }

  const {
    round,
    players = [],
    murderedPlayers = [],
    banishedPlayers = [],
    banishedTraitors = [],
    aliveCount,
    totalCount,
    confirmedCount = 0,
    requiredCount = 0,
    myConfirmed = false,
  } = data;

  return (
    <div style={{ padding: '1rem', maxWidth: '520px', margin: '0 auto' }}>

      {/* Header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '1rem',
      }}>
        <h2 style={{ margin: 0, fontSize: '1.3rem' }}>📋 Situation Report</h2>
      </div>

      {/* Round + summary pill */}
      <div style={{
        display: 'flex',
        gap: '0.75rem',
        flexWrap: 'wrap',
        marginBottom: '1.25rem',
      }}>
        <div style={{
          padding: '0.4rem 0.9rem',
          borderRadius: '20px',
          background: '#1a237e',
          color: 'white',
          fontWeight: 700,
          fontSize: '0.9rem',
        }}>
          Round {round}
        </div>
        <div style={{
          padding: '0.4rem 0.9rem',
          borderRadius: '20px',
          background: '#e8f5e9',
          color: '#2e7d32',
          fontWeight: 700,
          fontSize: '0.9rem',
          border: '1px solid #a5d6a7',
        }}>
          {aliveCount} / {totalCount} alive
        </div>
        {murderedPlayers.length > 0 && (
          <div style={{
            padding: '0.4rem 0.9rem',
            borderRadius: '20px',
            background: '#ffebee',
            color: '#c62828',
            fontWeight: 700,
            fontSize: '0.9rem',
            border: '1px solid #ef9a9a',
          }}>
            {murderedPlayers.length} murdered
          </div>
        )}
        {banishedPlayers.length > 0 && (
          <div style={{
            padding: '0.4rem 0.9rem',
            borderRadius: '20px',
            background: '#f3e5f5',
            color: '#6a1b9a',
            fontWeight: 700,
            fontSize: '0.9rem',
            border: '1px solid #ce93d8',
          }}>
            {banishedPlayers.length} banished
          </div>
        )}
        {banishedTraitors.length > 0 && (
          <div style={{
            padding: '0.4rem 0.9rem',
            borderRadius: '20px',
            background: '#fbe9e7',
            color: '#b71c1c',
            fontWeight: 700,
            fontSize: '0.9rem',
            border: '1px solid #ffab91',
          }}>
            {banishedTraitors.length} heartless banished
          </div>
        )}
      </div>

      {/* All Players */}
      <SectionCard title="All Players" icon="👥" color="#1976d2" empty={players.length === 0}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
          {players.map((p) => (
            <div key={p.id} style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.35rem 0.5rem',
              borderRadius: '6px',
              background: p.alive ? 'transparent' : '#fafafa',
              opacity: p.alive ? 1 : 0.7,
            }}>
              <span style={{ fontWeight: p.alive ? 500 : 400, color: p.alive ? '#212121' : '#9e9e9e' }}>
                {p.alive ? '' : '†'} {p.name}
              </span>
              <LifeBadge lifeStatus={p.lifeStatus} />
            </div>
          ))}
        </div>
      </SectionCard>

      {/* Murdered */}
      <SectionCard
        title="Murdered"
        icon="🔪"
        color="#c62828"
        empty={murderedPlayers.length === 0}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
          {murderedPlayers.map((p) => (
            <div key={p.id} style={{ padding: '0.3rem 0.5rem', color: '#c62828', fontWeight: 600 }}>
              † {p.name}
            </div>
          ))}
        </div>
      </SectionCard>

      {/* Banished */}
      <SectionCard
        title="Banished"
        icon="⚖️"
        color="#6a1b9a"
        empty={banishedPlayers.length === 0}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
          {banishedPlayers.map((p) => (
            <div key={p.id} style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.3rem 0.5rem',
            }}>
              <span style={{ fontWeight: 600, color: '#6a1b9a' }}>{p.name}</span>
              {p.wasTraitor && (
                <span style={{
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  background: '#b71c1c',
                  color: 'white',
                  padding: '2px 7px',
                  borderRadius: '10px',
                }}>
                  TRAITOR
                </span>
              )}
            </div>
          ))}
        </div>
      </SectionCard>

      {/* Banished Traitors */}
      <SectionCard
        title="Heartless Banished"
        icon="🩸"
        color="#b71c1c"
        empty={banishedTraitors.length === 0}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
          {banishedTraitors.map((p) => (
            <div key={p.id} style={{ padding: '0.3rem 0.5rem', color: '#b71c1c', fontWeight: 700 }}>
              ⚡ {p.name}
            </div>
          ))}
        </div>
      </SectionCard>

      {/* Confirm section */}
      {requiredCount > 0 && (
        <div style={{
          marginTop: '1.25rem',
          padding: '1rem',
          borderRadius: '8px',
          border: '1px solid #bdbdbd',
          background: myConfirmed ? '#eeeeee' : '#f5f5f5',
          textAlign: 'center',
        }}>
          <div style={{ marginBottom: '0.6rem', fontSize: '0.9rem', color: '#757575' }}>
            {confirmedCount} / {requiredCount} players confirmed
          </div>
          {/* Progress bar */}
          <div style={{
            height: '6px',
            borderRadius: '3px',
            background: '#e0e0e0',
            overflow: 'hidden',
            marginBottom: '0.75rem',
          }}>
            <div style={{
              height: '100%',
              width: `${requiredCount > 0 ? Math.round((confirmedCount / requiredCount) * 100) : 0}%`,
              background: '#388e3c',
              transition: 'width 0.4s ease',
            }} />
          </div>
          {myConfirmed ? (
            <div style={{ color: '#388e3c', fontWeight: 700, fontSize: '1rem' }}>✔ You confirmed</div>
          ) : (
            <button
              onClick={handleConfirm}
              disabled={confirming}
              style={{
                padding: '0.55rem 1.6rem',
                borderRadius: '6px',
                border: 'none',
                background: confirming ? '#616161' : '#2e7d32',
                color: 'white',
                fontWeight: 700,
                fontSize: '1rem',
                cursor: confirming ? 'not-allowed' : 'pointer',
              }}
            >
              {confirming ? 'Confirming…' : 'Confirm'}
            </button>
          )}
          {confirmError && (
            <div style={{ marginTop: '0.4rem', color: '#c62828', fontSize: '0.85rem' }}>{confirmError}</div>
          )}
        </div>
      )}

    </div>
  );
}
