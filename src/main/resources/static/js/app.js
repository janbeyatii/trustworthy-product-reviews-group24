// Fetch identity from Azure Easy Auth
fetch('/.auth/me', { credentials: 'include' })
  .then(res => res.ok ? res.json() : null)
  .then(data => {
    const principal = data?.clientPrincipal || null;

    if (principal) {
      // Pull a friendly name + email from claims
      const claims = principal.claims || [];
      const get = (k) => claims.find(c => (c.typ || '').toLowerCase().includes(k))?.val || '';
      const email = get('email');
      const name  = get('name') || principal.userDetails || email || 'User';

      const who = document.getElementById('whoami');
      if (who) who.textContent = `Signed in as ${name}${email ? ' (' + email + ')' : ''}.`;

      // Show logged-in view
      const authed = document.getElementById('authed');
      if (authed) authed.style.display = '';

    } else {
      // Show anonymous view
      const anon = document.getElementById('anon');
      if (anon) anon.style.display = '';
    }
  })
  .catch(() => {
    // If anything fails, default to anonymous
    const anon = document.getElementById('anon');
    if (anon) anon.style.display = '';
  });
