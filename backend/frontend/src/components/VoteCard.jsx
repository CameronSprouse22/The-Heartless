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
      {voters.length > 0 && (
        <div style={{
          marginTop: '0.3rem',
          fontSize: '0.75rem',
          opacity: 0.85,
          fontWeight: 'normal',
          color: selected ? 'rgba(255,255,255,0.9)' : '#555',
        }}>
          {voters.map(v => `${v.voterName}${v.submitted ? ' ✓' : ''}`).join(', ')}
        </div>
      )}
    </div>
  );
}

export default VoteCard;
