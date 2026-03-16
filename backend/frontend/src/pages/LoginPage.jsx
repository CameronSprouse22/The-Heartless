import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createGame, createTestGame } from '../services/api';

function LoginPage() {
  const navigate = useNavigate();
  const [playerName, setPlayerName] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const [error, setError] = useState('');

  const handleCreateGame = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const result = await createGame(playerName);
      localStorage.setItem('playerCode', result.playerCode);
      localStorage.setItem('gameCode', result.gameCode);
      localStorage.setItem('playerName', playerName);
      navigate(`/lobby/${result.gameCode}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleJoinGame = () => {
    if (joinCode.trim()) {
      navigate(`/join/${joinCode.trim()}`);
    }
  };

  const handleTestSetup = async () => {
    setError('');
    try {
      const result = await createTestGame();
      // Store VIP (Player 1) credentials
      localStorage.setItem('playerCode', result.vipPlayerCode);
      localStorage.setItem('gameCode', result.gameCode);
      localStorage.setItem('playerName', 'Player 1');
      // Store all player codes for easy switching
      localStorage.setItem('testPlayers', JSON.stringify(result.players));
      navigate(`/lobby/${result.gameCode}`);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div style={{ padding: '1rem', maxWidth: '400px', margin: '0 auto' }}>
      <h1>The Heartless</h1>

      <form onSubmit={handleCreateGame}>
        <h2>Create Game</h2>
        <input
          type="text"
          placeholder="Your name"
          value={playerName}
          onChange={(e) => setPlayerName(e.target.value)}
          required
          style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem' }}
        />
        <button type="submit" style={{ width: '100%', padding: '0.5rem' }}>
          Create Game
        </button>
      </form>

      <hr style={{ margin: '1rem 0' }} />

      <h2>Join a Game</h2>
      <input
        type="text"
        placeholder="Enter game code"
        value={joinCode}
        onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
        style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem' }}
      />
      <button onClick={handleJoinGame} style={{ width: '100%', padding: '0.5rem' }}>
        Join Game
      </button>

      <hr style={{ margin: '1rem 0' }} />

      <h2>Quick Test</h2>
      <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' }}>
        Creates a game with 7 players (1@gmail.com – 7@gmail.com), all active and ready to start.
      </p>
      <button
        onClick={handleTestSetup}
        style={{
          width: '100%',
          padding: '0.5rem',
          background: '#FF9800',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontWeight: 'bold',
        }}
      >
        Create Test Game (7 Players)
      </button>

      {error && <p style={{ color: 'red', marginTop: '1rem' }}>{error}</p>}
    </div>
  );
}

export default LoginPage;
