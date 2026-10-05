/* Original pixel field: slow polygonal currents and seeded micro geometry. */
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
  let time = 0, frame = 0, previous = 0;
  const hero = document.querySelector('.hero');
  let heroLayer;
  let scrollFrame = 0;
  // Stable randomness: regions keep their identity instead of flickering every frame.
  function hash(n) {
    const value = Math.sin(n * 127.1 + 311.7) * 43758.5453;
    return value - Math.floor(value);
  }
  function layer(parent, className) {
    const canvas = document.createElement('canvas');
    canvas.className = className;
    canvas.setAttribute('aria-hidden', 'true');
    parent.prepend(canvas);
    const ctx = canvas.getContext('2d', {alpha: false});
    if (!ctx) { canvas.remove(); return; }
    const item = {canvas, ctx};
    layers.push(item);
    new ResizeObserver(() => {
      item.image = null;
      render(item, time);
      if (item === heroLayer) parallax();
    }).observe(parent);
    return item;
  }
  layer(document.body, 'pixel-background');
  if (hero) {
    heroLayer = layer(hero, 'pixel-hero');
    const label = document.createElement('span');
    label.className = 'pixel-caption notranslate';
    label.setAttribute('translate', 'no');
    label.textContent = '01 / PIXEL CURRENTS — GEOMETRIC FIELD';
    label.setAttribute('aria-hidden', 'true');
    hero.append(label);
  }
  function render(item, t) {
    const isHero = item.canvas.className === 'pixel-hero';
    const width = isHero ? hero.clientWidth + 64 : window.innerWidth;
    const height = isHero ? hero.clientHeight + 64 : window.innerHeight;
    // Integer CSS-pixel scaling keeps pixel edges aligned and avoids soft resampling.
    // X/400 is the motif unit; a few units make each micro triangle identifiable.
    const step = Math.max(2, Math.ceil(width / 650));
    const w = Math.max(1, Math.ceil(width / step));
    const h = Math.max(1, Math.ceil(height / step));
    if (!item.image || item.canvas.width !== w || item.canvas.height !== h) {
      item.canvas.width = w; item.canvas.height = h;
      item.canvas.style.width = `${w * step}px`;
      item.canvas.style.height = `${h * step}px`;
      item.image = item.ctx.createImageData(w, h);
      item.unit = Math.max(1, width / 400 / step);
      // Each small region has a fixed polygon, location and flow phase.
      item.regions = Array.from({length: 24}, (_, index) => {
        const seed = index + (isHero ? 51 : 1);
        return {
          x: hash(seed) * w, y: hash(seed + 101) * h,
          radius: (14 + hash(seed + 41) * 25) * item.unit,
          phase: hash(seed + 73) * Math.PI * 2,
          kind: Math.floor(hash(seed + 23) * 3)
        };
      });
    }
    const data = item.image.data;
    // Eighty-second round trip, starting violet, with restrained brightness.
    const mix = (1 - Math.cos(t * Math.PI * 2 / 80)) / 2;
    const color = [175 + 54 * mix, 126 + 116 * mix, 237 - 194 * mix].map(Math.round);
    const unit = item.unit;
    const stride = Math.max(9, Math.round(unit * 16));
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const u = x / w * 8, v = y / h * 5;
        const a = u + .7 * Math.sin(v * 1.6 + t * .018) + .28 * Math.cos(u * 1.7 - v + t * .013);
        const b = v + .5 * Math.cos(u * 1.2 - t * .015) + .3 * Math.sin(v * 2 + u);
        const f = Math.sin(a * 2.7 + Math.sin(b * 2.3)) + Math.cos(b * 3.4 - a * .7);
        let band = ((f * 5 + t * .035) % 1 + 1) % 1;
        // Sparse flowing contours; empty space is part of the composition.
        let on = band < .045 && (x * 3 + y * 7) % 9 > 2;
        const i = (y * w + x) * 4;
        data[i] = on ? color[0] : 0;
        data[i+1] = on ? color[1] : 0;
        data[i+2] = on ? color[2] : 0;
        data[i+3] = 255;
      }
    }
    function pixel(x, y) {
      x = Math.round(x); y = Math.round(y);
      if (x < 0 || y < 0 || x >= w || y >= h) return;
      const i = (y * w + x) * 4;
      data[i] = color[0]; data[i+1] = color[1]; data[i+2] = color[2];
    }
    function line(x0, y0, x1, y1) {
      const length = Math.max(Math.abs(x1-x0), Math.abs(y1-y0));
      for (let n = 0; n <= Math.ceil(length); n++) {
        const p = length ? Math.min(1, n / length) : 0;
        pixel(x0 + (x1-x0)*p, y0 + (y1-y0)*p);
      }
    }
    // Local polygonal eddies: thin nested triangles, squares and diamonds.
    for (const region of item.regions) {
      const phase = (1 + Math.sin(t * .12 + region.phase)) / 2;
      const count = region.kind === 0 ? 3 : 4;
      for (let ring = 0; ring < 3; ring++) {
        const radius = region.radius * (.42 + ring * .15 + phase * .12);
        const rotation = region.kind === 1 ? Math.PI / 4 : -Math.PI / 2;
        const points = Array.from({length:count}, (_, n) => [
          region.x + Math.cos(rotation + n * Math.PI * 2 / count) * radius,
          region.y + Math.sin(rotation + n * Math.PI * 2 / count) * radius
        ]);
        // Broken contours merge with the fluid instead of becoming solid badges.
        for (let edge = 0; edge < count; edge++) {
          const start = points[edge], end = points[(edge+1)%count];
          const segment = .48 + .15 * Math.sin(t * .04 + region.phase + edge);
          line(start[0], start[1], start[0]+(end[0]-start[0])*segment, start[1]+(end[1]-start[1])*segment);
        }
      }
    }
    // Many simultaneous tiny polygons, anchored to scattered seeded cells.
    for (let cy = 0; cy < h / stride; cy++) {
      for (let cx = 0; cx < w / stride; cx++) {
        const seed = cx * 71 + cy * 137 + 907;
        if (hash(seed) > .22) continue;
        const drift = Math.sin(t * .045 + seed) * unit * 1.5;
        const x = cx * stride + hash(seed+5) * stride + drift;
        const y = cy * stride + hash(seed+7) * stride;
        const radius = Math.max(1.5, unit * (1 + hash(seed+11)));
        if (hash(seed+3) < .5) {
          line(x-radius,y+radius,x,y-radius);
          line(x,y-radius,x+radius,y+radius);
          line(x+radius,y+radius,x-radius,y+radius);
        } else {
          line(x-radius,y-radius,x+radius,y-radius);
          line(x+radius,y-radius,x+radius,y+radius);
          line(x+radius,y+radius,x-radius,y+radius);
          line(x-radius,y+radius,x-radius,y-radius);
        }
      }
    }
    item.ctx.putImageData(item.image, 0, 0);
  }
  function parallax() {
    scrollFrame = 0;
    if (!heroLayer) return;
    const bounds = hero.getBoundingClientRect();
    const offset = paused || reduced.matches ? 0 : Math.max(-24, Math.min(24, -bounds.top * .055));
    // Quantized translation: the heading stays still, only the field moves.
    heroLayer.canvas.style.transform = `translateY(${Math.round(offset / 2) * 2}px)`;
  }
  window.addEventListener('scroll', () => {
    if (!scrollFrame && !paused && !reduced.matches) scrollFrame = requestAnimationFrame(parallax);
  }, {passive:true});
  window.addEventListener('resize', () => {
    layers.forEach(item => { item.image = null; render(item, time); });
    parallax();
  }, {passive:true});
  function loop(now) {
    frame = 0;
    if (paused || document.hidden) return;
    if (now - previous >= 66) {
      time += Math.min((now - previous) / 1000 || .066, .15);
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
    cancelAnimationFrame(scrollFrame); scrollFrame = 0;
    parallax();
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
