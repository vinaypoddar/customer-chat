import { createRoot } from 'react-dom/client';
import App from './App';

// Polyfill global for sockjs-client
if (typeof window.global === 'undefined') {
    window.global = window.globalThis;
}

const container = document.getElementById('root');
if (container) {
    const root = createRoot(container);
    root.render(<App />);
}