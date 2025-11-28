import { supabaseClient } from './auth.js';

let currentUser = null;
let currentProductId = null;

const setStatus = (element, message, type = 'info') => {
    if (!element) return;
    element.textContent = message;
    element.classList.remove('review-status--error', 'review-status--success');
    if (type === 'error') {
        element.classList.add('review-status--error');
    } else if (type === 'success') {
        element.classList.add('review-status--success');
    }
};

const renderStars = (rating) => {
    const roundedDown = Math.floor(rating);
    let stars = '';

    for (let i = 0; i < roundedDown; i++) {
        stars += '★';
    }

    for (let i = roundedDown; i < 5; i++) {
        stars += '☆';
    }

    return stars;
};


const fetchAndDisplayProduct = async (productId) => {
    const container = document.getElementById('product-details');
    if (!container) return;

    try {
        const baseUrl = window.__API_BASE_URL__ ?? window.location.origin;
        const res = await fetch(`${baseUrl}/api/products/${productId}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const product = await res.json();

        container.innerHTML = `
            <div class="product-detail">
                <img src="${product.image}" alt="${product.name}">
                <div class="product-info-box">
                    <h2>${product.name}</h2>
                    <p class="category"><strong>Category:</strong> ${product.category}</p>
                    <p class="rating"><strong>Rating:</strong> ${renderStars(product.avg_rating || 0)} ${product.avg_rating ? product.avg_rating.toFixed(1) : 'No ratings yet'}</p>
                    <p class="description">${product.description}</p>
                    <a href="${product.link}" target="_blank" class="button">Buy/Visit Product</a>
                </div>
            </div>
        `;
    } catch (err) {
        console.error(err);
        container.innerHTML = '<p>Failed to load product details</p>';
    }
};

const fetchAndDisplayReviewSummary = async (productId) => {
    const summaryContainer = document.getElementById('review-summary');
    if (!summaryContainer) return;

    try {
        const baseUrl = window.__API_BASE_URL__ ?? window.location.origin;
        const res = await fetch(`${baseUrl}/api/products/${productId}/summary`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const summary = await res.json();

        const totalReviews = summary.total_reviews || 0;
        const avgRating = summary.avg_rating || 0;
        const distribution = summary.distribution || {};

        if (totalReviews === 0) {
            summaryContainer.innerHTML = '<p class="no-reviews">No reviews yet. Be the first to review this product!</p>';
            return;
        }

        const getPercentage = (count) => totalReviews > 0 ? Math.round((count / totalReviews) * 100) : 0;

        summaryContainer.innerHTML = `
            <div class="review-summary-content">
                <div class="overall-rating">
                    <div class="rating-number">${avgRating.toFixed(1)}</div>
                    <div class="rating-stars">${renderStars(avgRating)}</div>
                    <div class="rating-count">${totalReviews} review${totalReviews !== 1 ? 's' : ''}</div>
                </div>
                <div class="rating-distribution">
                    ${[5, 4, 3, 2, 1].map(star => {
                        const count = distribution[star] || 0;
                        const percentage = getPercentage(count);
                        return `
                            <div class="rating-bar">
                                <span class="star-label">${star} ★</span>
                                <div class="bar-container">
                                    <div class="bar-fill" style="width: ${percentage}%"></div>
                                </div>
                                <span class="percentage-label">${percentage}%</span>
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        `;
    } catch (err) {
        console.error('Error fetching review summary:', err);
        summaryContainer.innerHTML = '<p class="error">Failed to load review summary.</p>';
    }
};

const fetchAndDisplayReviews = async (productId) => {
    const reviewsList = document.getElementById('reviews-list');
    if (!reviewsList) return;

    try {
        const { data: { session } } = await supabaseClient.auth.getSession();
        const headers = session ? { 'Authorization': `Bearer ${session.access_token}` } : {};

        const baseUrl = window.__API_BASE_URL__ ?? window.location.origin;
        const res = await fetch(`${baseUrl}/api/products/${productId}/reviews`, { headers });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const reviews = await res.json();

        if (reviews.length === 0) {
            reviewsList.innerHTML = '<p class="no-reviews">No reviews yet.</p>';
            return;
        }

        reviewsList.innerHTML = reviews.map(review => {
            const displayName = review.display_name || review.email || 'Anonymous';
            const reviewDate = review.created_at ? new Date(review.created_at).toLocaleDateString() : '';
            const reviewText = review.review_desc || '';
            
            let degreeBadge = '';
            if (review.degree_of_separation !== undefined && review.degree_of_separation !== null) {
                const degree = review.degree_of_separation;
                const badgeClass = degree === 1 ? 'degree-badge-direct' : 'degree-badge-indirect';
                const badgeText = degree === 1 ? 'Direct Connection' : `${degree} Degrees Away`;
                degreeBadge = `<span class="degree-badge ${badgeClass}" title="Degree of Separation">🔗 ${badgeText}</span>`;
            }
            
            return `
                <div class="review-item">
                    <div class="review-header">
                        <div class="reviewer-info">
                            <a href="/user.html?id=${review.uid}" class="reviewer-name" style="text-decoration: none;">${escapeHtml(displayName)}</a>
                            ${degreeBadge}
                            <span class="review-date">${reviewDate}</span>
                        </div>
                        <div class="review-rating">${renderStars(review.review_rating)}</div>
                    </div>
                    ${reviewText ? `<p class="review-text">${escapeHtml(reviewText)}</p>` : ''}
                </div>
            `;
        }).join('');
    } catch (err) {
        console.error('Error fetching reviews:', err);
        reviewsList.innerHTML = '<p class="error">Failed to load reviews.</p>';
    }
};

const handleReviewSubmit = async (event) => {
    event.preventDefault();
    const form = event.target;
    const statusEl = document.getElementById('review-status');
    const submitButton = form.querySelector('button[type="submit"]');

    const rating = form.querySelector('input[name="rating"]:checked')?.value;
    const reviewText = document.getElementById('review-text')?.value;

    if (!rating) {
        setStatus(statusEl, 'Please select a rating.', 'error');
        return;
    }

    try {
        submitButton.disabled = true;
        submitButton.textContent = 'Submitting...';

        const { data: { session } } = await supabaseClient.auth.getSession();
        if (!session) {
            setStatus(statusEl, 'You must be logged in to submit a review.', 'error');
            return;
        }

        const baseUrl = window.__API_BASE_URL__ ?? window.location.origin;
        const response = await fetch(`${baseUrl}/api/reviews`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${session.access_token}`
            },
            body: JSON.stringify({
                product_id: parseInt(currentProductId),
                rating: parseInt(rating),
                review_text: reviewText || null
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || 'Failed to submit review');
        }

        setStatus(statusEl, 'Review submitted successfully!', 'success');
        form.reset();

        await Promise.all([
            fetchAndDisplayProduct(currentProductId),
            fetchAndDisplayReviewSummary(currentProductId),
            fetchAndDisplayReviews(currentProductId)
        ]);

        document.getElementById('reviews-list')?.scrollIntoView({ behavior: 'smooth' });

    } catch (err) {
        console.error('Error submitting review:', err);
        setStatus(statusEl, err.message || 'Failed to submit review. Please try again.', 'error');
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = 'Submit Review';
    }
};

const checkAuthAndShowReviewForm = async () => {
    const { data: { session } } = await supabaseClient.auth.getSession();
    const writeReviewSection = document.getElementById('write-review-section');
    
    if (session && writeReviewSection) {
        writeReviewSection.style.display = 'block';
        currentUser = session.user;
    }
};

const escapeHtml = (text) => {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
};

document.addEventListener('DOMContentLoaded', async () => {
    const yearEl = document.getElementById('year');
    if (yearEl) {
        yearEl.textContent = new Date().getFullYear().toString();
    }

    const productId = new URLSearchParams(window.location.search).get('id');
    if (!productId) {
        document.getElementById('product-details').innerHTML = '<p>Invalid product ID.</p>';
        return;
    }

    currentProductId = productId;

    await Promise.all([
        fetchAndDisplayProduct(productId),
        fetchAndDisplayReviewSummary(productId),
        fetchAndDisplayReviews(productId),
        checkAuthAndShowReviewForm()
    ]);

    const reviewForm = document.getElementById('review-form');
    if (reviewForm) {
        reviewForm.addEventListener('submit', handleReviewSubmit);
    }
});
