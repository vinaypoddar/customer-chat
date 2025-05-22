import { createRoot } from 'react-dom/client';
import App from './App';

const container = document.getElementById('root');
if (container) {
    const root = createRoot(container);
    root.render(<App />);
} else {
    console.error('index.tsx: Root element not found');
}