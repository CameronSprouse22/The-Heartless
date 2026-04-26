import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getGameState } from '../services/api';
import ChatWindow from '../components/ChatWindow';

function IndividualChatPage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
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
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ padding: '0.4rem 1rem', background: '#2a1a3a', color: '#CE93D8', fontWeight: 'bold', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        {onClose && (
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#CE93D8', cursor: 'pointer', fontSize: '1rem', padding: 0 }}>← Back</button>
        )}
        Individual Chat
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
