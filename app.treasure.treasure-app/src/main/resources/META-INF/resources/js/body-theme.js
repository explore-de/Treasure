/* Runs right after <body> opens: the dark theme class must be on the body
   before its content is painted. See theme-init.js for where __theme comes from. */
if (window.__theme === 'dark') document.body.classList.add('cds--g100');
