import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getNotifPrefs, updateNotifPrefs } from '../services/api';
import useNotifications from '../services/useNotifications';

const TOGGLES = [
  { key: 'allChats',        label: 'All Chats' },
  { key: 'individualChats', label: 'Individual Chats' },
  { key: 'traitorChats',    label: 'Traitor Chats' },
  { key: 'eventStarted',   label: 'Event Started' },
  { key: 'eventEnding',    label: 'Event Ending' },
];

const DEFAULT_PREFS = {
  allChats: true,
  individualChats: true,
  traitorChats: true,
  eventStarted: true,
  eventEnding: true,
};

function Toggle({ enabled, onToggle }) {
  return (
    <button
      onClick={onToggle}
      aria-pressed={enabled}
      style={{
        width: '52px',
        height: '28px',
        borderRadius: '14px',
        border: 'none',
        cursor: 'pointer',
        background: enabled ? '#4CAF50' : '#ccc',
        position: 'relative',
        transition: 'background 0.2s',
        flexShrink: 0,
      }}
    >
      <span style={{
        position: 'absolute',
        top: '3px',
        left: enabled ? '27px' : '3px',
        width: '22px',
        height: '22px',
        borderRadius: '50%',
        background: 'white',
        boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
        transition: 'left 0.2s',
      }} />
    </button>
  );
}

export default function NotificationsPage() {
  const { gameCode, playerName: playerNameParam } = useParams();
  const navigate = useNavigate();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const playerName = playerNameParam || sessionStorage.getItem('playerName') || localStorage.getItem('playerName') || '';

  const [prefs, setPrefs] = useState(DEFAULT_PREFS);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const { permissionState, enableNotifications } = useNotifications(playerCode, gameCode);

  const loadPrefs = useCallback(async () => {
    if (!playerCode) return;
    try {
      const data = await getNotifPrefs(playerCode);
      setPrefs({ ...DEFAULT_PREFS, ...data });
    } catch {
      // Use defaults silently — player may not have subscribed yet
    }
  }, [playerCode]);

  useEffect(() => { loadPrefs(); }, [loadPrefs]);

  const toggle = async (key) => {
    const updated = { ...prefs, [key]: !prefs[key] };
    setPrefs(updated);
    setSaving(true);
    try {
      await updateNotifPrefs(playerCode, updated);
    } catch {
      setError('Failed to save preference. Try again.');
      setPrefs(prefs); // revert
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* Header */}
      <div style={{
        padding: '0.75rem 1rem',
        background: '#37474f',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
      }}>
        <button
          onClick={() => navigate(`/gameOptions/${gameCode}/${encodeURIComponent(playerName)}`)}
          style={{ background: 'transparent', color: 'white', border: 'none', cursor: 'pointer', fontSize: '1.1rem' }}
        >
          ← Back
        </button>
        <span style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>Notifications</span>
      </div>

      <div style={{ padding: '1.25rem', maxWidth: '420px', margin: '0 auto', width: '100%' }}>

        {/* Push permission banner */}
        {permissionState === 'default' && (
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1.25rem',
            padding: '0.75rem',
            background: '#fff3e0',
            borderRadius: '8px',
            border: '1px solid #ffcc80',
            fontSize: '0.9rem',
          }}>
            <span>Enable browser notifications to receive alerts</span>
            <button
              onClick={enableNotifications}
              style={{
                marginLeft: '0.75rem',
                padding: '0.35rem 0.75rem',
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

        {permissionState === 'denied' && (
          <div style={{
            marginBottom: '1.25rem',
            padding: '0.75rem',
            background: '#ffebee',
            borderRadius: '8px',
            border: '1px solid #ef9a9a',
            fontSize: '0.9rem',
            color: '#c62828',
          }}>
            Notifications are blocked in your browser. Allow them in your browser settings to receive push alerts.
          </div>
        )}

        {/* Toggle list */}
        <div style={{
          background: 'white',
          borderRadius: '10px',
          boxShadow: '0 1px 4px rgba(0,0,0,0.1)',
          overflow: 'hidden',
        }}>
          {TOGGLES.map(({ key, label }, idx) => (
            <div
              key={key}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '0.9rem 1rem',
                borderBottom: idx < TOGGLES.length - 1 ? '1px solid #f0f0f0' : 'none',
              }}
            >
              <span style={{ fontSize: '1rem', color: '#212121' }}>{label}</span>
              <Toggle enabled={prefs[key]} onToggle={() => toggle(key)} />
            </div>
          ))}
        </div>

        {saving && (
          <p style={{ textAlign: 'center', color: '#888', marginTop: '0.75rem', fontSize: '0.85rem' }}>
            Saving…
          </p>
        )}
        {error && (
          <p style={{ textAlign: 'center', color: 'red', marginTop: '0.75rem', fontSize: '0.85rem' }}>
            {error}
          </p>
        )}

        <p style={{ marginTop: '1.5rem', fontSize: '0.8rem', color: '#999', textAlign: 'center' }}>
          Preferences apply to push notifications sent while you are away from this page.
        </p>
      </div>
    </div>
  );
}
