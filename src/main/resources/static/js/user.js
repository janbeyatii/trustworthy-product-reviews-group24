import { supabaseClient, requireSession, logout } from './auth.js';

let targetUserId = null;
let currentUserId = null;

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('year').textContent = new Date().getFullYear();
    
    // Check auth
    const session = await requireSession();
    currentUserId = session.user.id;
    
    // Get target user ID from URL
    const urlParams = new URLSearchParams(window.location.search);
    targetUserId = urlParams.get('id');
    
    if (!targetUserId) {
        window.location.href = '/app.html';
        return;
    }

    // Setup logout
    document.getElementById('logout-btn')?.addEventListener('click', (e) => {
        e.preventDefault();
        logout();
    });

    // Setup Tabs
    setupTabs();

    // Load Profile
    await loadUserProfile();
});

function setupTabs() {
    const tabs = document.querySelectorAll('.profile-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // Update tabs
            document.querySelectorAll('.profile-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            // Update content
            const tabName = tab.dataset.tab;
            document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
            document.getElementById(`tab-content-${tabName}`).classList.remove('hidden');
            
            // Load content if needed
            if (tabName === 'following') loadFollowing();
            if (tabName === 'followers') loadFollowers();
        });
    });
    
    // Clickable stats
    document.getElementById('show-followers').addEventListener('click', () => {
        document.querySelector('.profile-tab[data-tab="followers"]').click();
    });
    document.getElementById('show-following').addEventListener('click', () => {
        document.querySelector('.profile-tab[data-tab="following"]').click();
    });
}

async function loadUserProfile() {
    const loading = document.getElementById('loading-state');
    const content = document.getElementById('profile-content');
    const error = document.getElementById('error-state');
    
    try {
        // Fetch extended profile with metrics
        const { data: { session } } = await supabaseClient.auth.getSession();
        const response = await fetch(`/api/users/${targetUserId}`, {
            headers: {
                'Authorization': `Bearer ${session.access_token}`
            }
        });

        if (!response.ok) throw new Error('Failed to load user');
        const user = await response.json();

        // Update UI
        document.getElementById('user-name').textContent = user.display_name || 'User';
        document.getElementById('user-email').textContent = user.email;
        
        // Render Metrics
        const metricsContainer = document.getElementById('user-metrics');
        metricsContainer.innerHTML = '';
        
        // Degree of Separation
        if (user.degree_of_separation !== undefined && user.degree_of_separation !== null) {
            const degree = user.degree_of_separation;
            const degreeText = degree === 1 ? 'Direct Connection' : `${degree} Degrees Away`;
            metricsContainer.innerHTML += `<div class="metric-badge">🔗 ${degreeText}</div>`;
        } else if (targetUserId !== currentUserId) {
            metricsContainer.innerHTML += `<div class="metric-badge">Not Connected</div>`;
        }

        // Similarity Score
        if (user.similarity !== undefined) {
            const score = (user.similarity * 100).toFixed(1);
            const isHigh = user.similarity >= 0.5;
            metricsContainer.innerHTML += `
                <div class="metric-badge ${isHigh ? 'high-match' : ''}">
                    ${isHigh ? '🔥' : '📊'} ${score}% Match
                </div>
            `;
        }

        // Setup Follow Button
        setupFollowButton(targetUserId);

        // Load Stats
        loadUserStats();
        
        // Load Reviews (Default tab)
        loadUserReviews();

        loading.classList.add('hidden');
        content.classList.remove('hidden');

    } catch (err) {
        console.error(err);
        loading.classList.add('hidden');
        error.classList.remove('hidden');
    }
}

async function loadUserStats() {
    try {
        // Following count
        const { count: following } = await supabaseClient
            .from('relations')
            .select('*', { count: 'exact', head: true })
            .eq('uid', targetUserId);
            
        // Followers count
        const { count: followers } = await supabaseClient
            .from('relations')
            .select('*', { count: 'exact', head: true })
            .eq('following', targetUserId);

        // Reviews count (Needs an API endpoint or count query)
        // For now, we'll count when we fetch reviews or leave as is
        document.getElementById('following-count').textContent = following || 0;
        document.getElementById('followers-count').textContent = followers || 0;

    } catch (err) {
        console.error('Error loading stats:', err);
    }
}

async function setupFollowButton(targetId) {
    const btn = document.getElementById('follow-btn');
    if (targetId === currentUserId) {
        btn.style.display = 'none';
        return;
    }

        const { data } = await supabaseClient
            .from('relations')
            .select('*')
            .eq('uid', currentUserId)
            .eq('following', targetId)
            .maybeSingle();

    const updateBtn = (isFollowing) => {
        btn.textContent = isFollowing ? 'Unfollow' : 'Follow';
        btn.classList.toggle('button--outline', isFollowing); // Optional style
        btn.dataset.following = isFollowing;
    };

    updateBtn(!!data);

    btn.addEventListener('click', async () => {
        const isFollowing = btn.dataset.following === 'true';
        btn.disabled = true;
        
        try {
            if (isFollowing) {
                await supabaseClient
                    .from('relations')
                    .delete()
                    .eq('uid', currentUserId)
                    .eq('following', targetId);
            } else {
                await supabaseClient
                    .from('relations')
                    .insert({ uid: currentUserId, following: targetId });
            }
            updateBtn(!isFollowing);
            loadUserStats(); // Refresh stats
        } catch (err) {
            alert('Action failed. Please try again.');
        } finally {
            btn.disabled = false;
        }
    });
}

// Note: This requires a backend endpoint to fetch reviews by user. 
// Since I don't see one explicitly in ReviewController, I might need to add it or query DB directly via Supabase client if permitted (but RLS might block).
// Assuming we need to add an endpoint or use Supabase client if public read is allowed.
// Let's try Supabase client first as it's easier for frontend.
async function loadUserReviews() {
    const container = document.getElementById('reviews-list');
    container.innerHTML = '<p>Loading reviews...</p>';

    try {
        // Fetch reviews from Supabase directly
        const { data: reviews, error } = await supabaseClient
            .from('product_reviews')
            .select(`
                *,
                products (name)
            `)
            .eq('uid', targetUserId)
            .order('created_at', { ascending: false });

        if (error) throw error;

        container.innerHTML = '';
        document.getElementById('reviews-count').textContent = reviews.length;

        if (reviews.length === 0) {
            container.innerHTML = '<p>No reviews yet.</p>';
            return;
        }

        reviews.forEach(review => {
            const el = document.createElement('div');
            el.className = 'review-card';
            el.innerHTML = `
                <div class="review-header">
                    <div class="review-product-name">${escapeHtml(review.products?.name || 'Unknown Product')}</div>
                    <div class="review-date">${new Date(review.created_at).toLocaleDateString()}</div>
                </div>
                <div class="rating">⭐ ${review.review_rating}/5</div>
                <p>${escapeHtml(review.review_desc || '')}</p>
            `;
            container.appendChild(el);
        });

    } catch (err) {
        console.error('Error loading reviews:', err);
        container.innerHTML = '<p class="error">Failed to load reviews.</p>';
    }
}

async function loadFollowing() {
    const container = document.getElementById('following-list');
    if (container.dataset.loaded) return; // Avoid reload
    
    container.innerHTML = '<p>Loading...</p>';
    
    try {
        const { data } = await supabaseClient
            .from('relations')
            .select('following')
            .eq('uid', targetUserId);
            
        if (!data || data.length === 0) {
            container.innerHTML = '<p>Not following anyone.</p>';
            return;
        }

        const userIds = data.map(r => r.following);
        renderUserList(container, userIds);
        container.dataset.loaded = true;
    } catch (err) {
        container.innerHTML = '<p>Error loading list.</p>';
    }
}

async function loadFollowers() {
    const container = document.getElementById('followers-list');
    if (container.dataset.loaded) return;
    
    container.innerHTML = '<p>Loading...</p>';
    
    try {
        const { data } = await supabaseClient
            .from('relations')
            .select('uid')
            .eq('following', targetUserId);
            
        if (!data || data.length === 0) {
            container.innerHTML = '<p>No followers yet.</p>';
            return;
        }

        const userIds = data.map(r => r.uid);
        renderUserList(container, userIds);
        container.dataset.loaded = true;
    } catch (err) {
        container.innerHTML = '<p>Error loading list.</p>';
    }
}

async function renderUserList(container, userIds) {
    container.innerHTML = '';
    
    for (const id of userIds) {
        // Fetch basic info (could be optimized with bulk fetch)
        const { data: { session } } = await supabaseClient.auth.getSession();
        try {
            const res = await fetch(`/api/users/${id}`, {
                headers: { 'Authorization': `Bearer ${session.access_token}` }
            });
            const user = await res.json();
            
            const el = document.createElement('div');
            el.className = 'profile-item';
            el.innerHTML = `
                <div class="profile-item-info" style="cursor: pointer;">
                    <div class="profile-item-name">${escapeHtml(user.email)}</div>
                    ${user.display_name ? `<div class="profile-item-email">${escapeHtml(user.display_name)}</div>` : ''}
                </div>
                <a href="/user.html?id=${user.id}" class="button profile-action-btn">View</a>
            `;
            
            // Click entire card to view
            el.querySelector('.profile-item-info').addEventListener('click', () => {
                window.location.href = `/user.html?id=${user.id}`;
            });
            
            container.appendChild(el);
        } catch (err) {
            console.error('Failed to load user', id);
        }
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

