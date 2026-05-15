import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getMiniGame, submitMiniGameAnswer } from '../services/api';

// How long (ms) to show the correct/wrong flash before moving to the next question
const FEEDBACK_DELAY_MS = 1100;

export default function MiniGamePage({ onClose }) {
  const { gameCode } = useParams();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');

  // Server state
  const [data, setData]             = useState(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');

  // Per-question UI state
  const [submitting, setSubmitting] = useState(false);
  // { option: string, correct: boolean } while feedback flash is showing
  const [feedback, setFeedback]     = useState(null);

  // answers stored locally so we can show them immediately without waiting for a poll
  const myAnswersRef = useRef([]);

  // ── Polling ────────────────────────────────────────────────────────────────
  const load = useCallback(async () => {
    if (!playerCode || !gameCode) return;
    try {
      const result = await getMiniGame(gameCode, playerCode);
      // Sync local answer cache from server (handles page-refresh recovery)
      if (result.myAnswers && result.myAnswers.length > myAnswersRef.current.length) {
        myAnswersRef.current = result.myAnswers;
      }
      setData(result);
      setError('');
    } catch (err) {
      // 409 means the mini game event has ended (game moved on) — go back to menu
      if (err.status === 409) {
        onClose?.();
        return;
      }
      setError(err.message || 'Failed to load mini game');
    } finally {
      setLoading(false);
    }
  }, [gameCode, playerCode]);

  useEffect(() => {
    load();
    const interval = setInterval(load, 3000);
    return () => clearInterval(interval);
  }, [load]);

  // ── Answer handler ─────────────────────────────────────────────────────────
  const handleAnswer = async (option) => {
    if (submitting || feedback) return;
    setSubmitting(true);
    try {
      const result = await submitMiniGameAnswer(gameCode, playerCode, option);
      myAnswersRef.current = [...myAnswersRef.current, option];
      // Show feedback flash
      setFeedback({ option, correct: result.correct });
      setTimeout(async () => {
        setFeedback(null);
        setSubmitting(false);
        // Refresh to get next question / done state
        await load();
      }, FEEDBACK_DELAY_MS);
    } catch (err) {
      setSubmitting(false);
      // If the server says the answer was already recorded, just refresh
      await load();
    }
  };

  // ── Derived state (must be computed before all hooks) ─────────────────────
  const totalQuestions  = data?.totalQuestions ?? 0;
  const currentIdx      = myAnswersRef.current.length;
  // Guard: myDone is only true when data has actually loaded (avoids 0 >= 0 being true on null data)
  const myDone          = data != null && (data.myDone || currentIdx >= totalQuestions);
  const completedCount  = data?.completedCount ?? 0;
  const requiredCount   = data?.requiredCount ?? 0;
  const isTraitor       = data?.isTraitor ?? false;
  const question        = data?.question;   // null when player is done
  const options         = question?.options ?? [];

  // Traitors auto-close back to menu as soon as they finish the mini game
  // MUST be above any early returns to satisfy React rules of hooks
  useEffect(() => {
    if (myDone && isTraitor) {
      onClose?.();
    }
  }, [myDone, isTraitor, onClose]);

  // ── Render ─────────────────────────────────────────────────────────────────
  if (loading && !data) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#ccc' }}>
        Loading mini game\u2026
      </div>
    );
  }

  if (error && !data) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#ef5350' }}>
        {error}
      </div>
    );
  }

  // ── Waiting screen (all questions answered) ────────────────────────────────
  if (myDone) {
    return (
      <div style={{
        padding: '2rem',
        maxWidth: '420px',
        margin: '0 auto',
        color: '#e0e0e0',
        fontFamily: 'sans-serif',
        textAlign: 'center',
      }}>
        <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>\u2713</div>
        <h2 style={{ fontSize: '1.2rem', color: '#a5d6a7', marginBottom: '0.5rem' }}>
          Mini Game Complete!
        </h2>
        <p style={{ color: '#888', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
          You answered all {totalQuestions} question{totalQuestions !== 1 ? 's' : ''}.
        </p>

        {isTraitor ? (
          /* Traitors: direct them back to the menu where murder vote is waiting */
          <div style={{
            background: '#1e1e2e',
            border: '1px solid #b71c1c',
            borderRadius: '12px',
            padding: '1.25rem',
            marginBottom: '1rem',
          }}>
            <p style={{ fontSize: '0.9rem', color: '#ef9a9a', marginBottom: '1rem' }}>
              \u2694\ufe0f The murder vote is ready — return to the menu to cast your vote.
            </p>
            <button
              onClick={() => onClose?.()}
              style={{
                padding: '0.6rem 1.4rem',
                background: '#c62828',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                fontSize: '0.95rem',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Go to Murder Vote \u2192
            </button>
          </div>
        ) : (
          /* Faithful: show progress bar while waiting */
          <div style={{
            background: '#1e1e2e',
            border: '1px solid #444',
            borderRadius: '12px',
            padding: '1.25rem',
            marginBottom: '1rem',
          }}>
            <p style={{ fontSize: '0.85rem', color: '#aaa', marginBottom: '0.75rem' }}>
              Waiting for other players\u2026
            </p>
            {/* Progress bar */}
            <div style={{
              background: '#333',
              borderRadius: '999px',
              height: '8px',
              marginBottom: '0.5rem',
              overflow: 'hidden',
            }}>
              <div style={{
                width: `${requiredCount > 0 ? Math.round((completedCount / requiredCount) * 100) : 0}%`,
                height: '100%',
                background: '#43a047',
                borderRadius: '999px',
                transition: 'width 0.4s',
              }} />
            </div>
            <p style={{ fontSize: '1rem', fontWeight: 700, color: '#e0e0e0' }}>
              {completedCount} / {requiredCount} players completed
            </p>
          </div>
        )}
      </div>
    );
  }

  // ── Active question ────────────────────────────────────────────────────────
  return (
    <div style={{
      padding: '1.5rem',
      maxWidth: '420px',
      margin: '0 auto',
      color: '#e0e0e0',
      fontFamily: 'sans-serif',
    }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h2 style={{ fontSize: '1.1rem', margin: 0, color: '#fff' }}>Mini Game</h2>
        <span style={{ fontSize: '0.8rem', color: '#888' }}>
          {currentIdx + 1} / {totalQuestions}
        </span>
      </div>

      {/* Progress dots */}
      <div style={{ display: 'flex', gap: '6px', marginBottom: '1.25rem', justifyContent: 'center' }}>
        {Array.from({ length: totalQuestions }).map((_, i) => (
          <div key={i} style={{
            width: '10px',
            height: '10px',
            borderRadius: '50%',
            background: i < currentIdx ? '#43a047' : i === currentIdx ? '#fff' : '#444',
            border: i === currentIdx ? '2px solid #fff' : '2px solid transparent',
            transition: 'background 0.2s',
          }} />
        ))}
      </div>

      {/* Question card */}
      <div style={{
        background: '#1e1e2e',
        border: '1px solid #333',
        borderRadius: '10px',
        padding: '1.25rem',
        marginBottom: '1.25rem',
        textAlign: 'center',
        minHeight: '70px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
        <p style={{ fontSize: '1.05rem', margin: 0, lineHeight: 1.5 }}>
          {question?.question}
        </p>
      </div>

      {/* Option buttons — tap to submit immediately */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(70px, 1fr))',
        gap: '0.6rem',
      }}>
        {options.map((opt) => {
          const isFlashed  = feedback && feedback.option === opt;
          const isCorrect  = isFlashed && feedback.correct;
          const isWrong    = isFlashed && !feedback.correct;

          let bg     = '#2a2a3a';
          let border = '#444';
          let color  = '#ccc';
          if (isCorrect) { bg = '#1b5e20'; border = '#43a047'; color = '#fff'; }
          else if (isWrong) { bg = '#7f0000'; border = '#ef5350'; color = '#fff'; }

          return (
            <button
              key={opt}
              onClick={() => handleAnswer(opt)}
              disabled={submitting || !!feedback}
              style={{
                padding: '0.75rem 0.5rem',
                fontSize: '1.1rem',
                fontWeight: 700,
                background: bg,
                color,
                border: `2px solid ${border}`,
                borderRadius: '8px',
                cursor: submitting || feedback ? 'not-allowed' : 'pointer',
                transition: 'background 0.15s, border 0.15s',
              }}
            >
              {opt}
            </button>
          );
        })}
      </div>

      {feedback && (
        <p style={{
          textAlign: 'center',
          marginTop: '1rem',
          fontSize: '0.95rem',
          fontWeight: 600,
          color: feedback.correct ? '#a5d6a7' : '#ef9a9a',
        }}>
          {feedback.correct ? '\u2713 Correct!' : '\u2717 Wrong!'}
        </p>
      )}

      {/* Bottom player count */}
      <p style={{ textAlign: 'center', marginTop: '1.25rem', fontSize: '0.78rem', color: '#666' }}>
        {completedCount} / {requiredCount} players finished all questions
      </p>
    </div>
  );
}
