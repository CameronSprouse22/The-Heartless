import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import JoinPage from './pages/JoinPage';
import LobbyPage from './pages/LobbyPage';
import MenuPage from './pages/MenuPage';
import AllChatPage from './pages/AllChatPage';
import TraitorChatPage from './pages/TraitorChatPage';
import IndividualChatPage from './pages/IndividualChatPage';
import DeadChatPage from './pages/DeadChatPage';
import BanishVotePage from './pages/BanishVotePage';
import MurderVotePage from './pages/MurderVotePage';
import BanishRevealPage from './pages/BanishRevealPage';
import ActionsPage from './pages/ActionsPage';
import GameLogsPage from './pages/GameLogsPage';
import GameEventPage from './pages/GameEventPage';
import TestDashboardPage from './pages/TestDashboardPage';
import NotificationsPage from './pages/NotificationsPage';
import GameOptionsPage from './pages/GameOptionsPage';
import MiniGamePage from './pages/MiniGamePage';
import GamePageWrapper from './components/GamePageWrapper';

function NotFoundPage() {
  return <div style={{ padding: '2rem', textAlign: 'center' }}><h1>Not Found</h1></div>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/join/:gameCode" element={<JoinPage />} />
        <Route path="/lobby/:gameCode" element={<LobbyPage />} />
        <Route path="/menu/:gameCode/:playerName" element={<MenuPage />} />
        <Route path="/chat/:gameCode/all" element={<GamePageWrapper><AllChatPage /></GamePageWrapper>} />
        <Route path="/chat/:gameCode/traitors" element={<GamePageWrapper><TraitorChatPage /></GamePageWrapper>} />
        <Route path="/chat/:gameCode/individual" element={<GamePageWrapper><IndividualChatPage /></GamePageWrapper>} />
        <Route path="/chat/:gameCode/dead" element={<GamePageWrapper><DeadChatPage /></GamePageWrapper>} />
        <Route path="/vote/:gameCode/:playerName/banish" element={<GamePageWrapper><BanishVotePage /></GamePageWrapper>} />
        <Route path="/vote/:gameCode/:playerName/murder" element={<GamePageWrapper><MurderVotePage /></GamePageWrapper>} />
        <Route path="/reveal/:gameCode/:playerName" element={<GamePageWrapper><BanishRevealPage /></GamePageWrapper>} />
        <Route path="/actions/:gameCode" element={<GamePageWrapper><ActionsPage /></GamePageWrapper>} />
        <Route path="/logs/:gameCode" element={<GamePageWrapper><GameLogsPage /></GamePageWrapper>} />
        <Route path="/event/:gameCode" element={<GamePageWrapper><GameEventPage /></GamePageWrapper>} />
        <Route path="/test-dashboard" element={<TestDashboardPage />} />
        <Route path="/gameOptions/:gameCode/:playerName" element={<GamePageWrapper><GameOptionsPage /></GamePageWrapper>} />
        <Route path="/mini-game/:gameCode" element={<GamePageWrapper><MiniGamePage /></GamePageWrapper>} />
        <Route path="/notifications/:gameCode/:playerName" element={<NotificationsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
