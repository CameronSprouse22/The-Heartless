import React from 'react';

function VoteCard({ player, selected, onSelect, voters = [] }) {
  return (
    <div
      onClick={() => onSelect(player.id)}
      style={{
        padding: '0.75rem',
        marginBottom: '0.5rem',
        background: selected ? '#4CAF50' : '#f0f0f0',
        color: selected ? 'white' : '#333',
        borderRadius: '8px',
        cursor: 'pointer',
        border: selected ? '2px solid #388E3C' : '2px solid transparent',
        transition: 'all 0.2s',
        textAlign: 'center',
        fontSize: '1.1rem',
        fontWeight: selected ? 'bold' : 'normal',
      }}
    >
      {player.name}
    </div>
  );
}

export default VoteCard;
