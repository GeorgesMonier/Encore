import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';

const localHosts = new Set(['localhost', '127.0.0.1', '[::1]']);
if (
  import.meta.env.PROD
  && window.location.protocol === 'http:'
  && !localHosts.has(window.location.hostname)
) {
  window.location.replace(`https://${window.location.host}${window.location.pathname}${window.location.search}${window.location.hash}`);
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
