import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGameState } from '../services/api';
import ChatWindow from '../components/ChatWindow';

function IndividualChatPage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const playerName = localStorage.getItem('playerName') || '';
  const [players, setPlayers] = useState([]);
  const [selectedPlayer, setSelectedPlayer] = useState('');

  useEffect(() => {
    async function loadPlayers() {
      try {
        const state = await getGameState(gameCode, playerCode);
        const alivePlayers = state.players.filter(p => !p.isDead && p.id !== playerCode);
        setPlayers(alivePlayers);
      } catch (err) {
        // ignore
      }
    }
    loadPlayers();
  }, [gameCode, playerCode]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div style={{ padding: '0.5rem', background: '#9C27B0', color: 'white', display: 'flex', justifyContent: 'space-between' }}>
        <button onClick={() => navigate(`/menu/${gameCode}/${encodeURIComponent(playerName)}`)} style={{ background: 'transparent', color: 'white', border: 'none', cursor: 'pointer' }}>← Back</button>
        <span>Individual Chat</span>
        <span></span>
      </div>

      <div style={{ padding: '0.5rem' }}>
        <select
          value={selectedPlayer}
          onChange={(e) => setSelectedPlayer(e.target.value)}
          style={{ width: '100%', padding: '0.5rem' }}
        >
          <option value="">Select a player...</option>
          {players.map(p => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
      </div>

      <div style={{ flex: 1 }}>
        {selectedPlayer ? (
          <ChatWindow
            gameCode={gameCode}
            playerCode={playerCode}
            channel="individual"
            recipientId={selectedPlayer}
          />
        ) : (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#999' }}>
            Select a player to start chatting
          </div>
        )}
      </div>
    </div>
  );
}

export default IndividualChatPage;
