import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';
import VoteCard from '../components/VoteCard';
import { getMurderCandidates, castMurderVote } from '../services/api';
import { connect, disconnect, subscribe, send } from '../services/websocket';

function MurderVotePage() {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();
  const playerCode = localStorage.getItem('playerCode');
  const [candidates, setCandidates] = useState([]);
  const [selected, setSelected] = useState([]);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  // otherPlayers: { [voterId]: { voterName, targetNames, submitted } }
  const [otherPlayers, setOtherPlayers] = useState({});
  const stompConnected = useRef(false);

  const updateOtherPlayer = (voterId, voterName, targetIds, targetNames, isSubmitted) => {
    setOtherPlayers(prev => {
      const existing = prev[voterId];
      return {
        ...prev,
        [voterId]: {
          voterName,
          targetIds: targetIds || [],
          targetNames: targetNames || [],
          // once submitted, never go back to unsubmitted
          submitted: isSubmitted || (existing && existing.submitted),
        },
      };
    });
  };

  // Initial load + polling every 3s for submitted votes
  useEffect(() => {
    if (!playerCode) { navigate('/'); return; }
    const load = () =>
      getMurderCandidates(gameCode, playerCode)
        .then(res => {
          setCandidates(res.candidates || []);
          (res.othersVotes || []).forEach(v =>
            updateOtherPlayer(v.voterId, v.voterName, v.targetIds || [], v.targetNames || [], true)
          );
        })
        .catch(err => setError(err.message || 'Failed to load'));
    load();
    const interval = setInterval(load, 3000);
    return () => clearInterval(interval);
  }, [gameCode, playerCode, navigate]);

  // WebSocket: receive submitted votes + live selections from other traitors
  useEffect(() => {
    if (!playerCode) return;
    connect(playerCode, () => {
      stompConnected.current = true;
      subscribe(`/topic/games/${gameCode}/murder-vote`, (msg) => {
        if (msg.type === 'MURDER_VOTE_UPDATE' || msg.type === 'MURDER_SELECTION_UPDATE') {
          updateOtherPlayer(
            msg.voterId,
            msg.voterName,
            msg.targetIds || [],
            msg.targetNames || [],
            msg.type === 'MURDER_VOTE_UPDATE'
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
    const newSelected = selected.includes(id)
      ? selected.filter(s => s !== id)
      : [...selected, id];
    setSelected(newSelected);

    // Broadcast live selection to other traitors via WebSocket
    if (stompConnected.current) {
      const targetNames = newSelected.map(tid => {
        const c = candidates.find(c => c.id === tid);
        return c ? c.name : tid;
      });
      try {
        send(`/app/games/${gameCode}/murder-selection`, { targetIds: newSelected, targetNames });
      } catch (e) { /* ignore if not yet connected */ }
    }
  };

  const handleSubmit = async () => {
    if (selected.length === 0) return;
    try {
      await castMurderVote(gameCode, playerCode, selected);
      setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Vote failed');
    }
  };

  const otherPlayersList = Object.values(otherPlayers);

  // For each candidate, which other players have selected them?
  const votersByCandidateId = {};
  for (const v of otherPlayersList) {
    for (const tid of (v.targetIds || [])) {
      if (!votersByCandidateId[tid]) votersByCandidateId[tid] = [];
      votersByCandidateId[tid].push({ voterName: v.voterName, submitted: v.submitted });
    }
  }

  const OthersPanel = () => (
    <div style={{ marginTop: '1.5rem', textAlign: 'left' }}>
      <h3 style={{ textAlign: 'center', color: 'inherit' }}>Other Traitors' Selections</h3>
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
            <strong>{v.voterName}</strong>{v.submitted ? ' âœ“' : ''}:{' '}
            {v.targetNames.length > 0
              ? v.targetNames.join(', ')
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
        <p>Your murder vote has been recorded.</p>
        <OthersPanel />
        <button onClick={() => navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)}>Back to Menu</button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center' }}>
      <GameStatusBar gameCode={gameCode} playerCode={playerCode} />
      <h2>Murder Vote</h2>
      <p>Select target(s) to eliminate:</p>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <div style={{ marginBottom: '1rem' }}>
        {candidates.map(p => (
          <VoteCard
            key={p.id}
            player={p}
            selected={selected.includes(p.id)}
            onSelect={toggleSelect}
            voters={votersByCandidateId[p.id] || []}
          />
        ))}
      </div>
      <button onClick={handleSubmit} disabled={selected.length === 0} style={{ padding: '0.5rem 2rem' }}>
        Submit Vote
      </button>
      <OthersPanel />
    </div>
  );
}

export default MurderVotePage;

