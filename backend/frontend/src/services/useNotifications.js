import { useCallback, useEffect, useRef, useState } from 'react';

const PUSH_SUBSCRIBE_URL = '/api/push/subscribe';
const VAPID_KEY_URL = '/api/push/vapid-public-key';

/** Convert a Base64URL string to a Uint8Array (required by pushManager.subscribe). */
function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(base64);
  return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
}

/** Convert an ArrayBuffer to a Base64URL string (for sending subscription keys to the server). */
function arrayBufferToBase64Url(buffer) {
  return btoa(String.fromCharCode(...new Uint8Array(buffer)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

/**
 * Subscribe the browser to push and register with the server.
 * Assumes Notification.permission === 'granted' by the time this is called.
 */
async function subscribePush(playerCode, gameCode) {
  console.log('[push] subscribePush called — playerCode:', playerCode, 'gameCode:', gameCode);
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    console.warn('[push] Service workers or PushManager not supported in this browser');
    return false;
  }

  let registration;
  try {
    registration = await navigator.serviceWorker.register('/sw.js');
    await navigator.serviceWorker.ready;
    console.log('[push] Service worker ready, scope:', registration.scope);
  } catch (err) {
    console.warn('[push] Service worker registration failed:', err);
    return false;
  }

  // Fetch VAPID public key from server first — we need it to detect stale subscriptions
  let vapidPublicKey;
  try {
    const res = await fetch(VAPID_KEY_URL);
    const data = await res.json();
    vapidPublicKey = data.publicKey;
    console.log('[push] VAPID key fetched OK');
  } catch (err) {
    console.warn('[push] Failed to fetch VAPID public key:', err);
    return false;
  }

  const serverKeyBytes = urlBase64ToUint8Array(vapidPublicKey);

  // Check for an existing subscription and verify it uses the current VAPID key
  let subscription = await registration.pushManager.getSubscription();

  if (subscription) {
    // Compare the stored applicationServerKey with the server's current key.
    // If they differ (e.g. server keys were rotated) we must unsubscribe and re-subscribe.
    const existingKey = subscription.options?.applicationServerKey;
    const existingBytes = existingKey ? new Uint8Array(existingKey) : null;
    const keysMatch = existingBytes &&
      existingBytes.length === serverKeyBytes.length &&
      existingBytes.every((b, i) => b === serverKeyBytes[i]);

    if (!keysMatch) {
      console.info('[push] VAPID key changed — unsubscribing stale subscription');
      await subscription.unsubscribe();
      subscription = null;
    }
  }

  if (!subscription) {
    try {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: serverKeyBytes,
      });
      console.log('[push] New push subscription created');
    } catch (err) {
      console.warn('[push] Push subscription failed:', err);
      return false;
    }
  } else {
    console.log('[push] Reusing existing push subscription');
  }

  // Send subscription to backend
  const key = subscription.getKey('p256dh');
  const auth = subscription.getKey('auth');
  const p256dh = key  ? arrayBufferToBase64Url(key)  : null;
  const authStr = auth ? arrayBufferToBase64Url(auth) : null;

  try {
    const res = await fetch(PUSH_SUBSCRIBE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        playerCode,
        gameCode,
        endpoint: subscription.endpoint,
        p256dh,
        auth: authStr,
      }),
    });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      console.warn('[push] Server rejected subscription:', res.status, text);
      return false;
    }
    console.log('[push] Subscription registered with server ✓');
  } catch (err) {
    console.warn('[push] Failed to register subscription with server:', err);
    return false;
  }

  return true;
}

/**
 * Hook: manages Web Push registration.
 *
 * Returns:
 *   - permissionState: 'default' | 'granted' | 'denied' | 'unsupported'
 *   - enableNotifications: call this from a button click to prompt + subscribe
 *
 * @param {string} playerCode
 * @param {string} gameCode
 */
export default function useNotifications(playerCode, gameCode) {
  const registered = useRef(false);
  const [permissionState, setPermissionState] = useState(() => {
    if (!('Notification' in window)) return 'unsupported';
    return Notification.permission; // 'default' | 'granted' | 'denied'
  });

  // If permission is already granted (e.g. returning visitor), subscribe automatically.
  useEffect(() => {
    if (!playerCode || !gameCode) return;
    if (registered.current) return;
    if (permissionState !== 'granted') return;
    subscribePush(playerCode, gameCode).then((ok) => {
      if (ok) registered.current = true;
    });
  }, [playerCode, gameCode, permissionState]);

  // Called from a button click — triggers the browser permission prompt.
  const enableNotifications = useCallback(async () => {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'denied') return;

    // requestPermission MUST be called from a user gesture.
    const result = await Notification.requestPermission();
    setPermissionState(result);

    if (result === 'granted' && playerCode && gameCode && !registered.current) {
      const ok = await subscribePush(playerCode, gameCode);
      if (ok) registered.current = true;
    }
  }, [playerCode, gameCode]);

  return { permissionState, enableNotifications };
}

