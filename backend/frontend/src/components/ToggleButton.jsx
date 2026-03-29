export default function ToggleButton({ label, selected, onClick, disabled }) {
  const className = `vote-card${selected ? ' selected' : ''}`;

  return (
    <div
      className={className}
      onClick={disabled ? undefined : onClick}
      style={disabled ? { opacity: 0.5, cursor: 'not-allowed' } : undefined}
    >
      {label}
    </div>
  );
}
