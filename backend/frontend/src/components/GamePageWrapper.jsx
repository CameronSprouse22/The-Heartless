import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Wraps game sub-pages (chat, vote, etc.).
 * If MenuPage auto-navigated here for a single-option event,
 * it will have stored the event's end time in sessionStorage.
 * This wrapper sets a timer to return the player to the menu
 * when that event expires.
 */
export default function GamePageWrapper({ children }) {
  const navigate = useNavigate();

  useEffect(() => {
    const endTime = Number(sessionStorage.getItem('autoNavEventEndTime') || 0);
    const menuPath = sessionStorage.getItem('autoNavMenuPath') || '';

    if (!endTime || !menuPath) return;

    const remaining = endTime - Date.now();
    if (remaining <= 0) {
      sessionStorage.removeItem('autoNavEventEndTime');
      sessionStorage.removeItem('autoNavMenuPath');
      navigate(menuPath, { replace: true });
      return;
    }

    const timer = setTimeout(() => {
      sessionStorage.removeItem('autoNavEventEndTime');
      sessionStorage.removeItem('autoNavMenuPath');
      navigate(menuPath, { replace: true });
    }, remaining);

    return () => clearTimeout(timer);
  }, []);

  return children;
}
