fetch('/.auth/me', { credentials: 'include' })
  .then(r => r.json())
  .then(data => {
    const principal = data?.clientPrincipal || null;
    if (principal) {
      const email = (principal.claims || []).find(c => c.typ?.toLowerCase().includes('email'))?.val || '';
      const name  = (principal.claims || []).find(c => c.typ?.toLowerCase().includes('name'))?.val
                    || principal.userDetails || email || 'User';
      document.getElementById('whoami').textContent =
        `Signed in as ${name}${email ? ' (' + email + ')' : ''}.`;
      document.getElementById('authed').style.display = '';
    } else {
      document.getElementById('anon').style.display = '';
    }
  })
  .catch(() => {
    // If something unexpected happens, still show anonymous UI.
    document.getElementById('anon').style.display = '';
  });
