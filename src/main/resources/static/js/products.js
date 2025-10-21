(async () => {
  const ORIGIN = window.location.origin;
  const HERE = window.location.href;

  try {
    const res = await fetch('/.auth/me', { credentials: 'include' });
    const data = res.ok ? await res.json() : {};
    const principal = data?.clientPrincipal || null;

    if (principal) {
      const claims = principal.claims || [];
      const get = (k) => claims.find(c => (c.typ || '').toLowerCase().includes(k))?.val || '';
      const email = get('email');
      const name = get('name') || principal.userDetails || email || 'User';

      document.getElementById('whoami').textContent = `Signed in as ${name} (${email})`;
      document.getElementById('authed').style.display = '';

      // Set logout redirect
      document.getElementById('logout').href =
        `${ORIGIN}/.auth/logout?post_logout_redirect_uri=${encodeURIComponent(ORIGIN + '/')}`;

    } else {
      // Not logged in → redirect home
      window.location.href = '/';
    }
  } catch (err) {
    console.error('Error checking auth:', err);
    window.location.href = '/';
  }
})();
