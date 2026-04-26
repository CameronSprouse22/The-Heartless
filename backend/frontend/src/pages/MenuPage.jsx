import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMenu, getPlayerInfo, resolvePlayer, getChatCounts, dismissInitialMessage } from '../services/api';
import GameStatusBar from '../components/GameStatusBar';
import useNotifications from '../services/useNotifications';
import BanishVotePage from './BanishVotePage';
import MurderVotePage from './MurderVotePage';
import BanishRevealPage from './BanishRevealPage';
import AllChatPage from './AllChatPage';
import TraitorChatPage from './TraitorChatPage';
import IndividualChatPage from './IndividualChatPage';
import DeadChatPage from './DeadChatPage';

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
  const [showInitialMessage, setShowInitialMessage] = useState(false);
  const [initialMessage, setInitialMessage] = useState('');
  const dismissedEventEndTimeRef = useRef(null);
  const [activePanel, setActivePanel] = useState(null);
  // Track which eventEndTime we've already auto-navigated for (persisted across remounts)
  const getAutoNavDone = () => Number(sessionStorage.getItem('autoNavDoneEventEndTime') || 0);
  const setAutoNavDone = (t) => sessionStorage.setItem('autoNavDoneEventEndTime', String(t));

  // Register this browser for Web Push so notifications arrive even when the tab is closed
  const { permissionState, enableNotifications } = useNotifications(playerCode, gameCode);

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
      const eventKey = menuData.eventEndTime || null;
      const locallyDismissed = eventKey !== null && dismissedEventEndTimeRef.current === eventKey;
      if (menuData.initialMessage && !menuData.hasDismissedInitialMessage && !locallyDismissed) {
        setInitialMessage(menuData.initialMessage);
        setShowInitialMessage(true);
      } else if (!menuData.initialMessage || menuData.hasDismissedInitialMessage || locallyDismissed) {
        setShowInitialMessage(false);
      }

      // Auto-navigate when exactly one server-controlled item is enabled
      const enabledItems = (menuData.menuItems || []).filter(i => i.enabled && i.visible);
      const currentEndTime = menuData.eventEndTime || 0;
      const PANEL_IDS = ['banish-vote', 'murder-vote', 'reveal', 'all-chat', 'traitor-chat', 'individual-chat', 'dead-chat'];
      if (enabledItems.length === 1 && currentEndTime && getAutoNavDone() !== currentEndTime) {
        const navId = enabledItems[0].id;
        if (PANEL_IDS.includes(navId)) {
          setAutoNavDone(currentEndTime);
          setActivePanel(navId);
        } else {
          setAutoNavDone(currentEndTime);
          sessionStorage.setItem('autoNavEventEndTime', String(currentEndTime));
          sessionStorage.setItem('autoNavMenuPath', `/menu/${gameCode}/${encodeURIComponent(playerName)}`);
          const navPaths = {
            'actions':         `/actions/${gameCode}`,
            'game-options':    `/gameOptions/${gameCode}/${encodeURIComponent(playerName)}`,
            'game-logs':       `/logs/${gameCode}`,
          };
          const dest = navPaths[navId];
          if (dest) { navigate(dest); return; }
        }
      }
    } catch (err) {      if (err.status === 403) {
        sessionStorage.removeItem('playerCode');
        localStorage.removeItem('playerCode');
        navigate(`/join/${gameCode}`);
        return;
      }      setError(err.message);
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
    const channelKey = channelKeyMap[id];
    if (channelKey) markChannelSeen(channelKey);
    switch (id) {
      case 'game':            navigate(`/event/${gameCode}`); break;
      case 'traitor-chat':   setActivePanel('traitor-chat'); break;
      case 'all-chat':       setActivePanel('all-chat'); break;
      case 'individual-chat': setActivePanel('individual-chat'); break;
      case 'dead-chat':      setActivePanel('dead-chat'); break;
      case 'banish-vote':    setActivePanel('banish-vote'); break;
      case 'murder-vote':    setActivePanel('murder-vote'); break;
      case 'reveal':         setActivePanel('reveal'); break;
      case 'actions':        navigate(`/actions/${gameCode}`); break;
      case 'game-options':   navigate(`/gameOptions/${gameCode}/${encodeURIComponent(playerName)}`); break;
      case 'game-logs':      navigate(`/logs/${gameCode}`); break;
      default: break;
    }
  };

  // Fixed display order — server menuItems control enabled/visible per id
  const menuItemMap = Object.fromEntries((menu?.menuItems || []).map(i => [i.id, i]));
  const MENU_ORDER = [
    { id: 'game',            label: '🎮 Game' },
    { id: 'banish-vote',     label: '🗳 Banish Vote' },
    { id: 'murder-vote',     label: '🩸 Murder Vote' },
    { id: 'reveal',          label: '👁️ Reveal' },
    { id: 'all-chat',        label: '💬 All Chat' },
    { id: 'traitor-chat',    label: '👤 Traitor Chat' },
    { id: 'individual-chat', label: '🕵️ Individual Chat' },
    { id: 'actions',         label: '⚡ Actions' },
    { id: 'game-options',    label: '⚙️ Options' },
    { id: 'game-logs',       label: '📜 Game Logs' },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <GameStatusBar
        gameStatus={menu.statusString || menu.gameStatus}
        round={menu.round}
        playerName={menu.playerName}
        playersRemaining={menu.playersRemaining}
        eventType={menu.eventType}
        timeLeftMs={timeLeftMs}
      />

      <div style={{ padding: '1rem', maxWidth: '400px', margin: '0 auto', width: '100%' }}>

        {permissionState === 'default' && (
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1rem',
            padding: '0.5rem 0.75rem',
            background: '#37474f',
            color: 'white',
            borderRadius: '6px',
            fontSize: '0.9rem',
          }}>
            <span>🔔 Enable notifications to get game alerts</span>
            <button
              onClick={enableNotifications}
              style={{
                marginLeft: '0.75rem',
                padding: '0.3rem 0.75rem',
                background: '#FF9800',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontWeight: 'bold',
                whiteSpace: 'nowrap',
              }}
            >
              Enable
            </button>
          </div>
        )}

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

        {MENU_ORDER.map(({ id, label }) => {
          const serverItem = menuItemMap[id];
          // 'game' is not server-controlled — always visible. Everything else defaults to hidden if absent.
          const visible = serverItem ? serverItem.visible : id === 'game';
          const enabled = serverItem ? serverItem.enabled : id === 'game';
          if (!visible) return null;
          const channelKey = channelKeyMap[id];
          const unread = channelKey ? getUnreadCount(channelKey) : 0;
          return (
            <div key={id} style={{ position: 'relative', marginBottom: '0.5rem' }}>
              <button
                onClick={() => enabled && handleNavigate(id)}
                disabled={!enabled}
                style={buttonStyle(enabled)}
              >
                {label}
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

      {activePanel && (
        <div style={{ position: 'fixed', inset: 0, background: '#121212', zIndex: 100, overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
          <GameStatusBar
            gameStatus={menu?.statusString || menu?.gameStatus}
            round={menu?.round}
            playerName={menu?.playerName}
            playersRemaining={menu?.playersRemaining}
            eventType={menu?.eventType}
            timeLeftMs={timeLeftMs}
          />
          <div style={{ flex: 1, overflow: 'auto' }}>
            {activePanel === 'banish-vote' && <BanishVotePage onClose={() => setActivePanel(null)} />}
            {activePanel === 'murder-vote' && <MurderVotePage onClose={() => setActivePanel(null)} />}
            {activePanel === 'reveal' && <BanishRevealPage onClose={() => setActivePanel(null)} />}
            {activePanel === 'all-chat' && <AllChatPage onClose={() => setActivePanel(null)} />}
            {activePanel === 'traitor-chat' && <TraitorChatPage onClose={() => setActivePanel(null)} />}
            {activePanel === 'individual-chat' && <IndividualChatPage onClose={() => setActivePanel(null)} />}
            {activePanel === 'dead-chat' && <DeadChatPage onClose={() => setActivePanel(null)} />}
          </div>
        </div>
      )}

      {showInitialMessage && initialMessage && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '1rem',
        }}>
          <div style={{
            background: '#1a237e',
            color: 'white',
            borderRadius: '12px',
            padding: '2rem',
            maxWidth: '360px',
            width: '100%',
            textAlign: 'center',
            boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
          }}>
            <p style={{ fontSize: '1.1rem', lineHeight: '1.6', marginBottom: '1.5rem' }}>
              {initialMessage}
            </p>
            <button
              onClick={async () => {
                dismissedEventEndTimeRef.current = eventEndTimeRef.current;
                setShowInitialMessage(false);
                try {
                  await dismissInitialMessage(gameCode, playerCode);
                } catch { /* ignore */ }
              }}
              style={{
                padding: '0.6rem 2rem',
                background: '#FF9800',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                fontSize: '1rem',
                fontWeight: 'bold',
                cursor: 'pointer',
              }}
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default MenuPage;
