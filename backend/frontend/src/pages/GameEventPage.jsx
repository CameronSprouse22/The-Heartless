import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ToggleButton from '../components/ToggleButton';
import CountdownTimer from '../components/CountdownTimer';
import PlayerStatusList from '../components/PlayerStatusList';
import { getEventState, getEventPlayers } from '../services/api';
import { connect, disconnect, subscribe, send } from '../services/websocket';

export default function GameEventPage() {
  const { gameCode } = useParams();
  const navigate = useNavigate();
  const playerCode = sessionStorage.getItem('playerCode') || localStorage.getItem('playerCode');
  const [config, setConfig] = useState(null);
  const [selectedItems, setSelectedItems] = useState(new Set());
  const [textInput, setTextInput] = useState('');
  const [submissionStatus, setSubmissionStatus] = useState('NONE');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [players, setPlayers] = useState([]);
  const [resolved, setResolved] = useState(false);
  const [resolvedResult, setResolvedResult] = useState(null);
  const [disagreement, setDisagreement] = useState('');
  const stompConnected = useRef(false);

  // Load event state on mount
  useEffect(() => {
    if (!playerCode) { navigate('/'); return; }

    getEventState(gameCode, playerCode)
      .then((data) => {
        setConfig(data.config);
        if (data.mySelection && data.mySelection.selectedItems) {
          setSelectedItems(new Set(data.mySelection.selectedItems));
          if (data.mySelection.textInput) setTextInput(data.mySelection.textInput);
          if (data.mySelection.submissionStatus) setSubmissionStatus(data.mySelection.submissionStatus);
        }
        if (data.resolved) {
          setResolved(true);
          setResolvedResult(data.result);
        }
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to load event');
        setLoading(false);
      });
  }, [gameCode, playerCode, navigate]);

  // Connect to STOMP once config is loaded
  useEffect(() => {
    if (!config || !playerCode) return;

    connect(playerCode, () => {
      stompConnected.current = true;
      subscribe(`/topic/games/${gameCode}/event`, (msg) => {
        if (msg.type === 'SELECTION_UPDATE' || msg.type === 'SUBMISSION_UPDATE') {
          setPlayers((prev) => {
            const idx = prev.findIndex((p) => p.playerId === msg.playerId);
            const updated = {
              playerId: msg.playerId,
              playerName: msg.playerName,
              selectedItems: msg.selectedItems,
              submissionStatus: msg.submissionStatus,
            };
            if (idx >= 0) {
              const next = [...prev];
              next[idx] = updated;
              return next;
            }
            return [...prev, updated];
          });
        } else if (msg.type === 'DISAGREEMENT') {
          setDisagreement(msg.message);
          setSubmissionStatus('SELECTED');
          setTimeout(() => setDisagreement(''), 5000);
        } else if (msg.type === 'EVENT_RESOLVED') {
          setResolved(true);
          setResolvedResult(msg);
        } else if (msg.type === 'ROUND_STARTED') {
          // New round started — reset state and re-fetch event config
          setResolved(false);
          setResolvedResult(null);
          setSelectedItems(new Set());
          setTextInput('');
          setSubmissionStatus('NONE');
          setDisagreement('');
          setPlayers([]);
          getEventState(gameCode, playerCode)
            .then((data) => {
              setConfig(data.config);
              if (data.mySelection && data.mySelection.selectedItems) {
                setSelectedItems(new Set(data.mySelection.selectedItems));
                if (data.mySelection.textInput) setTextInput(data.mySelection.textInput);
                if (data.mySelection.submissionStatus) setSubmissionStatus(data.mySelection.submissionStatus);
              }
            })
            .catch(() => {});
        }
      });

      // Re-fetch state on every (re)connect to catch up on missed messages
      getEventState(gameCode, playerCode)
        .then((data) => {
          if (data.mySelection && data.mySelection.selectedItems) {
            setSelectedItems(new Set(data.mySelection.selectedItems));
            if (data.mySelection.textInput) setTextInput(data.mySelection.textInput);
            if (data.mySelection.submissionStatus) setSubmissionStatus(data.mySelection.submissionStatus);
          }
          if (data.resolved) {
            setResolved(true);
            setResolvedResult(data.result);
          }
        })
        .catch(() => {});

      if (config.showOthersSelections) {
        getEventPlayers(gameCode, playerCode)
          .then((data) => setPlayers(data.players || []))
          .catch(() => {});
      }
    }, () => {
      stompConnected.current = false;
    });

    return () => {
      disconnect();
      stompConnected.current = false;
    };
  }, [config, playerCode, gameCode]);

  const handleToggle = (item) => {
    setSelectedItems((prev) => {
      let next;
      if (config.singleAnswer) {
        next = prev.has(item) ? new Set() : new Set([item]);
      } else {
        next = new Set(prev);
        if (next.has(item)) {
          next.delete(item);
        } else {
          next.add(item);
        }
      }

      // Send selection to server via STOMP
      if (stompConnected.current) {
        send(`/app/games/${gameCode}/event/select`, {
          selectedItems: [...next],
          textInput: textInput || null,
        });
      }

      // Re-select after submit reverts to SELECTED
      setSubmissionStatus((prev) => prev === 'SUBMITTED' ? 'SELECTED' : prev);

      return next;
    });
  };

  const handleTextInput = (e) => {
    const val = e.target.value;
    setTextInput(val);

    // Also send selection update with new text
    if (stompConnected.current) {
      send(`/app/games/${gameCode}/event/select`, {
        selectedItems: [...selectedItems],
        textInput: val || null,
      });
    }

    if (submissionStatus === 'SUBMITTED') {
      setSubmissionStatus('SELECTED');
    }
  };

  const isSubmitEnabled = () => {
    if (!config || submissionStatus === 'SUBMITTED') return false;
    const count = selectedItems.size;
    if (count < config.minNumberSelectedToSubmit) return false;
    if (config.maxNumberSelectedToSubmit > 0 && count > config.maxNumberSelectedToSubmit) return false;
    if (config.inputString && (!textInput || !textInput.trim())) return false;
    return count > 0;
  };

  const handleSubmit = () => {
    if (!stompConnected.current) return;
    send(`/app/games/${gameCode}/event/submit`, {});
    setSubmissionStatus('SUBMITTED');
  };

  const handleCancelSubmit = () => {
    if (!stompConnected.current) return;
    send(`/app/games/${gameCode}/event/cancel-submit`, {});
    setSubmissionStatus('SELECTED');
  };

  const isDisabled = resolved || submissionStatus === 'SUBMITTED';

  if (loading) {
    return <div className="page-container"><p>Loading...</p></div>;
  }

  if (error) {
    return (
      <div className="page-container">
        <p className="error">{error}</p>
      </div>
    );
  }

  if (!config) {
    return <div className="page-container"><p>No active event.</p></div>;
  }

  return (
    <div className="page-container">
      <h2>{config.title}</h2>
      {config.prompt && <p>{config.prompt}</p>}
      {config.endTime > 0 && !resolved && <CountdownTimer endTime={config.endTime} />}
      {disagreement && <p className="error">{disagreement}</p>}
      {resolved && resolvedResult && (
        <div className="card" style={{ marginBottom: '1rem', padding: '1rem' }}>
          <p className="success">Event resolved: {resolvedResult.resolutionType}</p>
        </div>
      )}
      <div style={{ marginTop: '1rem' }}>
        {config.listOfItems.map((item) => (
          <ToggleButton
            key={item}
            label={item}
            selected={selectedItems.has(item)}
            onClick={() => handleToggle(item)}
            disabled={isDisabled}
          />
        ))}
      </div>
      {config.inputString && (
        <div style={{ marginTop: '1rem' }}>
          <input
            type="text"
            className="card"
            placeholder="Enter your answer..."
            value={textInput}
            onChange={handleTextInput}
            disabled={isDisabled}
            style={{ width: '100%', padding: '0.75rem', fontSize: '1rem', boxSizing: 'border-box' }}
          />
        </div>
      )}
      {config.showOthersSelections && <PlayerStatusList players={players} />}
      {!resolved && (
        <div style={{ marginTop: '1.5rem', textAlign: 'center' }}>
          {config.playersMustAgree && submissionStatus === 'SUBMITTED' ? (
            <button onClick={handleCancelSubmit} style={{ padding: '0.75rem 2rem', fontSize: '1.1rem' }}>
              Cancel Submit
            </button>
          ) : (
            <button
              onClick={handleSubmit}
              disabled={!isSubmitEnabled()}
              style={{ padding: '0.75rem 2rem', fontSize: '1.1rem' }}
            >
              {submissionStatus === 'SUBMITTED' ? 'Submitted' : 'Submit'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
