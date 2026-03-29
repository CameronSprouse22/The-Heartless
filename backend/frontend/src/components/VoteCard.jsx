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
          fontWeight: 'normal',
          color: selected ? 'rgba(255,255,255,0.85)' : '#555',
        }}>
          {voters.map(v => (
            <span key={v.voterName} style={{
              display: 'inline-block',
              marginRight: '0.4rem',
              padding: '1px 6px',
              borderRadius: '10px',
              background: v.submitted ? 'rgba(0,0,0,0.25)' : 'rgba(0,0,0,0.12)',
              border: v.submitted ? '1px solid rgba(255,255,255,0.4)' : '1px solid rgba(0,0,0,0.15)',
            }}>
              {v.voterName}{v.submitted ? ' ✓' : ''}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

export default VoteCard;
