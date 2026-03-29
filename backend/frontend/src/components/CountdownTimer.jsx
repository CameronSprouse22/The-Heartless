import { useState, useEffect } from 'react';

export default function CountdownTimer({ endTime }) {
  const [remaining, setRemaining] = useState(() => Math.max(0, endTime - Date.now()));

  useEffect(() => {
    if (remaining <= 0) return;

    const interval = setInterval(() => {
      const diff = Math.max(0, endTime - Date.now());
      setRemaining(diff);
      if (diff <= 0) clearInterval(interval);
    }, 1000);

    return () => clearInterval(interval);
  }, [endTime]);

  if (remaining <= 0) {
    return <p style={{ textAlign: 'center', fontWeight: 'bold', color: '#e94560' }}>Time's up</p>;
  }

  const totalSeconds = Math.ceil(remaining / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return (
    <p style={{ textAlign: 'center', fontSize: '1.25rem', fontWeight: 'bold' }}>
      {minutes}:{seconds.toString().padStart(2, '0')}
    </p>
  );
}
