// Polyfill global for sockjs-client
if (typeof window.global === 'undefined') {
    window.global = window.globalThis;
    console.log('index.js: Applied global polyfill');
} else {
    console.log('index.js: global already defined');
}

// Import main.tsx
import './main.tsx';