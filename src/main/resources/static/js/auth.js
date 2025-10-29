import { createClient } from 'https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2/+esm';

const supabaseUrl = window.__SUPABASE_URL__;
const supabaseAnonKey = window.__SUPABASE_ANON_KEY__;

if (!supabaseUrl || !supabaseAnonKey) {
    console.error('Supabase credentials are not defined. Set window.__SUPABASE_URL__ and window.__SUPABASE_ANON_KEY__.');
}

export const supabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
    auth: {
        persistSession: true,
        storageKey: 'trustworthy-product-reviews-session'
    }
});

const page = document.body?.dataset?.page ?? '';
const yearEl = document.getElementById('year');
if (yearEl) {
    yearEl.textContent = new Date().getFullYear().toString();
}

const setStatus = (element, message, type = 'info') => {
    if (!element) return;
    element.textContent = message;
    element.classList.remove('auth__status--error', 'auth__status--success');
    if (type === 'error') {
        element.classList.add('auth__status--error');
    } else if (type === 'success') {
        element.classList.add('auth__status--success');
    }
};

const toggleLoading = (button, isLoading, loadingLabel = 'Loading…') => {
    if (!button) return;
    if (isLoading) {
        button.dataset.originalText = button.textContent;
        button.textContent = loadingLabel;
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalText ?? button.textContent;
        button.disabled = false;
    }
};

async function handleLogin(event) {
    event.preventDefault();
    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const statusEl = document.getElementById('login-status');
    const submitButton = document.getElementById('login-submit');

    if (!emailInput?.value || !passwordInput?.value) {
        setStatus(statusEl, 'Email and password are required.', 'error');
        return;
    }

    try {
        toggleLoading(submitButton, true, 'Signing in…');
        const { error } = await supabaseClient.auth.signInWithPassword({
            email: emailInput.value.trim(),
            password: passwordInput.value
        });
        if (error) {
            throw error;
        }
        setStatus(statusEl, 'Logged in successfully! Redirecting…', 'success');
        window.location.href = 'app.html';
    } catch (error) {
        setStatus(statusEl, error.message ?? 'Unable to login. Please try again.', 'error');
    } finally {
        toggleLoading(submitButton, false);
    }
}

async function handleSignup(event) {
    event.preventDefault();
    const emailInput = document.getElementById('signup-email');
    const passwordInput = document.getElementById('signup-password');
    const confirmInput = document.getElementById('signup-confirm');
    const displayNameInput = document.getElementById('signup-display-name');
    const statusEl = document.getElementById('signup-status');
    const submitButton = document.getElementById('signup-submit');

    if (!emailInput?.value || !passwordInput?.value || !confirmInput?.value) {
        setStatus(statusEl, 'Email and password are required.', 'error');
        return;
    }
    if (passwordInput.value !== confirmInput.value) {
        setStatus(statusEl, 'Passwords do not match.', 'error');
        return;
    }

    try {
        toggleLoading(submitButton, true, 'Creating account…');
        const { error } = await supabaseClient.auth.signUp({
            email: emailInput.value.trim(),
            password: passwordInput.value,
            options: {
                data: {
                    display_name: displayNameInput?.value?.trim() || null
                },
                emailRedirectTo: `${window.location.origin}/app.html`
            }
        });
        if (error) {
            throw error;
        }
        setStatus(statusEl, 'Account created! Check your email to confirm your address.', 'success');
    } catch (error) {
        setStatus(statusEl, error.message ?? 'Unable to create account.', 'error');
    } finally {
        toggleLoading(submitButton, false);
    }
}

async function handleResetPassword(event) {
    event.preventDefault();
    const emailInput = document.getElementById('reset-email');
    const statusEl = document.getElementById('reset-status');
    const submitButton = document.getElementById('reset-submit');

    if (!emailInput?.value) {
        setStatus(statusEl, 'Email is required to reset your password.', 'error');
        return;
    }

    try {
        toggleLoading(submitButton, true, 'Sending…');
        const { error } = await supabaseClient.auth.resetPasswordForEmail(emailInput.value.trim(), {
            redirectTo: `${window.location.origin}/app.html`
        });
        if (error) {
            throw error;
        }
        setStatus(statusEl, 'Password reset email sent. Check your inbox!', 'success');
    } catch (error) {
        setStatus(statusEl, error.message ?? 'Unable to send reset email.', 'error');
    } finally {
        toggleLoading(submitButton, false);
    }
}

const switchForm = (target) => {
    const forms = {
        login: document.getElementById('login-form'),
        signup: document.getElementById('signup-form'),
        reset: document.getElementById('reset-form')
    };

    Object.entries(forms).forEach(([key, form]) => {
        if (form) {
            form.classList.toggle('auth__form--hidden', key !== target);
        }
    });

    const tabs = {
        login: document.getElementById('show-login'),
        signup: document.getElementById('show-signup')
    };
    Object.entries(tabs).forEach(([key, tab]) => {
        if (tab) {
            tab.classList.toggle('auth__tab--active', key === target);
        }
    });
};

async function initialiseAuthPage() {
    const loginForm = document.getElementById('login-form');
    const signupForm = document.getElementById('signup-form');
    const resetForm = document.getElementById('reset-form');

    loginForm?.addEventListener('submit', handleLogin);
    signupForm?.addEventListener('submit', handleSignup);
    resetForm?.addEventListener('submit', handleResetPassword);

    document.getElementById('show-login')?.addEventListener('click', () => switchForm('login'));
    document.getElementById('show-signup')?.addEventListener('click', () => switchForm('signup'));
    document.getElementById('switch-to-login')?.addEventListener('click', () => switchForm('login'));
    document.getElementById('switch-to-signup')?.addEventListener('click', () => switchForm('signup'));
    document.getElementById('forgot-password')?.addEventListener('click', () => switchForm('reset'));
    document.getElementById('back-to-login')?.addEventListener('click', () => switchForm('login'));

    const { data } = await supabaseClient.auth.getSession();
    if (data?.session) {
        window.location.href = 'app.html';
    }
}

async function initialiseAppGuards() {
    const { data } = await supabaseClient.auth.getSession();
    if (!data?.session) {
        window.location.replace('index.html');
    }
}

if (page === 'auth' || page === 'landing') {
    initialiseAuthPage().catch((error) => console.error('Failed to initialise auth page', error));
} else if (page === 'app') {
    initialiseAppGuards().catch((error) => console.error('Failed to guard app page', error));
}

export const requireSession = initialiseAppGuards;
