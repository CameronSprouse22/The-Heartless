import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import VoteCard from '../components/VoteCard';
import ChatWindow from '../components/ChatWindow';
import { getMurderCandidates, castMurderVote } from '../services/api';
import { connect, disconnect, subscribe, send } from '../services/websocket';

function MurderVotePage({ onClose }) {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const [candidates, setCandidates] = useState([]);
  // Single selection: one player ID or null
  const [selected, setSelected] = useState(null);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  // coTraitors: [{id, name}] — other active traitors (needed for consensus check)
  const [coTraitors, setCoTraitors] = useState([]);
  // otherPlayers: { [voterId]: { voterName, targetIds, targetNames, submitted } }
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
          // once submitted, never revert to unsubmitted
          submitted: isSubmitted || (existing && existing.submitted),
        },
      };
    });
  };

  // Initial load + polling every 3s
  useEffect(() => {
    if (!playerCode) { navigate('/'); return; }
    const load = () =>
      getMurderCandidates(gameCode, playerCode)
        .then(res => {
          setCandidates(res.candidates || []);
          if (res.coTraitors) setCoTraitors(res.coTraitors);
          if (res.existingVotes && res.existingVotes.length > 0) {
            setSelected(res.existingVotes[0]);
            setSubmitted(true);
          }
          (res.othersVotes || []).forEach(v =>
            updateOtherPlayer(v.voterId, v.voterName, v.targetIds || [], v.targetNames || [], v.submitted !== false)
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

  const handleSelect = (id) => {
    // Single-select: clicking the same card deselects it
    const newSelected = selected === id ? null : id;
    setSelected(newSelected);

    // Broadcast live selection to other traitors via WebSocket
    if (stompConnected.current) {
      const targetNames = newSelected
        ? [candidates.find(c => c.id === newSelected)?.name || newSelected]
        : [];
      try {
        send(`/app/games/${gameCode}/murder-selection`, {
          targetIds: newSelected ? [newSelected] : [],
          targetNames,
        });
      } catch (e) { /* ignore if not yet connected */ }
    }
  };

  const handleSubmit = async () => {
    if (!selected || !consensusReached) return;
    try {
      await castMurderVote(gameCode, playerCode, [selected]);
      setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Vote failed');
    }
  };

  // Consensus: I must have a selection AND every co-traitor must have chosen the same target
  const consensusReached = selected !== null && coTraitors.every(t => {
    const vote = otherPlayers[t.id];
    return vote && vote.targetIds.length === 1 && vote.targetIds[0] === selected;
  });

  // Build per-candidate voter list for VoteCard display
  const votersByCandidateId = {};
  for (const v of Object.values(otherPlayers)) {
    for (const tid of (v.targetIds || [])) {
      if (!votersByCandidateId[tid]) votersByCandidateId[tid] = [];
      votersByCandidateId[tid].push({ voterName: v.voterName, submitted: v.submitted });
    }
  }

  const myTargetName = selected ? candidates.find(c => c.id === selected)?.name : null;

  const statusMessage = () => {
    if (coTraitors.length === 0) {
      return selected ? `You selected: ${myTargetName}` : 'Select a target';
    }
    if (!selected) return 'Select a target to murder';
    if (consensusReached) return `All traitors agree on ${myTargetName} — submit now!`;
    const waiting = coTraitors.filter(t => {
      const vote = otherPlayers[t.id];
      return !vote || vote.targetIds.length !== 1 || vote.targetIds[0] !== selected;
    });
    return `Waiting for: ${waiting.map(t => t.name).join(', ')}`;
  };

  const headerStyle = {
    padding: '0.4rem 1rem',
    background: '#3a1a1a',
    color: '#EF9A9A',
    fontWeight: 'bold',
    fontSize: '0.85rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    flexShrink: 0,
  };

  // Inline chat section — NOT a nested component so ChatWindow is never unmounted on re-renders
  const chatSection = (
    <>
      <div style={{ borderTop: '2px solid #333' }} />
      <div style={{ padding: '0.4rem 0.75rem', background: '#1a1a2e', color: '#9fa8da', fontWeight: 'bold', fontSize: '0.8rem' }}>
        Traitor Chat
      </div>
      <div style={{ height: '40vh' }}>
        <ChatWindow gameCode={gameCode} playerCode={playerCode} channel="traitors" />
      </div>
    </>
  );

  if (submitted) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', color: '#e0e0e0' }}>
        <div style={headerStyle}>
          Murder Vote
        </div>
        <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center', padding: '1rem' }}>
          <h2>Vote Cast</h2>
          <p>Your murder vote has been recorded.</p>
          <button onClick={() => onClose ? onClose() : navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)}>Back to Menu</button>
        </div>
        {chatSection}
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', color: '#e0e0e0' }}>
      {/* Header */}
      <div style={headerStyle}>
        Murder Vote
      </div>

      {/* Vote section */}
      <div style={{ padding: '1rem', maxWidth: 400, margin: '0 auto', width: '100%', textAlign: 'center' }}>
        <h2 style={{ margin: '0 0 0.5rem' }}>Select who to murder</h2>
        {error && <p style={{ color: 'red' }}>{error}</p>}

        {/* Consensus status indicator */}
        <div style={{
          marginBottom: '0.75rem',
          padding: '0.4rem 0.75rem',
          background: consensusReached ? '#1a3a1a' : '#2a2a2a',
          border: `1px solid ${consensusReached ? '#4CAF50' : '#555'}`,
          borderRadius: '6px',
          fontSize: '0.85rem',
          color: consensusReached ? '#A5D6A7' : '#bbb',
        }}>
          {statusMessage()}
        </div>

        {/* Candidate cards */}
        <div style={{ marginBottom: '1rem' }}>
          {candidates.map(p => (
            <VoteCard
              key={p.id}
              player={p}
              selected={selected === p.id}
              onSelect={handleSelect}
              voters={votersByCandidateId[p.id] || []}
            />
          ))}
        </div>

        {/* Submit button — only enabled when all traitors agree */}
        <button
          onClick={handleSubmit}
          disabled={!selected || !consensusReached}
          style={{
            padding: '0.6rem 2rem',
            background: (selected && consensusReached) ? '#2e7d32' : '#616161',
            color: (selected && consensusReached) ? 'white' : '#9e9e9e',
            border: 'none',
            borderRadius: '6px',
            fontSize: '1rem',
            fontWeight: 600,
            cursor: (selected && consensusReached) ? 'pointer' : 'not-allowed',
            marginBottom: '0.5rem',
          }}
        >
          Submit Vote
        </button>
      </div>

      {/* Traitor Chat embedded below the vote */}
      {chatSection}
    </div>
  );
}

export default MurderVotePage;

