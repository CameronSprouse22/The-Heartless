import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getRevealedVotes, closeReveal } from '../services/api';

function BanishRevealPage({ onClose }) {
  const { gameCode, playerName } = useParams();
  const navigate = useNavigate();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  const [revealedVotes, setRevealedVotes] = useState([]);
  const [totalVotes, setTotalVotes] = useState(0);
  const [revealComplete, setRevealComplete] = useState(false);
  const [error, setError] = useState('');
  const prevCountRef = useRef(0);

  // Poll every 2 seconds until all votes are revealed
  useEffect(() => {
    if (!playerCode) return;

    const poll = async () => {
      try {
        const data = await getRevealedVotes(gameCode, playerCode);
        setRevealedVotes(data.revealedVotes || []);
        setTotalVotes(data.totalVotes || 0);
        setRevealComplete(data.revealComplete || false);
        prevCountRef.current = (data.revealedVotes || []).length;
      } catch (err) {
        setError(err.message || 'Failed to load reveal data');
      }
    };

    poll();
    const interval = setInterval(poll, 2000);
    return () => clearInterval(interval);
  }, [gameCode, playerCode]);

  // Build tally from revealed votes
  const tally = revealedVotes.reduce((acc, v) => {
    if (v.vote) acc[v.vote] = (acc[v.vote] || 0) + 1;
    return acc;
  }, {});

  const tallyEntries = Object.entries(tally).sort((a, b) => b[1] - a[1]);
  const maxCount = tallyEntries.length > 0 ? tallyEntries[0][1] : 1;

  if (error) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#f88' }}>
        {error}
      </div>
    );
  }

  return (
    <div style={{ padding: '1rem', minHeight: '100%', color: '#eee' }}>
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>

        {/* Left column — revealed vote cards */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <h3 style={{ margin: '0 0 0.75rem', fontSize: '1rem', color: '#ccc', borderBottom: '1px solid #444', paddingBottom: '0.4rem' }}>
            Revealed Votes
          </h3>

          {revealedVotes.length === 0 ? (
            <p style={{ color: '#666', fontSize: '0.9rem', fontStyle: 'italic' }}>
              No votes revealed yet…
            </p>
          ) : (
            revealedVotes.map((vote, i) => (
              <div
                key={i}
                style={{
                  background: '#2a2a2a',
                  border: '1px solid #444',
                  borderRadius: '8px',
                  padding: '0.65rem 0.85rem',
                  marginBottom: '0.6rem',
                  animation: 'fadeSlideIn 0.4s ease both',
                }}
              >
                <div style={{ fontSize: '0.95rem' }}>
                  <span style={{ fontWeight: 'bold', color: '#90caf9' }}>{vote.player}</span>
                  <span style={{ color: '#888', margin: '0 0.35rem' }}>voted for</span>
                  <span style={{ fontWeight: 'bold', color: '#ef9a9a' }}>{vote.vote}</span>
                </div>
                {vote.string && (
                  <div style={{
                    marginTop: '0.35rem',
                    fontSize: '0.82rem',
                    color: '#aaa',
                    fontStyle: 'italic',
                    borderTop: '1px solid #383838',
                    paddingTop: '0.3rem',
                  }}>
                    &ldquo;{vote.string}&rdquo;
                  </div>
                )}
              </div>
            ))
          )}
        </div>

        {/* Right column — live tally */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <h3 style={{ margin: '0 0 0.25rem', fontSize: '1rem', color: '#ccc', borderBottom: '1px solid #444', paddingBottom: '0.4rem' }}>
            Current Tally
          </h3>
          <p style={{ margin: '0 0 0.75rem', color: '#aaa', fontSize: '0.85rem' }}>
            {revealComplete
              ? `All ${totalVotes} votes revealed`
              : revealedVotes.length === 0
                ? 'Waiting for votes…'
                : `Revealed ${revealedVotes.length} of ${totalVotes}`}
          </p>

          {tallyEntries.length === 0 ? (
            <p style={{ color: '#666', fontSize: '0.9rem', fontStyle: 'italic' }}>
              No votes counted yet…
            </p>
          ) : (
            tallyEntries.map(([name, count]) => (
              <div key={name} style={{ marginBottom: '0.9rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem', fontSize: '0.9rem' }}>
                  <span style={{ fontWeight: 'bold', color: '#ef9a9a' }}>{name}</span>
                  <span style={{ color: '#ccc' }}>{count} vote{count !== 1 ? 's' : ''}</span>
                </div>
                <div style={{ background: '#333', borderRadius: '4px', height: '8px', overflow: 'hidden' }}>
                  <div
                    style={{
                      width: `${(count / maxCount) * 100}%`,
                      background: count === maxCount ? '#f44336' : '#607d8b',
                      height: '100%',
                      borderRadius: '4px',
                      transition: 'width 0.5s ease',
                    }}
                  />
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div style={{ textAlign: 'center', marginTop: '2rem' }}>
        <button
          disabled={!revealComplete}
          onClick={async () => {
            try { await closeReveal(gameCode, playerCode); } catch { /* ignore */ }
            if (onClose) {
              onClose();
            } else {
              navigate(`/menu/${gameCode}/${encodeURIComponent(playerName || '')}`);
            }
          }}
          style={{
            padding: '0.6rem 2rem',
            background: revealComplete ? '#607d8b' : '#444',
            color: revealComplete ? 'white' : '#888',
            border: 'none',
            borderRadius: '6px',
            fontSize: '1rem',
            cursor: revealComplete ? 'pointer' : 'not-allowed',
          }}
        >
          Close
        </button>
      </div>

      <style>{`
        @keyframes fadeSlideIn {
          from { opacity: 0; transform: translateY(-8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}

export default BanishRevealPage;
