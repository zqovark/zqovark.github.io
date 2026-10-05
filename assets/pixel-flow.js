/* Original quantized stream field. No reference artwork or video is embedded. */
(() => {
  'use strict';
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  let paused = reduced.matches;
  try { paused ||= localStorage.getItem('pixel-motion') === 'paused'; } catch {}
  const button = document.createElement('button');
  button.className = 'motion-toggle notranslate';
  button.type = 'button';
  button.setAttribute('translate', 'no');
  document.querySelector('.site-header')?.append(button);
  const layers = [];
  function layer(parent, className, size) {
    const canvas = document.createElement('canvas');
    canvas.className = className;
    canvas.setAttribute('aria-hidden', 'true');
    parent.prepend(canvas);
    const ctx = canvas.getContext('2d', {alpha: false});
    if (!ctx) { canvas.remove(); return; }
    const item = {canvas, ctx, size, tick: -1};
    layers.push(item);
    new ResizeObserver(() => { item.tick = -1; render(item, time); }).observe(canvas);
  }
  let time = 0, frame = 0, previous = 0;
  layer(document.body, 'pixel-background', 5);
  const hero = document.querySelector('.hero');
  if (hero) {
    layer(hero, 'pixel-hero', 3);
    const label = document.createElement('span');
    label.className = 'pixel-caption notranslate';
    label.setAttribute('translate', 'no');
    label.textContent = '01 / PIXEL CURRENTS — GENERATIVE FIELD';
    label.setAttribute('aria-hidden', 'true');
    hero.append(label);
  }
  function render(item, t) {
    const w = Math.min(460, Math.max(1, Math.ceil(item.canvas.clientWidth / item.size)));
    const h = Math.min(260, Math.max(1, Math.ceil(item.canvas.clientHeight / item.size)));
    if (!item.image || item.canvas.width !== w || item.canvas.height !== h) {
      item.canvas.width = w; item.canvas.height = h;
      item.image = item.ctx.createImageData(w, h);
    }
    const data = item.image.data;
    // Domain warping yields eddies; thresholded bands break into pixel trails.
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const u = x / w * 8, v = y / h * 5;
        const a = u + .8 * Math.sin(v * 1.6 + t * .15) + .32 * Math.cos(u * 1.7 - v + t * .11);
        const b = v + .6 * Math.cos(u * 1.2 - t * .12) + .35 * Math.sin(v * 2 + u);
        const f = Math.sin(a * 2.7 + Math.sin(b * 2.3)) + Math.cos(b * 3.4 - a * .7) + .48 * Math.sin(a * 4.1 + b * 2.8 + t * .09);
        const band = ((f * 7 + t * .65) % 1 + 1) % 1;
        const grain = ((x * 13 + y * 7 + Math.floor(f * 31)) % 11 + 11) % 11;
        const on = (band < .105 && grain > 1) || (f > .85 && band < .4 && (x + y) % 3 !== 0);
        const i = (y * w + x) * 4;
        data[i] = on ? 229 : 0; data[i+1] = on ? 242 : 0; data[i+2] = on ? 43 : 0; data[i+3] = 255;
      }
    }
    item.ctx.putImageData(item.image, 0, 0);
  }
  function loop(now) {
    frame = 0;
    if (paused || document.hidden) return;
    if (now - previous >= 50) {
      time += Math.min((now - previous) / 1000 || .05, .1);
      previous = now;
      layers.forEach(item => render(item, time));
    }
    frame = requestAnimationFrame(loop);
  }
  function sync() {
    button.textContent = paused ? '▶ Animar pixels' : 'Ⅱ Pausar pixels';
    button.setAttribute('aria-label', paused ? 'Ativar animação dos pixels' : 'Pausar animação dos pixels');
    button.setAttribute('aria-pressed', String(!paused));
    cancelAnimationFrame(frame); frame = 0; previous = performance.now();
    if (!paused && !document.hidden) frame = requestAnimationFrame(loop);
  }
  button.addEventListener('click', () => {
    paused = !paused;
    try { localStorage.setItem('pixel-motion', paused ? 'paused' : 'running'); } catch {}
    sync();
  });
  reduced.addEventListener('change', () => { paused = reduced.matches; sync(); });
  document.addEventListener('visibilitychange', sync);
  layers.forEach(item => render(item, time));
  sync();
})();
