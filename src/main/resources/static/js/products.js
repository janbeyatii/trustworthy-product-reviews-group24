(() => {
  const ORIGIN = window.location.origin;

  // Build the exact URL we want to come back to (path + query + hash)
  const here = location.pathname + location.search + location.hash;
  const toLogin = () => {
    // Send them to the login page (index) with a returnUrl back to THIS page
    const loginUrl = `/?returnUrl=${encodeURIComponent(here)}`;
    window.location.replace(loginUrl);
  };

  async function init() {
    try {
      const res = await fetch('/.auth/me', {
        credentials: 'include',
        cache: 'no-store' // avoid stale cached anon response
      });
      if (!res.ok) throw new Error(`/.auth/me status ${res.status}`);

      const data = await res.json();
      console.log('auth me →', data); // remove in prod

      const principal = data?.clientPrincipal || null;
      if (!principal) return toLogin(); // ⟵ key change

      const claims = principal.claims || [];
      const get = (k) =>
        claims.find(c => (c.typ || '').toLowerCase().includes(k))?.val || '';

      const email = get('email');
      const name  = get('name') || principal.userDetails || email || 'User';

      const who = document.getElementById('whoami');
      if (who) {
        who.textContent = `Signed in as ${name}${email ? ' (' + email + ')' : ''}`;
      }

      const authed = document.getElementById('authed');
      if (authed) authed.style.display = '';

      const logout = document.getElementById('logout');
      if (logout) {
        // Use a RELATIVE post-logout redirect (no allow-list needed)
        logout.href = `/.auth/logout?post_logout_redirect_uri=${encodeURIComponent('/')}`;
      }
    } catch (err) {
      console.error('Auth check failed:', err);
      toLogin();
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
