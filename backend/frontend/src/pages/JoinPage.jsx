import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getJoinInfo, joinGame } from '../services/api';

function JoinPage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const [gameInfo, setGameInfo] = useState(null);
  const [name, setName] = useState('');
  const [contact, setContact] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadInfo() {
      try {
        const info = await getJoinInfo(gameCode);
        setGameInfo(info);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
    loadInfo();
  }, [gameCode]);

  const handleJoin = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      const result = await joinGame(gameCode, name, contact);
      localStorage.setItem('playerCode', result.playerCode);
      localStorage.setItem('gameCode', gameCode);
      localStorage.setItem('playerName', name);
      setMessage('Joined successfully! Heading to lobby...');
      setTimeout(() => navigate(`/lobby/${gameCode}`), 1000);
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>;
  }

  return (
    <div style={{ padding: '1rem', maxWidth: '400px', margin: '0 auto' }}>
      <h1>Join Game</h1>

      {gameInfo && (
        <div style={{ background: '#f0f0f0', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
          <p><strong>Game Code:</strong> {gameCode}</p>
          <p><strong>Host:</strong> {gameInfo.hostName}</p>
          <p><strong>Players:</strong> {gameInfo.playerCount}</p>
          <p><strong>Status:</strong> {gameInfo.status}</p>
        </div>
      )}

      <form onSubmit={handleJoin}>
        <input
          type="text"
          placeholder="Your name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem' }}
        />
        <input
          type="text"
          placeholder="Email or phone (must match invitation)"
          value={contact}
          onChange={(e) => setContact(e.target.value)}
          required
          style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem' }}
        />
        <button type="submit" style={{ width: '100%', padding: '0.5rem' }}>
          Join Game
        </button>
      </form>

      <button
        onClick={() => navigate('/')}
        style={{ width: '100%', padding: '0.5rem', marginTop: '1rem', background: 'transparent', border: '1px solid #ccc' }}
      >
        Back to Home
      </button>

      {message && <p style={{ color: 'green', marginTop: '1rem' }}>{message}</p>}
      {error && <p style={{ color: 'red', marginTop: '1rem' }}>{error}</p>}
    </div>
  );
}

export default JoinPage;
