/* Runs right after the side nav element, before the ui-shell module upgrades
   it, so it renders expanded straight away on wide windows. */
if (window.__navExpanded) document.querySelector('cds-side-nav').setAttribute('expanded', '');
