export default function PlayerStatusList({ players }) {
  if (!players || players.length === 0) return null;

  return (
    <div style={{ marginTop: '1.5rem' }}>
      <h3>Players</h3>
      {players.map((p) => (
        <div key={p.playerId} className="card" style={{ marginBottom: '0.5rem', padding: '0.5rem 0.75rem' }}>
          <strong>{p.playerName}</strong>
          <span style={{ marginLeft: '0.75rem', opacity: 0.7 }}>
            {p.submissionStatus === 'SUBMITTED'
              ? 'Submitted'
              : p.selectedItems && p.selectedItems.length > 0
                ? p.selectedItems.join(', ')
                : '—'}
          </span>
        </div>
      ))}
    </div>
  );
}
