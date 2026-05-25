import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { getGameState, markEventReady } from '../services/api';
import ChatWindow from '../components/ChatWindow';

function ScuttlebuttPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const [players, setPlayers] = useState([]);
  const [selectedPlayerId, setSelectedPlayerId] = useState('');
  const [messagedIds, setMessagedIds] = useState(() => {
    try {
      const stored = sessionStorage.getItem(`scuttlebutt_messaged_${gameCode}`);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch { return new Set(); }
  });
  const [completed, setCompleted] = useState(false);
  const [completing, setCompleting] = useState(false);

  const REQUIRED = 2;

  useEffect(() => {
    async function loadPlayers() {
      try {
        const state = await getGameState(gameCode, playerCode);
        const myId = state.myPlayerId || playerCode;
        const alive = (state.players || []).filter(p => !p.isDead && p.id !== myId);
        setPlayers(alive);
      } catch {
        // ignore
      }
    }
    loadPlayers();
  }, [gameCode, playerCode]);

  const handleMessageSent = useCallback((recipientId) => {
    setMessagedIds(prev => {
      if (prev.has(recipientId)) return prev;
      const next = new Set(prev);
      next.add(recipientId);
      try {
        sessionStorage.setItem(`scuttlebutt_messaged_${gameCode}`, JSON.stringify([...next]));
      } catch { /* ignore */ }
      return next;
    });
  }, [gameCode]);

  const handleComplete = async () => {
    if (completing || completed) return;
    setCompleting(true);
    try {
      await markEventReady(gameCode, playerCode);
      setCompleted(true);
    } catch {
      // ignore — backend may already have transitioned
      setCompleted(true);
    } finally {
      setCompleting(false);
    }
  };

  const progressCount = messagedIds.size;
  const canComplete = progressCount >= REQUIRED;

  const selectedPlayer = players.find(p => p.id === selectedPlayerId);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: '#121212' }}>
      {/* Header */}
      <div style={{
        padding: '0.5rem 1rem',
        background: '#1a2a1a',
        color: '#81C784',
        fontWeight: 'bold',
        fontSize: '0.9rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid #2e7d32',
      }}>
        <span>🌊 Scuttlebutt</span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span style={{
            fontSize: '0.8rem',
            padding: '0.2rem 0.6rem',
            borderRadius: '12px',
            background: canComplete ? '#2e7d32' : '#333',
            color: canComplete ? '#A5D6A7' : '#888',
            fontWeight: 'bold',
          }}>
            {progressCount}/{REQUIRED} players messaged
          </span>
          <button
            onClick={handleComplete}
            disabled={!canComplete || completed || completing}
            style={{
              padding: '0.3rem 0.75rem',
              background: completed ? '#1b5e20' : (canComplete ? '#43a047' : '#333'),
              color: completed ? '#A5D6A7' : (canComplete ? 'white' : '#666'),
              border: canComplete && !completed ? 'none' : '1px dashed #555',
              borderRadius: '4px',
              cursor: canComplete && !completed ? 'pointer' : 'not-allowed',
              fontWeight: 'bold',
              fontSize: '0.8rem',
              opacity: completed ? 0.7 : 1,
            }}
          >
            {completed ? 'Done ✓' : completing ? '...' : 'Complete'}
          </button>
        </div>
      </div>

      {/* Player selector */}
      <div style={{ padding: '0.5rem', background: '#1a1a1a', borderBottom: '1px solid #333' }}>
        <select
          value={selectedPlayerId}
          onChange={(e) => setSelectedPlayerId(e.target.value)}
          style={{
            width: '100%',
            padding: '0.5rem',
            background: '#2a2a2a',
            color: '#e0e0e0',
            border: '1px solid #444',
            borderRadius: '4px',
            fontSize: '0.9rem',
          }}
        >
          <option value="">Select a player to message…</option>
          {players.map(p => (
            <option key={p.id} value={p.id}>
              {p.name}{messagedIds.has(p.id) ? ' ✓' : ''}
            </option>
          ))}
        </select>
        {selectedPlayer && messagedIds.has(selectedPlayerId) && (
          <div style={{ fontSize: '0.75rem', color: '#81C784', marginTop: '0.25rem', paddingLeft: '0.25rem' }}>
            ✓ You've messaged {selectedPlayer.name}
          </div>
        )}
      </div>

      {/* Chat area */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {selectedPlayerId ? (
          <ChatWindow
            key={selectedPlayerId}
            gameCode={gameCode}
            playerCode={playerCode}
            channel="individual"
            recipientId={selectedPlayerId}
            onMessageSent={handleMessageSent}
          />
        ) : (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#666', fontSize: '0.9rem' }}>
            Select a player to start a private conversation.<br />
            You must message {REQUIRED} different players to complete Scuttlebutt.
          </div>
        )}
      </div>
    </div>
  );
}

export default ScuttlebuttPage;
