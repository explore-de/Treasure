/* Runs in <head>, before the first paint: picks the theme and decides whether
   the side nav starts expanded, so nothing flashes or jumps after rendering.
   A static file because the Content-Security-Policy allows no inline scripts. */
(function () {
  'use strict';
  try {
    var t = localStorage.getItem('theme');
    if (t !== 'dark' && t !== 'light') {
      t = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    window.__theme = t;
    if (t === 'dark') document.documentElement.classList.add('cds--g100');
  } catch (e) {}
  window.__navExpanded = window.innerWidth >= 1550;
  if (window.__navExpanded) document.documentElement.classList.add('side-nav-expanded');
})();
