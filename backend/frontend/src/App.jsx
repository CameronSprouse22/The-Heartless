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
import ActionsPage from './pages/ActionsPage';
import GameLogsPage from './pages/GameLogsPage';
import GameEventPage from './pages/GameEventPage';
import TestDashboardPage from './pages/TestDashboardPage';

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
        <Route path="/chat/:gameCode/all" element={<AllChatPage />} />
        <Route path="/chat/:gameCode/traitors" element={<TraitorChatPage />} />
        <Route path="/chat/:gameCode/individual" element={<IndividualChatPage />} />
        <Route path="/chat/:gameCode/dead" element={<DeadChatPage />} />
        <Route path="/vote/:gameCode/:playerName/banish" element={<BanishVotePage />} />
        <Route path="/vote/:gameCode/:playerName/murder" element={<MurderVotePage />} />
        <Route path="/actions/:gameCode" element={<ActionsPage />} />
        <Route path="/logs/:gameCode" element={<GameLogsPage />} />
        <Route path="/event/:gameCode" element={<GameEventPage />} />
        <Route path="/test-dashboard" element={<TestDashboardPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
