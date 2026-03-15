import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createGame, invitePlayer } from '../services/api';

function LoginPage() {
  const navigate = useNavigate();
  const [playerName, setPlayerName] = useState('');
  const [gameCode, setGameCode] = useState('');
  const [playerCode, setPlayerCode] = useState('');
  const [inviteName, setInviteName] = useState('');
  const [inviteContact, setInviteContact] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleCreateGame = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const result = await createGame(playerName);
      setGameCode(result.gameCode);
      setPlayerCode(result.playerCode);
      localStorage.setItem('playerCode', result.playerCode);
      localStorage.setItem('gameCode', result.gameCode);
      localStorage.setItem('playerName', playerName);
      setMessage(`Game created! Code: ${result.gameCode}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleInvite = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await invitePlayer(gameCode, playerCode, inviteName, inviteContact);
      setMessage(`Invited ${inviteName} via ${inviteContact}`);
      setInviteName('');
      setInviteContact('');
    } catch (err) {
      setError(err.message);
    }
  };

  const handleGoToLobby = () => {
    navigate(`/lobby/${gameCode}`);
  };

  const handleJoinGame = () => {
    if (joinCode.trim()) {
      navigate(`/join/${joinCode.trim()}`);
    }
  };

  return (
    <div style={{ padding: '1rem', maxWidth: '400px', margin: '0 auto' }}>
      <h1>The Heartless</h1>

      {!gameCode && (
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
      )}

      {gameCode && (
        <>
          <div style={{ background: '#f0f0f0', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
            <p><strong>Game Code:</strong> {gameCode}</p>
            <button onClick={handleGoToLobby} style={{ width: '100%', padding: '0.5rem' }}>
              Go to Lobby
            </button>
          </div>

          <form onSubmit={handleInvite}>
            <h2>Invite Player</h2>
            <input
              type="text"
              placeholder="Player name"
              value={inviteName}
              onChange={(e) => setInviteName(e.target.value)}
              required
              style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem' }}
            />
            <input
              type="text"
              placeholder="Email or phone"
              value={inviteContact}
              onChange={(e) => setInviteContact(e.target.value)}
              required
              style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem' }}
            />
            <button type="submit" style={{ width: '100%', padding: '0.5rem' }}>
              Send Invitation
            </button>
          </form>
        </>
      )}

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

      {message && <p style={{ color: 'green', marginTop: '1rem' }}>{message}</p>}
      {error && <p style={{ color: 'red', marginTop: '1rem' }}>{error}</p>}
    </div>
  );
}

export default LoginPage;
