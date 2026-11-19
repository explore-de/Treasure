/* Behaviour of the layout (main.html and tags/header.html): side nav toggle,
   theme toggle, account panel. Loaded with defer on every page. */
(function () {
  'use strict';

  // The header's menu button is shown only below 66rem (1056px), see
  // treasure-core.css. The side nav may collapse only where that button
  // exists to reopen it; keep in step with theme-init.js.
  var BREAKPOINT = 1056;
  var THEME_ICON_DARK = '<path fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" d="M21.7519 15.0021C20.597 15.484 19.3296 15.7501 18 15.7501C12.6152 15.7501 8.25 11.3849 8.25 6.00011C8.25 4.67052 8.51614 3.40308 8.99806 2.24817C5.47566 3.71798 3 7.19493 3 11.2501C3 16.6349 7.36522 21.0001 12.75 21.0001C16.8052 21.0001 20.2821 18.5245 21.7519 15.0021Z"/>';
  var THEME_ICON_LIGHT = '<path fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" d="M12 3V5.25M18.364 5.63604L16.773 7.22703M21 12H18.75M18.364 18.364L16.773 16.773M12 18.75V21M7.22703 16.773L5.63604 18.364M5.25 12H3M7.22703 7.22703L5.63604 5.63604M15.75 12C15.75 14.0711 14.0711 15.75 12 15.75C9.92893 15.75 8.25 14.0711 8.25 12C8.25 9.92893 9.92893 8.25 12 8.25C14.0711 8.25 15.75 9.92893 15.75 12Z"/>';

  /* ---------- side nav ---------- */

  function sideNav() { return document.querySelector('cds-side-nav'); }

  function initSideNav() {
    var nav = sideNav();
    if (!nav) return;

    // No click handler on the menu button (#navToggle): Carbon's side nav
    // already listens for the button's toggle event, sets expanded itself and
    // keeps the button's state in step. A second toggle here would undo it,
    // and the nav would never open on a small window.

    // Carbon's side nav is position: fixed and does not push the content, so
    // the layout follows its expanded attribute through a class on <html>.
    function updateLayout() {
      document.documentElement.classList.toggle('side-nav-expanded', !!nav.expanded);
    }
    updateLayout();
    new MutationObserver(function (mutations) {
      mutations.forEach(function (mutation) {
        if (mutation.attributeName === 'expanded') updateLayout();
      });
    }).observe(nav, { attributes: true });

    // The initial state is set in theme-init.js. Afterwards only react when the
    // window crosses the breakpoint, so a manual toggle survives small resizes.
    var wasWide = window.innerWidth >= BREAKPOINT;
    var timer;
    window.addEventListener('resize', function () {
      clearTimeout(timer);
      timer = setTimeout(function () {
        var isWide = window.innerWidth >= BREAKPOINT;
        if (isWide === wasWide) return;
        wasWide = isWide;
        nav.expanded = isWide;
      }, 100);
    });
  }

  /* ---------- theme ---------- */

  function applyTheme(theme) {
    var isDark = theme === 'dark';
    document.documentElement.classList.toggle('cds--g100', isDark);
    document.body.classList.toggle('cds--g100', isDark);
    var icon = document.getElementById('themeIcon');
    if (icon) icon.innerHTML = isDark ? THEME_ICON_DARK : THEME_ICON_LIGHT;
  }

  function initTheme() {
    var stored = null;
    try { stored = localStorage.getItem('theme'); } catch (e) {}
    applyTheme(window.__theme || stored || 'light');

    var toggle = document.getElementById('themeToggle');
    if (!toggle) return;
    toggle.addEventListener('click', function () {
      var next = document.documentElement.classList.contains('cds--g100') ? 'light' : 'dark';
      try { localStorage.setItem('theme', next); } catch (e) {}
      window.__theme = next;
      applyTheme(next);
    });
  }

  /* ---------- account panel: closes on outside click and Escape ---------- */

  function initAccountPanel() {
    var button = document.getElementById('profileButton');
    var panel = document.getElementById('profilePanel');
    if (!button || !panel) return;

    function setOpen(open) {
      panel.classList.toggle('active', open);
    }
    button.addEventListener('click', function () { setOpen(!panel.classList.contains('active')); });
    document.addEventListener('click', function (event) {
      if (!panel.contains(event.target) && !button.contains(event.target)) setOpen(false);
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') setOpen(false);
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    initSideNav();
    initTheme();
    initAccountPanel();
  });
})();
