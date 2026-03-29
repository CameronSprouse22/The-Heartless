import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';
import VoteCard from '../components/VoteCard';
import { getBanishCandidates, castBanishVote } from '../services/api';

function BanishVotePage() {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();
  const playerCode = localStorage.getItem('playerCode');
  const [candidates, setCandidates] = useState([]);
  const [selected, setSelected] = useState(null);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!playerCode) { navigate('/'); return; }
    getBanishCandidates(gameCode, playerCode)
      .then(res => setCandidates(res.data))
      .catch(() => setError('Failed to load candidates'));
  }, [gameCode, playerCode, navigate]);

  const handleSubmit = async () => {
    if (!selected) return;
    try {
      await castBanishVote(gameCode, playerCode, selected);
      setSubmitted(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Vote failed');
    }
  };

  if (submitted) {
    return (
      <div style={{ maxWidth: 400, margin: '2rem auto', textAlign: 'center' }}>
        <GameStatusBar gameCode={gameCode} playerCode={playerCode} />
        <h2>Vote Cast</h2>
        <p>Your banishment vote has been recorded.</p>
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
          <VoteCard key={p.id} player={p} selected={selected === p.id} onSelect={setSelected} />
        ))}
      </div>
      <button onClick={handleSubmit} disabled={!selected} style={{ padding: '0.5rem 2rem' }}>
        Submit Vote
      </button>
    </div>
  );
}

export default BanishVotePage;
