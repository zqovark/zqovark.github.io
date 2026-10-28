/* Only decorative layers move. The page and all links work without this file. */
(() => {
  'use strict';
  const button = document.querySelector('.effects-toggle');
  if (!button) return;
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  let enabled = true;
  try { enabled = localStorage.getItem('bellerophon-effects') !== 'paused'; } catch {}
  function sync() {
    const active = enabled && !reduced.matches;
    document.documentElement.classList.toggle('effects-running', active && !document.hidden);
    button.textContent = active ? 'Pausar efeitos' : 'Ativar efeitos';
    button.setAttribute('aria-pressed', String(active));
    button.disabled = reduced.matches;
    if (reduced.matches) button.textContent = 'Movimento reduzido';
  }
  button.hidden = false;
  button.addEventListener('click', () => {
    enabled = !enabled;
    try { localStorage.setItem('bellerophon-effects', enabled ? 'running' : 'paused'); } catch {}
    sync();
  });
  reduced.addEventListener('change', sync);
  document.addEventListener('visibilitychange', sync);
  sync();
})();
