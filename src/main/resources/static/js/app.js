import { supabaseClient, requireSession } from './auth.js';
import { initializeProfileSection } from './profile.js';

const userGreeting = document.getElementById('user-greeting');
const profileName = document.getElementById('profile-name');
const profileMenuName = document.getElementById('profile-menu-name');
const profileMenuEmail = document.getElementById('profile-menu-email');
const displayNameInput = document.getElementById('profile-display-name');
const profileStatus = document.getElementById('profile-status');
const passwordInput = document.getElementById('profile-password');
const passwordStatus = document.getElementById('password-status');
const profileToggle = document.getElementById('profile-toggle');
const profileMenu = document.getElementById('profile-menu');
const profileDropdown = document.querySelector('.profile-dropdown');

// Modal elements
const displayNameModal = document.getElementById('display-name-modal');
const passwordModal = document.getElementById('password-modal');
const changeDisplayNameLink = document.getElementById('change-display-name-link');
const changePasswordLink = document.getElementById('change-password-link');
const closeDisplayNameModal = document.getElementById('close-display-name-modal');
const closePasswordModal = document.getElementById('close-password-modal');
const cancelDisplayName = document.getElementById('cancel-display-name');
const cancelPassword = document.getElementById('cancel-password');

//products elements
const productsContainer = document.getElementById('products-container');
const productSearchInput = document.getElementById('product-search-input');

// Holds fetched products for client-side filtering
let allProducts = [];

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
        // Close modal after successful update
        setTimeout(() => {
            closeModal(displayNameModal);
            setStatus(profileStatus, '');
        }, 1500);
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
        // Close modal after successful update
        setTimeout(() => {
            closeModal(passwordModal);
            setStatus(passwordStatus, '');
        }, 1500);
    } catch (error) {
        setStatus(passwordStatus, error.message ?? 'Unable to update password.', 'error');
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
    const email = currentUser.email;
    
    if (userGreeting) {
        userGreeting.textContent = `Hi, ${displayName}!`;
    }
    if (profileName) {
        profileName.textContent = displayName;
    }
    if (profileMenuName) {
        profileMenuName.textContent = displayName;
    }
    if (profileMenuEmail) {
        profileMenuEmail.textContent = email;
    }
    if (displayNameInput) {
        displayNameInput.value = currentUser.user_metadata?.display_name ?? '';
    }
};

const renderProducts = (products) => {
    if (!productsContainer) return;

    productsContainer.innerHTML = ''; // clear any existing content

    if (!products || products.length === 0) {
        productsContainer.innerHTML = '<p class="empty">No products found.</p>';
        return;
    }

    products.forEach(product => {
        const card = document.createElement('div');
        card.className = 'product-card';
        const ratingDisplay = product.avg_rating ? product.avg_rating.toFixed(1) : 'N/A';
        
        card.innerHTML = `
            <img src="${product.image}" alt="${product.name}">
            <h2>${product.name}</h2>
            <p class="rating">⭐ ${ratingDisplay}</p>
            <a href="product.html?id=${product.product_id}">View Product</a>
        `;

        productsContainer.appendChild(card);
    });
};

const fetchAndDisplayProducts = async () => {
    try {
        const baseUrl = window.__API_BASE_URL__ ?? window.location.origin;
        const response = await fetch(`${baseUrl}/api/products`);
        if (!response.ok) throw new Error('Failed to fetch products');
        const products = await response.json();

        // store for client-side search
        allProducts = Array.isArray(products) ? products : [];

        renderProducts(allProducts);
    } catch (err) {
        console.error('Error fetching products:', err);
        if (productsContainer) {
            productsContainer.innerHTML = '<p class="error">Failed to load products.</p>';
        }
    }
};

// simple debounce helper
const debounce = (fn, delay = 250) => {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
};

// wire up product search input to filter the product list by name
if (productSearchInput) {
    productSearchInput.addEventListener('input', debounce((e) => {
        const q = (e.target.value || '').trim().toLowerCase();
        if (!q) {
            renderProducts(allProducts);
            return;
        }
        const filtered = allProducts.filter(p => (p.name || '').toLowerCase().includes(q));
        renderProducts(filtered);
    }, 200));
}

const toggleProfileDropdown = () => {
    profileDropdown?.classList.toggle('active');
};

const closeProfileDropdown = () => {
    profileDropdown?.classList.remove('active');
};

// Modal functions
const openModal = (modal) => {
    modal?.classList.add('active');
    document.body.style.overflow = 'hidden';
};

const closeModal = (modal) => {
    modal?.classList.remove('active');
    document.body.style.overflow = '';
};

const openDisplayNameModal = () => {
    closeProfileDropdown();
    openModal(displayNameModal);
    displayNameInput?.focus();
};

const openPasswordModal = () => {
    closeProfileDropdown();
    openModal(passwordModal);
    passwordInput?.focus();
};

const initialiseAppPage = async () => {
    await requireSession();
    await refreshUserProfile();
    await initializeProfileSection();

    // Profile dropdown functionality
    profileToggle?.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleProfileDropdown();
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!profileDropdown?.contains(e.target)) {
            closeProfileDropdown();
        }
    });

    // Close dropdown on escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeProfileDropdown();
        }
    });

    // Modal functionality
    changeDisplayNameLink?.addEventListener('click', openDisplayNameModal);
    changePasswordLink?.addEventListener('click', openPasswordModal);

    // Close modal buttons
    closeDisplayNameModal?.addEventListener('click', () => closeModal(displayNameModal));
    closePasswordModal?.addEventListener('click', () => closeModal(passwordModal));
    cancelDisplayName?.addEventListener('click', () => closeModal(displayNameModal));
    cancelPassword?.addEventListener('click', () => closeModal(passwordModal));

    // Close modals when clicking outside
    displayNameModal?.addEventListener('click', (e) => {
        if (e.target === displayNameModal) {
            closeModal(displayNameModal);
        }
    });
    passwordModal?.addEventListener('click', (e) => {
        if (e.target === passwordModal) {
            closeModal(passwordModal);
        }
    });

    // Form submissions
    document.getElementById('profile-form')?.addEventListener('submit', handleProfileUpdate);
    document.getElementById('password-form')?.addEventListener('submit', handlePasswordUpdate);

    // Logout functionality
    document.getElementById('logout-button')?.addEventListener('click', async () => {
        await supabaseClient.auth.signOut();
        window.location.replace('index.html');
    });
        await fetchAndDisplayProducts();
};

if (document.body?.dataset?.page === 'app') {
    initialiseAppPage().catch((error) => console.error('Failed to initialise dashboard', error));
}
