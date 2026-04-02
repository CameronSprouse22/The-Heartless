import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMenu, getPlayerInfo, resolvePlayer, getChatCounts } from '../services/api';
import GameStatusBar from '../components/GameStatusBar';

function formatTime(ms) {
  if (!ms || ms <= 0) return '0:00';
  const totalSec = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSec / 60);
  const seconds = totalSec % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function MenuPage() {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();
  const [menu, setMenu] = useState(null);
  const [playerInfo, setPlayerInfo] = useState(null);
  const [error, setError] = useState('');
  const [playerCode, setPlayerCode] = useState(
    sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode')
  );
  const [chatCounts, setChatCounts] = useState({});
  const lastSeenCounts = useRef(JSON.parse(sessionStorage.getItem('lastSeenCounts') || '{}'));
  const [timeLeftMs, setTimeLeftMs] = useState(0);
  const eventEndTimeRef = useRef(0);

  // Resolve playerName to playerCode on mount
  useEffect(() => {
    async function resolve() {
      try {
        const result = await resolvePlayer(gameCode, playerName);
        const code = result.playerCode;
        setPlayerCode(code);
        // Use sessionStorage so each tab tracks its own player independently
        sessionStorage.setItem('playerCode', code);
        sessionStorage.setItem('playerName', playerName);
        sessionStorage.setItem('gameCode', gameCode);
      } catch (err) {
        setError('Could not find player "' + playerName + '" in game ' + gameCode);
      }
    }
    if (gameCode && playerName) {
      resolve();
    }
  }, [gameCode, playerName]);

  const loadMenu = useCallback(async () => {
    if (!playerCode) return;
    try {
      const [menuData, infoData] = await Promise.all([
        getMenu(gameCode, playerCode),
        getPlayerInfo(gameCode, playerCode),
      ]);
      setMenu(menuData);
      setPlayerInfo(infoData);
      if (menuData.eventEndTime) {
        eventEndTimeRef.current = menuData.eventEndTime;
        setTimeLeftMs(Math.max(0, menuData.eventEndTime - Date.now()));
      }
    } catch (err) {
      setError(err.message);
    }
  }, [gameCode, playerCode]);

  // Poll chat counts for notification badges
  const loadChatCounts = useCallback(async () => {
    if (!playerCode) return;
    try {
      const result = await getChatCounts(gameCode, playerCode);
      setChatCounts(result.counts || {});
    } catch {
      // silently ignore
    }
  }, [gameCode, playerCode]);

  useEffect(() => {
    loadMenu();
    const refresh = setInterval(loadMenu, 5000);
    return () => clearInterval(refresh);
  }, [loadMenu]);

  // 1-second local countdown driven by server-provided epoch end time
  useEffect(() => {
    const tick = setInterval(() => {
      setTimeLeftMs(Math.max(0, eventEndTimeRef.current - Date.now()));
    }, 1000);
    return () => clearInterval(tick);
  }, []);

  useEffect(() => {
    loadChatCounts();
    const interval = setInterval(loadChatCounts, 3000);
    return () => clearInterval(interval);
  }, [loadChatCounts]);

  // Update lastSeen when navigating to a chat
  const markChannelSeen = (channelKey) => {
    const current = chatCounts[channelKey] || 0;
    lastSeenCounts.current[channelKey] = current;
    sessionStorage.setItem('lastSeenCounts', JSON.stringify(lastSeenCounts.current));
  };

  const getUnreadCount = (channelKey) => {
    const total = chatCounts[channelKey] || 0;
    const seen = lastSeenCounts.current[channelKey] || 0;
    return Math.max(0, total - seen);
  };

  // Map menu item IDs to channel keys for notification
  const channelKeyMap = {
    'traitor-chat': 'traitors',
    'all-chat': 'all',
    'dead-chat': 'dead',
    'individual-chat': 'individual',
  };

  if (error && !menu) {
    return <div style={{ padding: '2rem', textAlign: 'center', color: 'red' }}>{error}</div>;
  }

  if (!menu) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading menu...</div>;
  }

  const buttonStyle = (enabled) => ({
    width: '100%',
    padding: '0.75rem',
    marginBottom: '0.5rem',
    fontSize: '1rem',
    border: 'none',
    borderRadius: '4px',
    cursor: enabled ? 'pointer' : 'not-allowed',
    background: enabled ? '#4CAF50' : '#ccc',
    color: 'white',
    position: 'relative',
  });

  const badgeStyle = {
    position: 'absolute',
    top: '-6px',
    right: '-6px',
    background: '#FF5252',
    color: 'white',
    borderRadius: '50%',
    width: '22px',
    height: '22px',
    fontSize: '0.75rem',
    fontWeight: 'bold',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '2px solid white',
  };

  const handleNavigate = (id) => {
    // Mark the channel as seen when entering
    const channelKey = channelKeyMap[id];
    if (channelKey) {
      markChannelSeen(channelKey);
    }
    switch (id) {
      case 'traitor-chat': navigate(`/chat/${gameCode}/traitors`); break;
      case 'all-chat': navigate(`/chat/${gameCode}/all`); break;
      case 'individual-chat': navigate(`/chat/${gameCode}/individual`); break;
      case 'dead-chat': navigate(`/chat/${gameCode}/dead`); break;
      case 'banish-vote': navigate(`/vote/${gameCode}/${encodeURIComponent(playerName)}/banish`); break;
      case 'murder-vote': navigate(`/vote/${gameCode}/${encodeURIComponent(playerName)}/murder`); break;
      default: break;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <GameStatusBar
        gameStatus={menu.statusString || menu.gameStatus}
        round={menu.round}
        currentTask={menu.currentTask}
        playerName={menu.playerName}
      />

      <div style={{ padding: '1rem', maxWidth: '400px', margin: '0 auto', width: '100%' }}>
        <h2>Game Menu</h2>

        {menu.eventType && (
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1rem',
            padding: '0.5rem 0.75rem',
            background: '#1a237e',
            color: 'white',
            borderRadius: '6px',
            fontSize: '0.95rem',
            fontWeight: 'bold',
          }}>
            <span>📋 {menu.eventType}</span>
            <span style={{
              fontFamily: 'monospace',
              fontSize: '1rem',
              color: timeLeftMs < 60000 ? '#FF5252' : '#A5D6A7',
            }}>⏱ {formatTime(timeLeftMs)}</span>
          </div>
        )}

        {playerInfo && playerInfo.card && (
          <div style={{
            textAlign: 'center',
            marginBottom: '1rem',
            padding: '0.5rem',
            background: '#f0f0f0',
            borderRadius: '8px',
          }}>
            <p><strong>Your Card:</strong> {playerInfo.card.number} of {playerInfo.card.suit}s</p>
            {menu.isTraitor && <p style={{ color: 'red', fontWeight: 'bold' }}>You are a TRAITOR</p>}
            {!menu.isTraitor && <p style={{ color: 'green' }}>You are Faithful</p>}
          </div>
        )}

        {menu.menuItems.map((item) => {
          if (!item.visible) return null;
          const channelKey = channelKeyMap[item.id];
          const unread = channelKey ? getUnreadCount(channelKey) : 0;
          return (
            <div key={item.id} style={{ position: 'relative', marginBottom: '0.5rem' }}>
              <button
                onClick={() => item.enabled && handleNavigate(item.id)}
                disabled={!item.enabled}
                style={buttonStyle(item.enabled)}
              >
                {item.label}
              </button>
              {unread > 0 && (
                <div style={badgeStyle}>
                  {unread > 99 ? '99+' : unread}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {error && <p style={{ color: 'red', padding: '1rem' }}>{error}</p>}
    </div>
  );
}

export default MenuPage;
