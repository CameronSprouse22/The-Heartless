import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGameState, startGame, invitePlayer, markLobbyReady, markLobbyNotReady, startGameWithUnconfirmed } from '../services/api';

const STATUS_LABELS = {
  INVITE_PENDING: { label: 'Invite Not Responded To', color: '#999', bg: '#f5f5f5' },
  NOT_READY: { label: 'Not Ready', color: '#e65100', bg: '#fff3e0' },
  READY: { label: 'Ready ✓', color: '#2e7d32', bg: '#e8f5e9' },
  DISCONNECTED: { label: 'Disconnected', color: '#757575', bg: '#eeeeee' },
};

function PlayerStatusBadge({ lobbyStatus }) {
  const s = STATUS_LABELS[lobbyStatus] || { label: lobbyStatus, color: '#555', bg: '#eee' };
  return (
    <span style={{
      fontSize: '0.75rem',
      fontWeight: 600,
      padding: '2px 8px',
      borderRadius: '12px',
      color: s.color,
      background: s.bg,
      border: `1px solid ${s.color}33`,
    }}>
      {s.label}
    </span>
  );
}

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
    const interval = setInterval(loadState, 3000);
    return () => clearInterval(interval);
  }, [loadState]);

  const handleReady = async () => {
    setError('');
    setMessage('');
    try {
      await markLobbyReady(gameCode, playerCode);
      setMessage('You are now marked as Ready!');
      loadState();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleNotReady = async () => {
    setError('');
    setMessage('');
    try {
      await markLobbyNotReady(gameCode, playerCode);
      setMessage('');
      loadState();
    } catch (err) {
      setError(err.message);
    }
  };

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

  const handleStartWithUnconfirmed = async () => {
    setError('');
    setMessage('');
    try {
      const result = await startGameWithUnconfirmed(gameCode, playerCode);
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
      setShowAddPlayer(false);
      loadState();
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading lobby...</div>;
  }

  const players = gameState?.players || [];
  const invitedCount = gameState?.invitedCount ?? players.length;
  const readyCount = gameState?.readyCount ?? 0;
  const minPlayers = gameState?.minPlayers ?? 0;
  const maxPlayers = gameState?.maxPlayers ?? Infinity;
  const isVip = gameState?.isVip ?? false;
  const isReady = gameState?.isReady ?? false;
  const myPlayerId = gameState?.myPlayerId;

  const enoughInvited = invitedCount >= minPlayers;
  const atMaxPlayers = invitedCount >= maxPlayers;

  // "Start Game" requires all active players ready and no pending invites
  const pendingCount = players.filter(p => p.lobbyStatus === 'INVITE_PENDING').length;
  const notReadyCount = players.filter(p => p.lobbyStatus === 'NOT_READY').length;
  const canStartGame = enoughInvited && pendingCount === 0 && notReadyCount === 0;

  // "Start With Unconfirmed" just needs min players invited
  const canStartWithUnconfirmed = enoughInvited;

  // VIP is always index 0
  const vipId = players.length > 0 ? players[0].id : null;

  return (
    <div style={{ padding: '1rem', maxWidth: '520px', margin: '0 auto' }}>
      <h1 style={{ margin: '0 0 1rem 0' }}>Game Lobby</h1>

      {/* Game info card */}
      <div style={{ background: '#f0f0f0', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
        <p style={{ margin: '0 0 0.25rem 0' }}><strong>Game Code:</strong> {gameCode}</p>
        <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
          <span>
            <strong>Invited:</strong> {invitedCount}
            <span style={{ color: '#888', fontSize: '0.85rem' }}> / {maxPlayers} max</span>
          </span>
          <span>
            <strong>Ready:</strong>{' '}
            <span style={{ color: readyCount >= invitedCount && invitedCount > 0 ? '#2e7d32' : '#e65100' }}>
              {readyCount} / {invitedCount}
            </span>
          </span>
        </div>
        {!enoughInvited && (
          <p style={{ color: '#e65100', margin: '0.5rem 0 0 0', fontSize: '0.9rem' }}>
            Need at least {minPlayers} players invited ({minPlayers - invitedCount} more needed to enable Start)
          </p>
        )}
      </div>

      {/* Player list */}
      <div style={{ marginBottom: '1rem' }}>
        <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem' }}>Players</h3>
        {players.length === 0 ? (
          <p style={{ color: '#888', fontStyle: 'italic' }}>No players yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            {players.map((p) => (
              <div
                key={p.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.5rem 0.75rem',
                  borderRadius: '6px',
                  background: p.id === myPlayerId ? '#e3f2fd' : '#fafafa',
                  border: p.id === myPlayerId ? '1px solid #90caf9' : '1px solid #e0e0e0',
                }}
              >
                <span style={{ fontWeight: p.id === myPlayerId ? 700 : 400 }}>
                  {p.name}
                  {p.id === vipId && (
                    <span style={{ marginLeft: '6px', fontSize: '0.7rem', color: '#f57c00', fontWeight: 700 }}>
                      VIP
                    </span>
                  )}
                  {p.id === myPlayerId && (
                    <span style={{ marginLeft: '4px', fontSize: '0.7rem', color: '#1565c0' }}>(you)</span>
                  )}
                </span>
                <PlayerStatusBadge lobbyStatus={p.lobbyStatus} />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Ready / Not Ready toggle */}
      {!isReady ? (
        <button
          onClick={handleReady}
          style={{
            width: '100%',
            padding: '0.75rem',
            marginBottom: '0.5rem',
            fontSize: '1rem',
            background: '#2e7d32',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          ✓ I'm Ready
        </button>
      ) : (
        <button
          onClick={handleNotReady}
          style={{
            width: '100%',
            padding: '0.75rem',
            marginBottom: '0.5rem',
            fontSize: '1rem',
            background: '#e8f5e9',
            color: '#2e7d32',
            border: '2px solid #a5d6a7',
            borderRadius: '6px',
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          ✓ Ready — click to set Not Ready
        </button>
      )}

      {/* Add player (VIP only, up to max) */}
      {isVip && !atMaxPlayers && (
        <div style={{ marginBottom: '1rem' }}>
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
      {isVip && atMaxPlayers && (
        <p style={{ color: '#888', fontSize: '0.85rem', textAlign: 'center', marginBottom: '0.5rem' }}>
          Maximum players ({maxPlayers}) reached.
        </p>
      )}

      {/* VIP start buttons */}
      {isVip && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '0.5rem' }}>
          <button
            onClick={handleStartGame}
            disabled={!canStartGame}
            title={
              !enoughInvited ? `Need at least ${minPlayers} players invited`
              : pendingCount > 0 ? `${pendingCount} player(s) haven't accepted their invite`
              : notReadyCount > 0 ? `${notReadyCount} player(s) are not ready`
              : 'Start game'
            }
            style={{
              padding: '0.75rem',
              fontSize: '1rem',
              fontWeight: 700,
              background: canStartGame ? '#4CAF50' : '#ccc',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: canStartGame ? 'pointer' : 'not-allowed',
            }}
          >
            🚀 Start Game
          </button>
          <button
            onClick={handleStartWithUnconfirmed}
            disabled={!canStartWithUnconfirmed}
            title={
              !enoughInvited ? `Need at least ${minPlayers} players invited`
              : 'Start with players who have joined (pending invites will be dropped)'
            }
            style={{
              padding: '0.65rem',
              fontSize: '0.9rem',
              background: canStartWithUnconfirmed ? '#ff7043' : '#ccc',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: canStartWithUnconfirmed ? 'pointer' : 'not-allowed',
            }}
          >
            ⚡ Start Game With Unconfirmed Players
          </button>
        </div>
      )}

      <button
        onClick={() => navigate('/')}
        style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem', background: 'transparent', border: '1px solid #ccc', borderRadius: '4px' }}
      >
        Back to Home
      </button>

      {message && <p style={{ color: 'green', marginTop: '1rem' }}>{message}</p>}
      {error && <p style={{ color: 'red', marginTop: '1rem' }}>{error}</p>}
    </div>
  );
}

export default LobbyPage;

