import { supabaseClient, requireSession } from './auth.js';

const userGreeting = document.getElementById('user-greeting');
const displayNameInput = document.getElementById('profile-display-name');
const profileStatus = document.getElementById('profile-status');
const passwordInput = document.getElementById('profile-password');
const passwordStatus = document.getElementById('password-status');
const reviewStatus = document.getElementById('review-status');
const followStatus = document.getElementById('follow-status');

const productsList = document.getElementById('products-list');
const myReviewsList = document.getElementById('my-reviews-list');
const followingList = document.getElementById('following-list');
const leaderboardList = document.getElementById('leaderboard-list');
const similarList = document.getElementById('similar-list');
const trustGraphContainer = document.getElementById('trust-graph-container');

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

const renderList = (container, items, renderItem, emptyMessage) => {
    if (!container) return;
    container.innerHTML = '';
    if (!items || items.length === 0) {
        if (emptyMessage) {
            const empty = document.createElement('p');
            empty.textContent = emptyMessage;
            empty.classList.add('auth__status');
            container.appendChild(empty);
        }
        return;
    }
    items.forEach((item) => {
        const element = document.createElement('div');
        element.classList.add('placeholder-item');
        element.innerHTML = renderItem(item);
        container.appendChild(element);
    });
};

const fetchWithAuth = async (url, options = {}) => {
    const { data } = await supabaseClient.auth.getSession();
    const token = data?.session?.access_token;
    const headers = new Headers(options.headers || {});
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    if (!headers.has('Content-Type') && options.method && options.method !== 'GET') {
        headers.set('Content-Type', 'application/json');
    }
    return fetch(url, { ...options, headers });
};

const loadProducts = async () => {
    try {
        const response = await fetchWithAuth('/api/products');
        if (!response.ok) {
            throw new Error('Unable to load products. Connect Supabase tables to populate this list.');
        }
        const products = await response.json();
        renderList(productsList, products, (product) => `
            <strong>${product.name}</strong>
            <p>${product.description ?? 'No description provided yet.'}</p>
            <p>Average rating: <strong>${product.averageRating ?? 'N/A'}</strong></p>
        `, 'No products yet. Add some using Supabase or the API.');
    } catch (error) {
        renderList(productsList, [], () => '', error.message);
    }
};

const loadMyReviews = async (userId) => {
    if (!userId) return;
    try {
        const response = await fetchWithAuth(`/api/reviews/user/${userId}`);
        if (!response.ok) {
            throw new Error('Unable to load your reviews yet.');
        }
        const reviews = await response.json();
        renderList(myReviewsList, reviews, (review) => `
            <div class="review-header">
                <strong>Product #${review.productId}</strong> · <span>${review.rating}★</span>
            </div>
            <p>${review.content ?? 'No review text added yet.'}</p>
            ${review.productUrl ? `<p><a href="${review.productUrl}" target="_blank" rel="noopener">Product link</a></p>` : ''}
        `, 'You have not written any reviews yet. Start by sharing your first experience.');
    } catch (error) {
        renderList(myReviewsList, [], () => '', error.message);
    }
};

const loadFollowing = async (userId) => {
    if (!userId) return;
    try {
        const response = await fetchWithAuth(`/api/follow/following/${userId}`);
        if (!response.ok) {
            throw new Error('Unable to load following users.');
        }
        const following = await response.json();
        renderList(followingList, following, (relation) => `
            <strong>${relation.followeeId}</strong>
            <p>Followed on: ${relation.createdAt ? new Date(relation.createdAt).toLocaleString() : 'Pending data'}</p>
        `, 'Not following anyone yet. Follow trusted reviewers to build your graph.');
    } catch (error) {
        renderList(followingList, [], () => '', error.message);
    }
};

const loadLeaderboard = async () => {
    const placeholder = [
        { rank: 1, name: 'Top Reviewer', followers: 128 },
        { rank: 2, name: 'Insightful Shopper', followers: 102 },
        { rank: 3, name: 'Value Hunter', followers: 94 }
    ];
    renderList(leaderboardList, placeholder, (item) => `
        <strong>#${item.rank} · ${item.name}</strong>
        <p>${item.followers} followers</p>
    `, 'Leaderboard coming soon once follow data is available.');
};

const loadSimilarUsers = async () => {
    const placeholder = [
        { name: 'Accessory Ace', similarity: 0.78 },
        { name: 'Eco Enthusiast', similarity: 0.74 },
        { name: 'Smart Home Sage', similarity: 0.71 }
    ];
    renderList(similarList, placeholder, (item) => `
        <strong>${item.name}</strong>
        <p>Similarity score: ${(item.similarity * 100).toFixed(0)}%</p>
    `, 'No similar users calculated yet. Add reviews to power Jaccard similarity.');
};

const loadTrustGraph = async () => {
    renderList(trustGraphContainer, [
        { title: 'Trust graph preview', content: 'Connect to the API to visualise first, second, and third degree connections.' }
    ], (item) => `
        <strong>${item.title}</strong>
        <p>${item.content}</p>
    `);
};

const handleProfileUpdate = async (event) => {
    event.preventDefault();
    if (!displayNameInput?.value) {
        setStatus(profileStatus, 'Display name cannot be empty.', 'error');
        return;
    }
    try {
        setStatus(profileStatus, 'Saving…');
        const { error } = await supabaseClient.auth.updateUser({
            data: { display_name: displayNameInput.value.trim() }
        });
        if (error) {
            throw error;
        }
        setStatus(profileStatus, 'Profile updated!', 'success');
        await refreshUserProfile();
    } catch (error) {
        setStatus(profileStatus, error.message ?? 'Unable to update profile.', 'error');
    }
};

const handlePasswordUpdate = async (event) => {
    event.preventDefault();
    if (!passwordInput?.value || passwordInput.value.length < 6) {
        setStatus(passwordStatus, 'Password must be at least 6 characters.', 'error');
        return;
    }
    try {
        setStatus(passwordStatus, 'Updating password…');
        const { error } = await supabaseClient.auth.updateUser({ password: passwordInput.value });
        if (error) {
            throw error;
        }
        setStatus(passwordStatus, 'Password updated successfully.', 'success');
        passwordInput.value = '';
    } catch (error) {
        setStatus(passwordStatus, error.message ?? 'Unable to update password.', 'error');
    }
};

const handleReviewSubmit = async (event, userId) => {
    event.preventDefault();
    const productId = parseInt(document.getElementById('review-product')?.value ?? '', 10);
    const rating = parseInt(document.getElementById('review-rating')?.value ?? '', 10);
    const content = document.getElementById('review-content')?.value ?? '';
    const productUrl = document.getElementById('review-url')?.value ?? null;

    if (!productId || !rating) {
        setStatus(reviewStatus, 'Product ID and rating are required.', 'error');
        return;
    }

    try {
        setStatus(reviewStatus, 'Submitting review…');
        const response = await fetchWithAuth('/api/reviews', {
            method: 'POST',
            body: JSON.stringify({
                productId,
                rating,
                content,
                productUrl,
                userId
            })
        });
        if (!response.ok) {
            throw new Error('Unable to save review. Ensure the API is connected to Supabase.');
        }
        setStatus(reviewStatus, 'Review submitted!', 'success');
        document.getElementById('review-form').reset();
        await loadMyReviews(userId);
    } catch (error) {
        setStatus(reviewStatus, error.message ?? 'Unable to submit review.', 'error');
    }
};

const handleFollowSubmit = async (event, userId) => {
    event.preventDefault();
    const followInput = document.getElementById('follow-user');
    if (!followInput?.value) {
        setStatus(followStatus, 'Enter a user ID to follow.', 'error');
        return;
    }

    try {
        setStatus(followStatus, 'Sending follow request…');
        const response = await fetchWithAuth('/api/follow', {
            method: 'POST',
            body: JSON.stringify({ followerId: userId, followeeId: followInput.value.trim() })
        });
        if (!response.ok) {
            throw new Error('Unable to follow user yet.');
        }
        setStatus(followStatus, 'Following user!', 'success');
        await loadFollowing(userId);
    } catch (error) {
        setStatus(followStatus, error.message ?? 'Unable to follow user.', 'error');
    }
};

const handleUnfollow = async (userId) => {
    const followInput = document.getElementById('follow-user');
    if (!followInput?.value) {
        setStatus(followStatus, 'Enter a user ID to unfollow.', 'error');
        return;
    }
    try {
        setStatus(followStatus, 'Updating follow list…');
        const response = await fetchWithAuth('/api/follow', {
            method: 'DELETE',
            body: JSON.stringify({ followerId: userId, followeeId: followInput.value.trim() })
        });
        if (!response.ok) {
            throw new Error('Unable to unfollow user yet.');
        }
        setStatus(followStatus, 'User unfollowed.', 'success');
        await loadFollowing(userId);
    } catch (error) {
        setStatus(followStatus, error.message ?? 'Unable to unfollow user.', 'error');
    }
};

let currentUser;

const refreshUserProfile = async () => {
    const { data } = await supabaseClient.auth.getUser();
    currentUser = data?.user;
    if (!currentUser) {
        return;
    }
    const displayName = currentUser.user_metadata?.display_name ?? currentUser.email;
    if (userGreeting) {
        userGreeting.textContent = `Hi, ${displayName}!`;
    }
    if (displayNameInput) {
        displayNameInput.value = currentUser.user_metadata?.display_name ?? '';
    }
};

const initialiseAppPage = async () => {
    await requireSession();
    await refreshUserProfile();

    const session = await supabaseClient.auth.getSession();
    const userId = session.data?.session?.user?.id;

    await Promise.all([
        loadProducts(),
        loadMyReviews(userId),
        loadFollowing(userId),
        loadLeaderboard(),
        loadSimilarUsers(),
        loadTrustGraph()
    ]);

    document.getElementById('profile-form')?.addEventListener('submit', handleProfileUpdate);
    document.getElementById('password-form')?.addEventListener('submit', handlePasswordUpdate);
    document.getElementById('review-form')?.addEventListener('submit', (event) => handleReviewSubmit(event, userId));
    document.getElementById('follow-form')?.addEventListener('submit', (event) => handleFollowSubmit(event, userId));
    document.getElementById('unfollow-button')?.addEventListener('click', () => handleUnfollow(userId));

    document.getElementById('refresh-dashboard')?.addEventListener('click', async () => {
        setStatus(reviewStatus, '');
        setStatus(followStatus, '');
        await Promise.all([
            loadProducts(),
            loadMyReviews(userId),
            loadFollowing(userId),
            loadLeaderboard(),
            loadSimilarUsers(),
            loadTrustGraph()
        ]);
    });

    document.querySelectorAll('[data-action="load-products"]').forEach((button) => {
        button.addEventListener('click', loadProducts);
    });
    document.querySelectorAll('[data-action="load-my-reviews"]').forEach((button) => {
        button.addEventListener('click', () => loadMyReviews(userId));
    });
    document.querySelectorAll('[data-action="load-leaderboard"]').forEach((button) => {
        button.addEventListener('click', loadLeaderboard);
    });
    document.querySelectorAll('[data-action="load-similar"]').forEach((button) => {
        button.addEventListener('click', loadSimilarUsers);
    });
    document.querySelectorAll('[data-action="load-trust"]').forEach((button) => {
        button.addEventListener('click', loadTrustGraph);
    });

    document.getElementById('logout-button')?.addEventListener('click', async () => {
        await supabaseClient.auth.signOut();
        window.location.replace('index.html');
    });
};

if (document.body?.dataset?.page === 'app') {
    initialiseAppPage().catch((error) => console.error('Failed to initialise dashboard', error));
}
