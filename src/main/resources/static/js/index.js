const ORIGIN = window.location.origin;
document.getElementById('login').href =
  `${ORIGIN}/.auth/login/google?post_login_redirect_uri=${encodeURIComponent(ORIGIN + '/products.html')}`;
