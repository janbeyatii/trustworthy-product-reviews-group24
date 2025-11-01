document.addEventListener('DOMContentLoaded', async () => {
    const container = document.getElementById('product-details');
    if (!container) return;

    const productId = new URLSearchParams(window.location.search).get('id');
    if (!productId) {
        container.innerHTML = '<p>Invalid product.</p>';
        return;
    }

    try {
        const res = await fetch(`http://localhost:8080/api/products/${productId}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const product = await res.json();
        console.log('Fetched product:', product);

        container.innerHTML = `
    <div class="product-detail">
        <img src="${product.image}" alt="${product.name}">
        <div class="product-info-box">
            <h2>${product.name}</h2>
            <p class="category"><strong>Category:</strong> ${product.category}</p>
            <p class="rating"><strong>Rating:</strong> ⭐ ${product.avg_rating ?? 'N/A'}</p>
            <p class="description">${product.description}</p>
            <a href="${product.link}" target="_blank">Buy/Visit Product</a>
    </div>
</div>

        `;
    } catch (err) {
        console.error(err);
        container.innerHTML = '<p>Failed to load product details</p>';
    }
});
