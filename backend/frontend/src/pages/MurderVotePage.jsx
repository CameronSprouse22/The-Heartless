import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';
import VoteCard from '../components/VoteCard';
import { getMurderCandidates, castMurderVote } from '../services/api';

function MurderVotePage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const playerCode = localStorage.getItem('playerCode');
  const playerName = localStorage.getItem('playerName') || '';
  const [candidates, setCandidates] = useState([]);
  const [selected, setSelected] = useState([]);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!playerCode) { navigate('/'); return; }
    getMurderCandidates(gameCode, playerCode)
      .then(res => setCandidates(res.data))
      .catch(err => {
        if (err.response?.status === 403) {
          setError('Only traitors can access the murder vote.');
        } else {
          setError('Failed to load candidates');
        }
      });
  }, [gameCode, playerCode, navigate]);

  const toggleSelect = (id) => {
    setSelected(prev =>
      prev.includes(id) ? prev.filter(s => s !== id) : [...prev, id]
    );
  };

  const handleSubmit = async () => {
    if (selected.length === 0) return;
    try {
      await castMurderVote(gameCode, playerCode, selected);
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
        <p>Your murder vote has been recorded.</p>
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
          <VoteCard key={p.id} player={p} selected={selected.includes(p.id)} onSelect={toggleSelect} />
        ))}
      </div>
      <button onClick={handleSubmit} disabled={selected.length === 0} style={{ padding: '0.5rem 2rem' }}>
        Submit Vote
      </button>
    </div>
  );
}

export default MurderVotePage;
