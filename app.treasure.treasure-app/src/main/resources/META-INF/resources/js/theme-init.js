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
  // 1056px (66rem) is where Carbon starts showing the menu button in the
  // header. Above it the button is hidden, so the side nav must stay open
  // there or nobody could bring it back. Keep in step with shell.js.
  window.__navExpanded = window.innerWidth >= 1056;
  if (window.__navExpanded) document.documentElement.classList.add('side-nav-expanded');
})();
