import { supabaseClient } from './auth.js';

let currentUserId = null;

// Profile data cache
let followingUsers = [];
let followersUsers = [];

// Initialize profile tab functionality
export async function initializeProfileSection() {
    const { data: { user } } = await supabaseClient.auth.getUser();
    if (!user) return;
    
    currentUserId = user.id;

    // Tab switching
    const followingTab = document.getElementById('following-tab');
    const followersTab = document.getElementById('followers-tab');
    const searchUsersTab = document.getElementById('search-users-tab');
    
    const followingList = document.getElementById('following-list');
    const followersList = document.getElementById('followers-list');
    const searchUsersSection = document.getElementById('search-users-section');

    followingTab?.addEventListener('click', () => switchTab('following', followingTab));
    followersTab?.addEventListener('click', () => switchTab('followers', followersTab));
    searchUsersTab?.addEventListener('click', () => switchTab('search', searchUsersTab));

    // Load initial data
    await loadFollowing();
    
    // Search functionality with debounce
    const searchInput = document.getElementById('user-search-input');
    if (searchInput) {
        let searchTimeout;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            const query = e.target.value.trim();
            if (query.length >= 2) {
                searchTimeout = setTimeout(() => searchUsersByUsername(query), 300);
            } else {
                clearSearchResults();
            }
        });
    }

    // User profile modal
    const userProfileModal = document.getElementById('user-profile-modal');
    const closeUserProfileModal = document.getElementById('close-user-profile-modal');
    
    closeUserProfileModal?.addEventListener('click', () => closeModal(userProfileModal));
    userProfileModal?.addEventListener('click', (e) => {
        if (e.target === userProfileModal) closeModal(userProfileModal);
    });

    const toggleFollowButton = document.getElementById('toggle-follow-button');
    toggleFollowButton?.addEventListener('click', async () => {
        const targetUserId = toggleFollowButton.dataset.targetUserId;
        if (targetUserId) {
            await toggleFollow(targetUserId);
        }
    });
}

function switchTab(tabName, activeTab) {
    // Update tab styles
    document.querySelectorAll('.profile-tab').forEach(tab => tab.classList.remove('active'));
    activeTab.classList.add('active');

    // Show/hide sections
    const followingList = document.getElementById('following-list');
    const followersList = document.getElementById('followers-list');
    const searchUsersSection = document.getElementById('search-users-section');

    followingList?.classList.add('hidden');
    followersList?.classList.add('hidden');
    searchUsersSection?.classList.add('hidden');

    if (tabName === 'following') {
        followingList?.classList.remove('hidden');
        searchUsersSection?.classList.add('hidden');
        loadFollowing();
    } else if (tabName === 'followers') {
        followersList?.classList.remove('hidden');
        searchUsersSection?.classList.add('hidden');
        loadFollowers();
    } else if (tabName === 'search') {
        searchUsersSection?.classList.remove('hidden');
    }
}

async function loadFollowing() {
    const followingList = document.getElementById('following-list');
    if (!followingList || !currentUserId) return;

    try {
        // Query relations where current user is following others (UID matches current user)
        const { data, error } = await supabaseClient
            .from('relations')
            .select('following')
            .eq('uid', currentUserId);

        if (error) throw error;

        followingUsers = data || [];
        
        followingList.innerHTML = '';
        
        if (followingUsers.length === 0) {
            followingList.innerHTML = '<div class="empty-state">You are not following anyone yet.<br><br>Use the Search tab to find and follow other users.</div>';
            return;
        }

        // Display users with their email addresses
        const profiles = await Promise.all(followingUsers.map(async (relation) => {
            const userId = relation.following;
            const userDetails = await fetchUserDetails(userId);
            return { userId, userDetails };
        }));

        for (const { userId, userDetails } of profiles) {
            const userItem = createUserItem(userDetails, userId);
            followingList.appendChild(userItem);
        }
    } catch (error) {
        console.error('Error loading following:', error);
        followingList.innerHTML = '<div class="empty-state">Error loading following list.</div>';
    }
}

async function loadFollowers() {
    const followersList = document.getElementById('followers-list');
    if (!followersList || !currentUserId) return;

    try {
        // Query relations where others are following current user (following matches current user)
        const { data, error } = await supabaseClient
            .from('relations')
            .select('uid')
            .eq('following', currentUserId);

        if (error) throw error;

        followersUsers = data || [];
        
        followersList.innerHTML = '';
        
        if (followersUsers.length === 0) {
            followersList.innerHTML = '<div class="empty-state">No one is following you yet.</div>';
            return;
        }

        // Display users with their email addresses
        const profiles = await Promise.all(followersUsers.map(async (relation) => {
            const userId = relation.uid;
            const userDetails = await fetchUserDetails(userId);
            return { userId, userDetails };
        }));

        for (const { userId, userDetails } of profiles) {
            const userItem = createUserItem(userDetails, userId);
            followersList.appendChild(userItem);
        }
    } catch (error) {
        console.error('Error loading followers:', error);
        followersList.innerHTML = '<div class="empty-state">Error loading followers list.</div>';
    }
}

function createUserItem(user, userId) {
    const item = document.createElement('div');
    item.className = 'profile-item';
    
    const email = user?.email || (userId ? `${userId.substring(0, 8)}…` : 'Email unavailable');
    const displayName = user?.display_name;

    item.innerHTML = `
        <div class="profile-item-info" data-user-id="${userId}">
            <div class="profile-item-name">${escapeHtml(email)}</div>
            ${displayName ? `<div class="profile-item-email">${escapeHtml(displayName)}</div>` : ''}
        </div>
        <button class="button profile-action-btn">View</button>
    `;

    const infoDiv = item.querySelector('.profile-item-info');
    const viewButton = item.querySelector('button');

    const viewProfile = async () => {
        await showUserProfile(userId, email);
    };

    infoDiv.addEventListener('click', viewProfile);
    viewButton.addEventListener('click', viewProfile);

    return item;
}

async function showUserProfile(userId, email) {
    const userProfileModal = document.getElementById('user-profile-modal');
    const profileModalName = document.getElementById('profile-modal-name');
    const profileFollowingCount = document.getElementById('profile-following-count');
    const profileFollowersCount = document.getElementById('profile-followers-count');
    const toggleFollowButton = document.getElementById('toggle-follow-button');

    if (!userProfileModal) return;

    profileModalName.textContent = email || 'User';

    try {
        // Load following count
        const { count: followingCount } = await supabaseClient
            .from('relations')
            .select('*', { count: 'exact', head: true })
            .eq('uid', userId);

        // Load followers count
        const { count: followersCount } = await supabaseClient
            .from('relations')
            .select('*', { count: 'exact', head: true })
            .eq('following', userId);

        profileFollowingCount.textContent = followingCount || 0;
        profileFollowersCount.textContent = followersCount || 0;

        // Check if current user is following this user
        const { data: isFollowing } = await supabaseClient
            .from('relations')
            .select('*')
            .eq('uid', currentUserId)
            .eq('following', userId)
            .single();

        toggleFollowButton.dataset.targetUserId = userId;
        toggleFollowButton.textContent = isFollowing ? 'Unfollow' : 'Follow';
        
        if (userId === currentUserId) {
            toggleFollowButton.style.display = 'none';
        } else {
            toggleFollowButton.style.display = 'block';
        }
    } catch (error) {
        console.error('Error loading user profile:', error);
        profileFollowingCount.textContent = '0';
        profileFollowersCount.textContent = '0';
    }

    openModal(userProfileModal);
}

async function toggleFollow(targetUserId) {
    if (!currentUserId || !targetUserId) return;

    try {
        // Check if following
        const { data: existingRelation, error: checkError } = await supabaseClient
            .from('relations')
            .select('*')
            .eq('uid', currentUserId)
            .eq('following', targetUserId);

        if (checkError) {
            console.error('Error checking relation:', checkError);
        }

        if (existingRelation && existingRelation.length > 0) {
            // Unfollow - delete the row
            const { error: deleteError } = await supabaseClient
                .from('relations')
                .delete()
                .eq('uid', currentUserId)
                .eq('following', targetUserId);

            if (deleteError) {
                console.error('Delete error:', deleteError);
                throw deleteError;
            }
        } else {
            // Follow - insert new row with uid and following
            const { error: insertError } = await supabaseClient
                .from('relations')
                .insert({ 
                    uid: currentUserId, 
                    following: targetUserId
                });

            if (insertError) {
                console.error('Insert error:', insertError);
                throw insertError;
            }
        }

        // Refresh the profile modal
        const targetUserEmail = document.getElementById('profile-modal-name').textContent;
        await showUserProfile(targetUserId, targetUserEmail);
        
        // Refresh the following/followers lists
        await loadFollowing();
        await loadFollowers();
    } catch (error) {
        console.error('Error toggling follow:', error);
        alert('Error updating follow status: ' + (error.message || 'Please try again.'));
    }
}

async function searchUsersByUsername(query) {
    const searchResults = document.getElementById('search-results');
    if (!searchResults) return;

    try {
        // Get the current session token
        const { data: { session } } = await supabaseClient.auth.getSession();
        if (!session) {
            throw new Error('Not authenticated');
        }

        // Call backend API to search users
        const response = await fetch(`/api/users/search?query=${encodeURIComponent(query)}`, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${session.access_token}`
            }
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Search failed with status ${response.status}`);
        }

        const users = await response.json();
        
        searchResults.innerHTML = '';
        
        if (!users || users.length === 0) {
            searchResults.innerHTML = '<div class="empty-state">No users found matching your search.</div>';
            return;
        }

        // Display each user
        for (const user of users) {
            // Skip current user
            if (user.id === currentUserId) {
                continue;
            }
            
            const userItem = createSearchUserItem(user.id, user.email);
            searchResults.appendChild(userItem);
        }

        // If we filtered out all users (only current user found)
        if (searchResults.children.length === 0) {
            searchResults.innerHTML = '<div class="empty-state">No other users found matching your search.</div>';
        }
    } catch (error) {
        console.error('Error searching users:', error);
        searchResults.innerHTML = `<div class="empty-state">Error: ${error.message || 'Failed to search users'}</div>`;
    }
}

function createSearchUserItem(userId, email) {
    const item = document.createElement('div');
    item.className = 'profile-item';
    
    item.innerHTML = `
        <div class="profile-item-info" data-user-id="${userId}">
            <div class="profile-item-name">${escapeHtml(email)}</div>
        </div>
        <button class="button profile-action-btn">View Profile</button>
    `;

    const infoDiv = item.querySelector('.profile-item-info');
    const viewButton = item.querySelector('button');

    const viewProfile = async () => {
        await showUserProfile(userId, email);
    };

    infoDiv.addEventListener('click', viewProfile);
    viewButton.addEventListener('click', viewProfile);

    return item;
}

function clearSearchResults() {
    const searchResults = document.getElementById('search-results');
    if (searchResults) {
        searchResults.innerHTML = '';
    }
}

function openModal(modal) {
    modal?.classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeModal(modal) {
    modal?.classList.remove('active');
    document.body.style.overflow = '';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}


async function fetchUserDetails(userId) {
    try {
        // Get the current session token
        const { data: { session } } = await supabaseClient.auth.getSession();
        if (!session) {
            console.error('Not authenticated');
            return null;
        }

        const response = await fetch(`/api/users/${userId}`, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${session.access_token}`
            }
        });
        
        if (response.ok) {
            return await response.json();
        }

        const errorBody = await response.json().catch(() => ({}));
        console.warn(`Failed to load user ${userId}:`, errorBody.message || response.statusText);
    } catch (error) {
        console.error('Error fetching user details:', error);
    }
    return null;
}
