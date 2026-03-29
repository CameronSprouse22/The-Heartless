import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient = null;

export function connect(playerCode, onConnect, onError) {
  if (stompClient && stompClient.connected) {
    return stompClient;
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { 'X-Player-Code': playerCode },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      if (onConnect) onConnect(stompClient);
    },
    onStompError: (frame) => {
      if (onError) onError(frame);
    },
    onWebSocketClose: () => {
      if (onError) onError(new Error('WebSocket connection closed'));
    },
  });

  stompClient.activate();
  return stompClient;
}

export function disconnect() {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
}

export function subscribe(destination, callback) {
  if (!stompClient || !stompClient.connected) {
    throw new Error('STOMP client is not connected');
  }
  return stompClient.subscribe(destination, (message) => {
    callback(JSON.parse(message.body));
  });
}

export function send(destination, body = {}) {
  if (!stompClient || !stompClient.connected) {
    throw new Error('STOMP client is not connected');
  }
  stompClient.publish({ destination, body: JSON.stringify(body) });
}
