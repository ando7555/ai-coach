import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import './styles.css';

function renderStartupError(root: HTMLElement, error: unknown) {
  const message = error instanceof Error ? error.message : 'The app could not start in this browser.';

  root.innerHTML = `
    <main class="startup-fallback">
      <section>
        <h1>PitchMind Intelligence Portal</h1>
        <p>The application shell could not start. Refresh the page, or open the app in a current Chrome, Edge, Firefox, or Safari browser.</p>
        <pre></pre>
      </section>
    </main>
  `;

  root.querySelector('pre')?.append(document.createTextNode(message));
}

function mountApp() {
  const root = document.getElementById('root');

  if (!root) {
    return;
  }

  try {
    createRoot(root).render(
      <StrictMode>
        <App />
      </StrictMode>
    );
  } catch (error) {
    renderStartupError(root, error);
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', mountApp, { once: true });
} else {
  mountApp();
}
