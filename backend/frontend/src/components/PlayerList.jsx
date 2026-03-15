import React from 'react';

function PlayerList({ players, vipId }) {
  if (!players || players.length === 0) {
    return <p>No players yet.</p>;
  }

  return (
    <div>
      <h3>Players ({players.length})</h3>
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {players.map((player) => (
          <li
            key={player.id}
            style={{
              padding: '0.5rem',
              marginBottom: '0.25rem',
              background: player.isDead ? '#ffcccc' : '#f0f0f0',
              borderRadius: '4px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <span>
              {player.name}
              {player.id === vipId && ' (VIP)'}
            </span>
            <span style={{
              fontSize: '0.8rem',
              color: player.status === 'ACTIVE' ? 'green' :
                     player.status === 'PENDING' ? 'orange' :
                     player.status === 'DISCONNECTED' ? 'gray' : 'red',
            }}>
              {player.status}
              {player.isDead && ' - Dead'}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default PlayerList;
