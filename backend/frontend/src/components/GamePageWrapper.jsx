import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getMenu } from '../services/api';
import GameStatusBar from './GameStatusBar';

function formatTime(ms) {
  if (!ms || ms <= 0) return '0:00';
  const totalSec = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSec / 60);
  const seconds = totalSec % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * SPA shell for all game sub-pages (chat, vote, actions, etc.).
 * Provides the shared dark background, the top game bar (event name + countdown),
 * and auto-navigation back to menu when an event expires.
 */
export default function GamePageWrapper({ children }) {
  const navigate = useNavigate();
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const playerName = sessionStorage.getItem('playerName') || localStorage.getItem('playerName') || '';

  const [gameBar, setGameBar] = useState(null);
  const [timeLeftMs, setTimeLeftMs] = useState(0);
  const eventEndTimeRef = useRef(0);

  // Poll menu endpoint for game bar data
  useEffect(() => {
    if (!gameCode || !playerCode) return;
    const load = async () => {
      try {
        const data = await getMenu(gameCode, playerCode);
        setGameBar(data);
        if (data.eventEndTime) {
          eventEndTimeRef.current = data.eventEndTime;
          setTimeLeftMs(Math.max(0, data.eventEndTime - Date.now()));
        }
      } catch { /* silently ignore — sub-pages handle their own errors */ }
    };
    load();
    const interval = setInterval(load, 5000);
    return () => clearInterval(interval);
  }, [gameCode, playerCode]);

  // Local countdown driven by server epoch end time
  useEffect(() => {
    const tick = setInterval(() => {
      setTimeLeftMs(Math.max(0, eventEndTimeRef.current - Date.now()));
    }, 1000);
    return () => clearInterval(tick);
  }, []);

  // Auto-navigate back to menu when the event the player was sent here for expires
  useEffect(() => {
    const endTime = Number(sessionStorage.getItem('autoNavEventEndTime') || 0);
    const menuPath = sessionStorage.getItem('autoNavMenuPath') || '';
    if (!endTime || !menuPath) return;

    const remaining = endTime - Date.now();
    if (remaining <= 0) {
      sessionStorage.removeItem('autoNavEventEndTime');
      sessionStorage.removeItem('autoNavMenuPath');
      navigate(menuPath, { replace: true });
      return;
    }

    const timer = setTimeout(() => {
      sessionStorage.removeItem('autoNavEventEndTime');
      sessionStorage.removeItem('autoNavMenuPath');
      navigate(menuPath, { replace: true });
    }, remaining);

    return () => clearTimeout(timer);
  }, []);

  const backPath = gameCode && playerName
    ? `/menu/${gameCode}/${encodeURIComponent(playerName)}`
    : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: '#1a1a1a', color: '#e0e0e0' }}>
      <GameStatusBar
        playerName={gameBar?.playerName || playerName}
        gameStatus={gameBar?.statusString || gameBar?.gameStatus}
        round={gameBar?.round ?? -1}
        playersRemaining={gameBar?.playersRemaining}
        eventType={gameBar?.eventType}
        timeLeftMs={timeLeftMs}
        backPath={backPath}
      />
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {children}
      </div>
    </div>
  );
}
