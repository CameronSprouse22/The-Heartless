import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGameState, startGame, invitePlayer } from '../services/api';
import PlayerList from '../components/PlayerList';

function LobbyPage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const [gameState, setGameState] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [inviteName, setInviteName] = useState('');
  const [inviteContact, setInviteContact] = useState('');
  const [showAddPlayer, setShowAddPlayer] = useState(false);

  const playerCode = localStorage.getItem('playerCode');

  const loadState = useCallback(async () => {
    try {
      const state = await getGameState(gameCode, playerCode);
      setGameState(state);

      // If game has started, navigate to menu
      if (state.gameStatus === 'START' || state.gameStatus === 'END') {
        const pName = localStorage.getItem('playerName') || '';
        navigate(`/menu/${gameCode}/${encodeURIComponent(pName)}`);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [gameCode, playerCode, navigate]);

  useEffect(() => {
    loadState();
    // Poll every 3 seconds for lobby updates
    const interval = setInterval(loadState, 3000);
    return () => clearInterval(interval);
  }, [loadState]);

  const handleStartGame = async () => {
    setError('');
    setMessage('');
    try {
      const result = await startGame(gameCode, playerCode);
      setMessage(result.message);
      const pName = localStorage.getItem('playerName') || '';
      setTimeout(() => navigate(`/menu/${gameCode}/${encodeURIComponent(pName)}`), 1500);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleInvite = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await invitePlayer(gameCode, playerCode, inviteName, inviteContact);
      setMessage(`Invited ${inviteName}`);
      setInviteName('');
      setInviteContact('');
      loadState();
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading lobby...</div>;
  }

  const isVip = gameState && gameState.players && gameState.players.length > 0 &&
    gameState.players[0].id === getMyPlayerId(gameState.players, playerCode);
  const canStart = gameState && gameState.playerCount >= 4;
  const activeCount = gameState ? gameState.playerCount : 0;

  return (
    <div style={{ padding: '1rem', maxWidth: '500px', margin: '0 auto' }}>
      <h1>Game Lobby</h1>

      <div style={{ background: '#f0f0f0', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
        <p><strong>Game Code:</strong> {gameCode}</p>
        <p><strong>Status:</strong> {gameState?.gameStatus || 'Unknown'}</p>
        <p><strong>Active Players:</strong> {activeCount}</p>
        {activeCount < 4 && (
          <p style={{ color: 'orange' }}>Need at least 4 players to start ({4 - activeCount} more needed)</p>
        )}
      </div>

      {gameState && gameState.players && (
        <PlayerList
          players={gameState.players}
          vipId={gameState.players.length > 0 ? gameState.players[0].id : null}
        />
      )}

      {activeCount >= 1 && (
        <div style={{ marginTop: '1rem', marginBottom: '1rem' }}>
          {!showAddPlayer ? (
            <button
              onClick={() => setShowAddPlayer(true)}
              style={{
                width: '100%',
                padding: '0.5rem',
                background: '#2196F3',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
              }}
            >
              + Add Player
            </button>
          ) : (
            <form onSubmit={handleInvite} style={{ background: '#e3f2fd', padding: '0.75rem', borderRadius: '8px' }}>
              <h3 style={{ margin: '0 0 0.5rem 0' }}>Add Player</h3>
              <input
                type="text"
                placeholder="Player name"
                value={inviteName}
                onChange={(e) => setInviteName(e.target.value)}
                required
                style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem', boxSizing: 'border-box' }}
              />
              <input
                type="text"
                placeholder="Email or phone"
                value={inviteContact}
                onChange={(e) => setInviteContact(e.target.value)}
                required
                style={{ width: '100%', padding: '0.5rem', marginBottom: '0.5rem', boxSizing: 'border-box' }}
              />
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" style={{ flex: 1, padding: '0.5rem', background: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                  Send Invite
                </button>
                <button type="button" onClick={() => setShowAddPlayer(false)} style={{ flex: 1, padding: '0.5rem', background: '#999', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      <div style={{ marginTop: '1rem' }}>
        <button
          onClick={handleStartGame}
          disabled={!canStart}
          style={{
            width: '100%',
            padding: '0.75rem',
            fontSize: '1.1rem',
            background: canStart ? '#4CAF50' : '#ccc',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: canStart ? 'pointer' : 'not-allowed',
          }}
        >
          {canStart ? 'Start Game' : `Waiting for players (${activeCount}/4 minimum)`}
        </button>
      </div>

      <button
        onClick={() => navigate('/')}
        style={{ width: '100%', padding: '0.5rem', marginTop: '0.5rem', background: 'transparent', border: '1px solid #ccc', borderRadius: '4px' }}
      >
        Back to Home
      </button>

      {message && <p style={{ color: 'green', marginTop: '1rem' }}>{message}</p>}
      {error && <p style={{ color: 'red', marginTop: '1rem' }}>{error}</p>}
    </div>
  );
}

// Helper: we don't have a direct player ID mapping client-side,
// so VIP detection is done by checking if first player matches
function getMyPlayerId() {
  // In this simple implementation, VIP start button is always shown.
  // The server enforces VIP-only access and returns 403 if non-VIP tries.
  return null;
}

export default LobbyPage;
