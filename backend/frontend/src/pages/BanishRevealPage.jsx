import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getRevealedVotes, markEventReady } from '../services/api';

// --- Slot Machine Component ---
function SlotMachine({ candidates, pickedName, onDone }) {
  const [displayName, setDisplayName] = useState(candidates[0] || '');
  const [phase, setPhase] = useState('spinning'); // 'spinning' | 'slowing' | 'done'
  const frameRef = useRef(null);
  const startTimeRef = useRef(Date.now());
  const SPIN_DURATION = 2800;  // fast spin ms
  const SLOW_DURATION = 1800;  // slow-down ms
  const TOTAL = SPIN_DURATION + SLOW_DURATION;

  useEffect(() => {
    let idx = 0;
    let delay = 80;

    const tick = () => {
      const elapsed = Date.now() - startTimeRef.current;
      if (elapsed < SPIN_DURATION) {
        // Fast cycling through all candidates
        idx = (idx + 1) % candidates.length;
        setDisplayName(candidates[idx]);
        setPhase('spinning');
        delay = 80;
        frameRef.current = setTimeout(tick, delay);
      } else if (elapsed < TOTAL) {
        // Slowing down — interpolate interval from 80ms → 400ms
        const progress = (elapsed - SPIN_DURATION) / SLOW_DURATION;
        delay = 80 + progress * 320;
        idx = (idx + 1) % candidates.length;
        setDisplayName(candidates[idx]);
        setPhase('slowing');
        frameRef.current = setTimeout(tick, delay);
      } else {
        // Land on the picked name
        setDisplayName(pickedName);
        setPhase('done');
        setTimeout(() => onDone && onDone(), 1200);
      }
    };

    frameRef.current = setTimeout(tick, delay);
    return () => clearTimeout(frameRef.current);
  }, []);

  const glow = phase === 'done'
    ? '0 0 24px 8px #ff5252, 0 0 4px 2px #ff1744'
    : phase === 'slowing'
      ? '0 0 10px 3px #ef9a9a'
      : 'none';

  return (
    <div style={{ textAlign: 'center', margin: '2rem 0' }}>
      <p style={{ color: '#aaa', fontSize: '0.9rem', marginBottom: '1rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
        {phase === 'done' ? '\u2620 Banished by fate \u2620' : 'Drawing lots\u2026'}
      </p>
      <div style={{
        display: 'inline-block',
        background: '#1a1a1a',
        border: `2px solid ${phase === 'done' ? '#f44336' : '#555'}`,
        borderRadius: '12px',
        padding: '1.2rem 3rem',
        minWidth: '220px',
        transition: 'border-color 0.4s',
        boxShadow: glow,
      }}>
        <span style={{
          display: 'block',
          fontSize: '2rem',
          fontWeight: 'bold',
          color: phase === 'done' ? '#f44336' : '#eee',
          transition: phase === 'done' ? 'color 0.4s' : 'none',
          letterSpacing: '0.04em',
        }}>
          {displayName}
        </span>
      </div>
      {phase === 'done' && (
        <p style={{ color: '#f44336', marginTop: '1rem', fontSize: '1rem', fontWeight: 'bold', animation: 'fadeSlideIn 0.5s ease' }}>
          has been banished!
        </p>
      )}
    </div>
  );
}

function BanishRevealPage({ onClose }) {
  const { gameCode, playerName } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  const [revealedVotes, setRevealedVotes] = useState([]);
  const [totalVotes, setTotalVotes] = useState(0);
  const [revealComplete, setRevealComplete] = useState(false);
  const [randomPickedName, setRandomPickedName] = useState(null);
  const [randomPickCandidates, setRandomPickCandidates] = useState([]);
  const [slotDone, setSlotDone] = useState(false);
  const [myConfirmed, setMyConfirmed] = useState(false);
  const [confirmedCount, setConfirmedCount] = useState(0);
  const [requiredCount, setRequiredCount] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const prevCountRef = useRef(0);

  const canClose = revealComplete && (randomPickedName == null || slotDone);

  // Poll every 2 seconds until all votes are revealed
  useEffect(() => {
    if (!playerCode) return;

    const poll = async () => {
      try {
        const data = await getRevealedVotes(gameCode, playerCode);
        setRevealedVotes(data.revealedVotes || []);
        setTotalVotes(data.totalVotes || 0);
        setRevealComplete(data.revealComplete || false);
        if (data.randomPickedName) setRandomPickedName(data.randomPickedName);
        if (data.randomPickCandidates) setRandomPickCandidates(data.randomPickCandidates);
        if (data.myConfirmed) setMyConfirmed(true);
        if (data.confirmedCount !== undefined) setConfirmedCount(data.confirmedCount);
        if (data.requiredCount !== undefined) setRequiredCount(data.requiredCount);
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
        {requiredCount > 0 && (
          <div style={{
            marginBottom: '0.75rem',
            fontSize: '0.85rem',
            color: '#888',
          }}>
            {confirmedCount} / {requiredCount} players confirmed
          </div>
        )}
        {myConfirmed ? (
          <div style={{
            padding: '0.6rem 2rem',
            background: 'rgba(255,255,255,0.04)',
            color: '#bbb',
            border: '1px solid rgba(255,255,255,0.15)',
            borderRadius: '6px',
            fontSize: '1rem',
            fontWeight: 600,
            display: 'inline-block',
          }}>
            ✓ Confirmed — awaiting others…
          </div>
        ) : (
          <button
            disabled={!canClose || confirming}
            onClick={async () => {
              setConfirming(true);
              try { await markEventReady(gameCode, playerCode); setMyConfirmed(true); } catch { /* ignore */ }
              setConfirming(false);
            }}
            style={{
              padding: '0.6rem 2rem',
              background: canClose ? '#2e7d32' : '#616161',
              color: canClose ? 'white' : '#9e9e9e',
              border: 'none',
              borderRadius: '6px',
              fontSize: '1rem',
              fontWeight: 600,
              cursor: canClose ? 'pointer' : 'not-allowed',
            }}
          >
            Ready
          </button>
        )}
      </div>

      {revealComplete && randomPickedName && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.75)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          animation: 'fadeSlideIn 0.3s ease',
        }}>
          <div style={{
            background: '#1e1e1e',
            border: '1px solid #444',
            borderRadius: '16px',
            padding: '2.5rem 3rem',
            minWidth: '320px',
            textAlign: 'center',
            boxShadow: '0 8px 40px rgba(0,0,0,0.7)',
          }}>
            <h2 style={{ margin: '0 0 0.25rem', color: '#eee', fontSize: '1.3rem' }}>Tiebreaker!</h2>
            <p style={{ margin: '0 0 1.5rem', color: '#888', fontSize: '0.9rem' }}>The vote is tied — fate decides.</p>
            <SlotMachine
              candidates={randomPickCandidates.length >= 2 ? randomPickCandidates : [randomPickedName]}
              pickedName={randomPickedName}
              onDone={() => setSlotDone(true)}
            />
          </div>
        </div>
      )}

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
