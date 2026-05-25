import React, { useState, useEffect, useCallback, useRef } from 'react';
import { markEventReady, dismissInitialMessage, getMenu, getChatCounts, resolvePlayer } from '../services/api';
import { useParams } from 'react-router-dom';
import GameStatusBar from '../components/GameStatusBar';
import useNotifications from '../services/useNotifications';
import BanishVotePage from './BanishVotePage';
import MurderVotePage from './MurderVotePage';
import BanishRevealPage from './BanishRevealPage';
import IdentityRevealPage from './IdentityRevealPage';
import RoleRevealPage from './RoleRevealPage';
import AllChatPage from './AllChatPage';
import TraitorChatPage from './TraitorChatPage';
import IndividualChatPage from './IndividualChatPage';
import DeadChatPage from './DeadChatPage';
import SitRepPage from './SitRepPage';
import MiniGamePage from './MiniGamePage';
import ScuttlebuttPage from './ScuttlebuttPage';
import TestDashboardPage from './TestDashboardPage';

function formatTime(ms) {
  if (!ms || ms <= 0) return '0:00';
  const totalSec = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSec / 60);
  const seconds = totalSec % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function MenuPage() {
  const { gameCode, playerName } = useParams();
  const [menu, setMenu] = useState(null);
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
  const [panelTab, setPanelTab] = useState('event');
  const [chatSubTab, setChatSubTab] = useState('all');
  // Track the last eventEndTime seen so we can reset tabs on event change
  const prevEventEndTimeRef = useRef(0);

  const { permissionState, enableNotifications } = useNotifications(playerCode, gameCode);

  // Resolve playerName to playerCode on mount
  useEffect(() => {
    async function resolve() {
      try {
        const result = await resolvePlayer(gameCode, playerName);
        const code = result.playerCode;
        setPlayerCode(code);
        sessionStorage.setItem('playerCode', code);
        sessionStorage.setItem('playerName', playerName);
        sessionStorage.setItem('gameCode', gameCode);
      } catch (err) {
        setError('Could not find player "' + playerName + '" in game ' + gameCode);
      }
    }
    if (gameCode && playerName && !playerCode) {
      resolve();
    }
  }, [gameCode, playerName]);

  const loadMenu = useCallback(async () => {
    if (!playerCode) return;
    try {
      const menuData = await getMenu(gameCode, playerCode);
      setMenu(menuData);
      if (menuData.eventEndTime) {
        eventEndTimeRef.current = menuData.eventEndTime;
        setTimeLeftMs(Math.max(0, menuData.eventEndTime - Date.now()));
      }
      // Reset to Event tab when a new event starts
      const newEndTime = menuData.eventEndTime || 0;
      if (newEndTime !== prevEventEndTimeRef.current) {
        prevEventEndTimeRef.current = newEndTime;
        setPanelTab('event');
      }
      const eventKey = menuData.eventEndTime || null;
      const locallyDismissed = eventKey !== null && dismissedEventEndTimeRef.current === eventKey;
      if (menuData.initialMessage && !menuData.hasDismissedInitialMessage && !locallyDismissed) {
        setInitialMessage(menuData.initialMessage);
        setShowInitialMessage(true);
      } else if (!menuData.initialMessage || menuData.hasDismissedInitialMessage || locallyDismissed) {
        setShowInitialMessage(false);
      }
    } catch (err) {
      setError(err.message);
    }
  }, [gameCode, playerCode]);

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
    const refresh = setInterval(loadMenu, 2000);
    return () => clearInterval(refresh);
  }, [loadMenu]);

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

  const channelKeyMap = {
    'all': 'all',
    'individual': 'individual',
    'traitor': 'traitors',
    'dead': 'dead',
  };

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

  if (error && !menu) {
    return <div style={{ padding: '2rem', textAlign: 'center', color: 'red' }}>{error}</div>;
  }

  if (!menu) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading...</div>;
  }

  const isAfterLife = menu.eventType === 'After Life';
  const showChatTab = menu.allChatEnabled || menu.individualChatEnabled || menu.traitorChatEnabled;
  const showStatusTab = menu.statusEnabled;

  const tabs = [
    { id: 'event', label: 'Event', color: '#2e7d32' },
    ...(showChatTab ? [{ id: 'chat', label: 'Chat', color: '#e65100' }] : []),
    ...(showStatusTab ? [{ id: 'status', label: 'Status', color: '#37474f' }] : []),
  ];

  // Render the appropriate event component for the Event tab
  function renderEventPanel() {
    const et = menu?.eventType;
    if (!et) {
      return (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#aaa', marginTop: '4rem' }}>
          Waiting for next event...
        </div>
      );
    }
    switch (et) {
      case 'Banish Vote':
      case 'Banish Second Vote':
        return <BanishVotePage />;
      case 'Banish Pre':
        return (
          <AllChatPage
            onReady={async () => {
              await markEventReady(gameCode, playerCode).catch(() => {});
              await loadMenu();
            }}
            readyDone={menu?.mySelection?.submitPressed === true}
          />
        );
      case 'Banish Reveal':
      case 'Banish Second Reveal':
      case 'Vote Reveal':
      case 'Murder Reveal':
        return <BanishRevealPage />;
      case 'Murder Vote':
        return <MurderVotePage />;
      case 'Reveal Player Identity':
        return <IdentityRevealPage />;
      case 'Reveal Role':
        return <RoleRevealPage />;
      case 'Mini Game':
        return <MiniGamePage onClose={loadMenu} />;
      case 'Scuttlebutt':
        return <ScuttlebuttPage />;
      case 'Sit Rep':
        return <SitRepPage />;
      case 'Recruit':
        return <TraitorChatPage />;
      case 'After Life':
        return <DeadChatPage />;
      case 'Testing':
        return <TestDashboardPage />;
      default:
        return (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#aaa', marginTop: '4rem' }}>
            {et}
          </div>
        );
    }
  }

  // Available chat sub-tabs for the Chat tab
  const chatSubTabs = [
    ...(menu.allChatEnabled && !isAfterLife ? [{ id: 'all', label: 'All Chat', channel: 'all' }] : []),
    ...(menu.individualChatEnabled ? [{ id: 'individual', label: 'Individual', channel: 'individual' }] : []),
    ...(menu.traitorChatEnabled ? [{ id: 'traitor', label: 'Traitor Chat', channel: 'traitors' }] : []),
    ...(isAfterLife ? [{ id: 'dead', label: 'Dead Chat', channel: 'dead' }] : []),
  ];

  // Ensure chatSubTab is valid given current event
  const validChatSubTab = chatSubTabs.find(t => t.id === chatSubTab)
    ? chatSubTab
    : (chatSubTabs[0]?.id || 'all');

  function renderChatSubPanel(id) {
    switch (id) {
      case 'all':
        return (
          <AllChatPage
            onReady={menu?.eventType === 'Banish Pre' ? async () => {
              await markEventReady(gameCode, playerCode).catch(() => {});
              await loadMenu();
            } : undefined}
            readyDone={menu?.eventType === 'Banish Pre' && menu?.mySelection?.submitPressed === true}
          />
        );
      case 'individual':
        return menu?.eventType === 'Scuttlebutt'
          ? <ScuttlebuttPage />
          : <IndividualChatPage />;
      case 'traitor':
        return <TraitorChatPage />;
      case 'dead':
        return <DeadChatPage />;
      default:
        return null;
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: '#121212' }}>
      <GameStatusBar
        gameStatus={menu.statusString || menu.gameStatus}
        round={menu.round}
        playerName={menu.playerName}
        playersRemaining={menu.playersRemaining}
        eventType={menu.eventType}
        timeLeftMs={timeLeftMs}
        tabs={tabs}
        activeTab={panelTab}
        onTabChange={(id) => {
          setPanelTab(id);
          if (id === 'chat') {
            const ch = chatSubTabs.find(t => t.id === validChatSubTab)?.channel;
            if (ch) markChannelSeen(ch);
          }
        }}
      />

      <div style={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
        {/* Event tab */}
        {panelTab === 'event' && renderEventPanel()}

        {/* Chat tab */}
        {panelTab === 'chat' && showChatTab && (
          <div style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
            {chatSubTabs.length > 1 && (
              <div style={{
                display: 'flex',
                gap: '0.4rem',
                padding: '0.4rem 0.6rem',
                background: '#1a1a1a',
                borderBottom: '1px solid #333',
                flexShrink: 0,
              }}>
                {chatSubTabs.map(tab => {
                  const unread = getUnreadCount(tab.channel);
                  return (
                    <div key={tab.id} style={{ position: 'relative' }}>
                      <button
                        onClick={() => {
                          setChatSubTab(tab.id);
                          markChannelSeen(tab.channel);
                        }}
                        style={{
                          padding: '0.3rem 0.7rem',
                          background: validChatSubTab === tab.id ? '#e65100' : '#2a2a2a',
                          color: 'white',
                          border: validChatSubTab === tab.id ? 'none' : '1px solid #444',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontWeight: validChatSubTab === tab.id ? 'bold' : 'normal',
                          fontSize: '0.82rem',
                        }}
                      >
                        {tab.label}
                      </button>
                      {unread > 0 && (
                        <div style={{
                          position: 'absolute',
                          top: '-6px',
                          right: '-6px',
                          background: '#FF5252',
                          color: 'white',
                          borderRadius: '50%',
                          width: '18px',
                          height: '18px',
                          fontSize: '0.7rem',
                          fontWeight: 'bold',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          border: '2px solid #121212',
                        }}>
                          {unread > 99 ? '99+' : unread}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
            <div style={{ flex: 1, overflow: 'auto' }}>
              {renderChatSubPanel(validChatSubTab)}
            </div>
          </div>
        )}

        {/* Status tab */}
        {panelTab === 'status' && showStatusTab && <SitRepPage />}
      </div>

      {permissionState === 'default' && (
        <div style={{
          position: 'fixed',
          bottom: '1rem',
          left: '50%',
          transform: 'translateX(-50%)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          padding: '0.5rem 0.75rem',
          background: '#37474f',
          color: 'white',
          borderRadius: '6px',
          fontSize: '0.9rem',
          zIndex: 200,
          maxWidth: '360px',
          width: 'calc(100% - 2rem)',
        }}>
          <span>ðŸ”” Enable notifications for game alerts</span>
          <button
            onClick={enableNotifications}
            style={{
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