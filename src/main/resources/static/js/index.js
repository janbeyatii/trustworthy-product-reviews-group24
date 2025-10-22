  const params = new URLSearchParams(location.search);
  const returnUrl = params.get('returnUrl') || '/products.html';

  (async () => {
    // If already signed in, skip login page
    try {
      const res = await fetch('/.auth/me', { credentials: 'include' });
      const me = await res.json();
      if (me?.clientPrincipal) {
        location.replace(returnUrl);
        return;
      }
    } catch (_) {}

    // Not signed in → set login link using a RELATIVE redirect
    const login = document.getElementById('login');
    login.href = `/.auth/login/google?post_login_redirect_uri=${encodeURIComponent(returnUrl)}`;
  })();
