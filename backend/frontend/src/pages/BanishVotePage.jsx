import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';
import VoteCard from '../components/VoteCard';
import { getBanishCandidates, castBanishVote } from '../services/api';
import { connect, disconnect, subscribe, send } from '../services/websocket';

function BanishVotePage() {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();
  const playerCode = localStorage.getItem('playerCode');
  const [candidates, setCandidates] = useState([]);
  const [selected, setSelected] = useState(null);
  const [nameInput, setNameInput] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  // otherPlayers: { [voterId]: { voterName, targetName, submitted } }
  const [otherPlayers, setOtherPlayers] = useState({});
  const stompConnected = useRef(false);

  const updateOtherPlayer = (voterId, voterName, targetId, targetName, isSubmitted) => {
    setOtherPlayers(prev => {
      const existing = prev[voterId];
      return {
        ...prev,
        [voterId]: {
          voterName,
          targetId: targetId || null,
          targetName: targetName || null,
          submitted: isSubmitted || (existing && existing.submitted),
        },
      };
    });
  };

  // Initial load + polling every 3s for submitted votes
  useEffect(() => {
    if (!playerCode) { navigate('/'); return; }
    const load = () =>
      getBanishCandidates(gameCode, playerCode)
        .then(res => {
          setCandidates(res.candidates || []);
          (res.othersVotes || []).forEach(v =>
            updateOtherPlayer(v.voterId, v.voterName, v.targetId || null, v.targetName || null, true)
          );
        })
        .catch(err => setError(err.message || 'Failed to load'));
    load();
    const interval = setInterval(load, 3000);
    return () => clearInterval(interval);
  }, [gameCode, playerCode, navigate]);

  // WebSocket: receive submitted votes + live selections from other players
  useEffect(() => {
    if (!playerCode) return;
    connect(playerCode, () => {
      stompConnected.current = true;
      subscribe(`/topic/games/${gameCode}/banish-vote`, (msg) => {
        if (msg.type === 'BANISH_VOTE_UPDATE' || msg.type === 'BANISH_SELECTION_UPDATE') {
          updateOtherPlayer(
            msg.voterId,
            msg.voterName,
            msg.targetId || null,
            msg.targetName || null,
            msg.type === 'BANISH_VOTE_UPDATE'
          );
        }
      });
    }, (err) => console.error('WebSocket error:', err));
    return () => {
      stompConnected.current = false;
      disconnect();
    };
  }, [gameCode, playerCode]);

  const toggleSelect = (id) => {
    const newSelected = selected === id ? null : id;
    setSelected(newSelected);
    const targetName = newSelected
      ? (candidates.find(c => c.id === newSelected)?.name || '')
      : '';
    setNameInput(targetName);

    // Broadcast live selection to other players via WebSocket
    if (stompConnected.current) {
      try {
        send(`/app/games/${gameCode}/banish-selection`, { targetId: newSelected, targetName: targetName || null });
      } catch (e) { /* ignore if not yet connected */ }
    }
  };

  const selectedCandidate = candidates.find(c => c.id === selected);
  const canSubmit = selected !== null && nameInput.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      await castBanishVote(gameCode, playerCode, selected);
      setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Vote failed');
    }
  };

  const otherPlayersList = Object.values(otherPlayers);

  // For each candidate, which other players have selected them?
  const votersByCandidateId = {};
  for (const v of otherPlayersList) {
    if (v.targetId) {
      if (!votersByCandidateId[v.targetId]) votersByCandidateId[v.targetId] = [];
      votersByCandidateId[v.targetId].push({ voterName: v.voterName, submitted: v.submitted });
    }
  }

  const OthersPanel = () => (
    <div style={{ marginTop: '1.5rem', textAlign: 'left' }}>
      <h3 style={{ textAlign: 'center', color: 'inherit' }}>Other Players' Selections</h3>
      {otherPlayersList.length === 0
        ? <p style={{ textAlign: 'center', color: '#888' }}>No selections yet</p>
        : otherPlayersList.map(v => (
          <div key={v.voterName} style={{
            padding: '0.5rem 0.75rem',
            marginBottom: '0.5rem',
            background: v.submitted ? '#1a3a1a' : '#38383d',
            color: 'white',
            borderRadius: '6px',
            border: v.submitted ? '1px solid #4CAF50' : '1px solid #888',
          }}>
            <strong>{v.voterName}</strong>{v.submitted ? ' ✓' : ''}:{' '}
            {v.targetName
              ? v.targetName
              : <em style={{ color: '#bbb' }}>none selected</em>
            }
          </div>
        ))
      }
    </div>
  );

  if (submitted) {
    return (
      <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center' }}>
        <GameStatusBar gameCode={gameCode} playerCode={playerCode} />
        <h2>Vote Cast</h2>
        <p>Your banishment vote has been recorded.</p>
        <OthersPanel />
        <button onClick={() => navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)}>Back to Menu</button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center' }}>
      <GameStatusBar gameCode={gameCode} playerCode={playerCode} />
      <h2>Banish Vote</h2>
      <p>Select a player to banish:</p>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <div style={{ marginBottom: '1rem' }}>
        {candidates.map(p => (
          <VoteCard
            key={p.id}
            player={p}
            selected={selected === p.id}
            onSelect={toggleSelect}
            voters={votersByCandidateId[p.id] || []}
          />
        ))}
      </div>
      <div style={{ marginBottom: '1rem' }}>
        <input
          type="text"
          placeholder={selected ? 'Confirm player name...' : 'Select a player first'}
          value={nameInput}
          onChange={e => setNameInput(e.target.value)}
          disabled={!selected}
          style={{
            padding: '0.5rem',
            width: '100%',
            boxSizing: 'border-box',
            borderRadius: '6px',
            border: selected ? '2px solid #4CAF50' : '2px solid #eee',
            fontSize: '1rem',
          }}
        />
      </div>
      <button onClick={handleSubmit} disabled={!canSubmit} style={{ padding: '0.5rem 2rem' }}>
        Submit Vote
      </button>
      <OthersPanel />
    </div>
  );
}

export default BanishVotePage;
