import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { getGameState, getChatMessages, markEventReady } from '../services/api';

function ScuttlebuttPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const [myPlayerId, setMyPlayerId] = useState(null);
  const [isTraitor, setIsTraitor] = useState(false);
  const [messagedIds, setMessagedIds] = useState(() => {
    try {
      const stored = sessionStorage.getItem(`scuttlebutt_messaged_${gameCode}`);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch { return new Set(); }
  });
  const [completed, setCompleted] = useState(false);
  const [completing, setCompleting] = useState(false);

  const REQUIRED = 2;

  // Load player identity once
  useEffect(() => {
    async function loadIdentity() {
      try {
        const state = await getGameState(gameCode, playerCode);
        setMyPlayerId(state.myPlayerId || null);
        setIsTraitor(!!state.isTraitor);
      } catch { /* ignore */ }
    }
    loadIdentity();
  }, [gameCode, playerCode]);

  // Poll individual chat messages to track distinct recipients this player has messaged
  const pollMessages = useCallback(async () => {
    if (!myPlayerId) return;
    try {
      const result = await getChatMessages(gameCode, playerCode, 'individual', null);
      const msgs = result.messages || [];
      const newIds = new Set(
        msgs
          .filter(m => m.senderId === myPlayerId && m.recipientId)
          .map(m => m.recipientId)
      );
      if (newIds.size > 0) {
        setMessagedIds(prev => {
          const merged = new Set([...prev, ...newIds]);
          if (merged.size !== prev.size) {
            try {
              sessionStorage.setItem(`scuttlebutt_messaged_${gameCode}`, JSON.stringify([...merged]));
            } catch { /* ignore */ }
          }
          return merged;
        });
      }
    } catch { /* ignore */ }
  }, [gameCode, playerCode, myPlayerId]);

  useEffect(() => {
    pollMessages();
    const interval = setInterval(pollMessages, 3000);
    return () => clearInterval(interval);
  }, [pollMessages]);

  const handleComplete = async () => {
    if (completing || completed) return;
    setCompleting(true);
    try {
      await markEventReady(gameCode, playerCode);
      setCompleted(true);
    } catch {
      setCompleted(true);
    } finally {
      setCompleting(false);
    }
  };

  const progressCount = messagedIds.size;
  const canComplete = isTraitor || progressCount >= REQUIRED;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: '#121212' }}>
      {/* Header */}
      <div style={{
        padding: '0.5rem 1rem',
        background: '#1a2a1a',
        color: '#81C784',
        fontWeight: 'bold',
        fontSize: '0.9rem',
        borderBottom: '1px solid #2e7d32',
      }}>
        🌊 Scuttlebutt
      </div>

      {/* Body */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '2rem', gap: '1.5rem' }}>

        {isTraitor ? (
          <div style={{ textAlign: 'center', color: '#ef9a9a', fontSize: '0.9rem', maxWidth: '260px' }}>
            Use the <strong>Individual Chat</strong> tab to coordinate if you wish.<br />
            You may complete this phase at any time.
          </div>
        ) : (
          <>
            <div style={{ textAlign: 'center', color: '#aaa', fontSize: '0.9rem', maxWidth: '260px' }}>
              Use the <strong>Individual Chat</strong> tab to privately message your fellow players.
              Message at least <strong>{REQUIRED}</strong> different players before marking yourself ready.
            </div>
            {/* Progress */}
            <div style={{
              padding: '0.4rem 1.2rem',
              borderRadius: '20px',
              background: canComplete ? '#2e7d32' : '#333',
              color: canComplete ? '#A5D6A7' : '#888',
              fontWeight: 'bold',
              fontSize: '0.9rem',
            }}>
              {progressCount} / {REQUIRED} players messaged
            </div>
          </>
        )}

        {/* Complete button */}
        <button
          onClick={handleComplete}
          disabled={!canComplete || completed || completing}
          style={{
            padding: '0.6rem 2rem',
            background: completed ? '#1b5e20' : (canComplete ? '#43a047' : '#333'),
            color: completed ? '#A5D6A7' : (canComplete ? 'white' : '#666'),
            border: canComplete && !completed ? 'none' : '1px dashed #555',
            borderRadius: '6px',
            cursor: canComplete && !completed ? 'pointer' : 'not-allowed',
            fontWeight: 'bold',
            fontSize: '1rem',
            opacity: completed ? 0.75 : 1,
          }}
        >
          {completed ? 'Done ✓' : completing ? '...' : 'Mark Ready'}
        </button>
      </div>
    </div>
  );
}

export default ScuttlebuttPage;
