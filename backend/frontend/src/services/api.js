const BASE_URL = '/api';

async function fetchJson(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const response = await fetch(`${BASE_URL}${url}`, { ...options, headers });

  if (!response.ok) {
    const bodyText = await response.text().catch(() => '');
    let message = response.statusText;
    try {
      const parsed = JSON.parse(bodyText);
      message = parsed.error || parsed.message || message;
    } catch {
      if (bodyText) message = bodyText;
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

function authHeader(playerCode) {
  return { 'X-Player-Code': playerCode };
}

// Game Management
export function createGame(playerName) {
  return fetchJson('/games', {
    method: 'POST',
    body: JSON.stringify({ playerName }),
  });
}

export function createTestGame() {
  return fetchJson('/games/test-setup', {
    method: 'POST',
  });
}

export function getGameState(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}`, {
    headers: authHeader(playerCode),
  });
}

export function startGame(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/start`, {
    method: 'POST',
    headers: authHeader(playerCode),
  });
}

// Player & Invitation
export function invitePlayer(gameCode, playerCode, name, contact) {
  return fetchJson(`/games/${gameCode}/invite`, {
    method: 'POST',
    headers: authHeader(playerCode),
    body: JSON.stringify({ name, contact }),
  });
}

export function getJoinInfo(gameCode) {
  return fetchJson(`/join/${gameCode}`);
}

export function joinGame(gameCode, name, contact) {
  return fetchJson(`/join/${gameCode}`, {
    method: 'POST',
    body: JSON.stringify({ name, contact }),
  });
}

// Player Info
export function getPlayerInfo(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/me`, {
    headers: authHeader(playerCode),
  });
}

// Chat
export function sendChatMessage(gameCode, playerCode, channel, text, recipientId) {
  const body = { text };
  if (recipientId) body.recipientId = recipientId;
  return fetchJson(`/games/${gameCode}/chat/${channel}/messages`, {
    method: 'POST',
    headers: authHeader(playerCode),
    body: JSON.stringify(body),
  });
}

export function getChatMessages(gameCode, playerCode, channel, since) {
  const params = since ? `?since=${since}` : '';
  return fetchJson(`/games/${gameCode}/chat/${channel}/messages${params}`, {
    headers: authHeader(playerCode),
  });
}

// Voting
export function getBanishCandidates(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/vote/banish`, {
    headers: authHeader(playerCode),
  });
}

export function castBanishVote(gameCode, playerCode, targetPlayerId) {
  return fetchJson(`/games/${gameCode}/vote/banish`, {
    method: 'POST',
    headers: authHeader(playerCode),
    body: JSON.stringify({ targetPlayerId }),
  });
}

export function getMurderCandidates(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/vote/murder`, {
    headers: authHeader(playerCode),
  });
}

export function castMurderVote(gameCode, playerCode, targetPlayerIds) {
  return fetchJson(`/games/${gameCode}/vote/murder`, {
    method: 'POST',
    headers: authHeader(playerCode),
    body: JSON.stringify({ targetPlayerIds }),
  });
}

// Menu
export function getMenu(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/menu`, {
    headers: authHeader(playerCode),
  });
}

export function dismissInitialMessage(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/dismiss-message`, {
    method: 'POST',
    headers: authHeader(playerCode),
  });
}

// Resolve player by name (no auth required)
export function resolvePlayer(gameCode, playerName) {
  return fetchJson(`/games/${gameCode}/resolve/${encodeURIComponent(playerName)}`);
}

// Chat Counts
export function getChatCounts(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/chat/counts`, {
    headers: authHeader(playerCode),
  });
}

// Round Info
export function getRoundInfo(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/round`, {
    headers: authHeader(playerCode),
  });
}

// Game Events
export function createEvent(gameCode, playerCode, config) {
  return fetchJson(`/games/${gameCode}/event`, {
    method: 'POST',
    headers: authHeader(playerCode),
    body: JSON.stringify(config),
  });
}

export function getEventState(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/event`, {
    headers: authHeader(playerCode),
  });
}

export function getEventPlayers(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/event/players`, {
    headers: authHeader(playerCode),
  });
}

// Push notification preferences
export function getNotifPrefs(playerCode) {
  return fetchJson('/push/prefs', {
    headers: authHeader(playerCode),
  });
}

export function updateNotifPrefs(playerCode, prefs) {
  return fetchJson('/push/prefs', {
    method: 'PUT',
    headers: authHeader(playerCode),
    body: JSON.stringify(prefs),
  });
}

// Vote Reveal
export function getRevealedVotes(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/reveal`, {
    headers: authHeader(playerCode),
  });
}

export function closeReveal(gameCode, playerCode) {
  return fetchJson(`/games/${gameCode}/reveal/close`, {
    method: 'POST',
    headers: authHeader(playerCode),
  });
}
