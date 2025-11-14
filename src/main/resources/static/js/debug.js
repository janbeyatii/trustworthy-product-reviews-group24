const API_BASE = window.location.origin;

let statusInterval = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initializeControls();
    loadCircuitBreakerStatus();
    
    // Auto-refresh every 2 seconds
    statusInterval = setInterval(loadCircuitBreakerStatus, 2000);
});

function initializeControls() {
    document.getElementById('toggle-user-failure')?.addEventListener('click', toggleUserServiceFailure);
    document.getElementById('toggle-product-failure')?.addEventListener('click', toggleProductServiceFailure);
    document.getElementById('test-user-search')?.addEventListener('click', () => testUserSearch());
    document.getElementById('test-products')?.addEventListener('click', () => testProducts());
    document.getElementById('stress-test-user')?.addEventListener('click', () => stressTestUserService());
    document.getElementById('stress-test-product')?.addEventListener('click', () => stressTestProductService());
    document.getElementById('refresh-status')?.addEventListener('click', loadCircuitBreakerStatus);
}

async function loadCircuitBreakerStatus() {
    try {
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        if (!response.ok) {
            console.error('Failed to load circuit breaker status');
            return;
        }
        
        const data = await response.json();
        renderCircuitBreakerStatus(data);
        updateSimulationStatus(data.simulation);
    } catch (error) {
        console.error('Error loading circuit breaker status:', error);
    }
}

function renderCircuitBreakerStatus(data) {
    const container = document.getElementById('circuit-status');
    container.innerHTML = '';
    
    // Render UserService circuits
    if (data.userService) {
        Object.entries(data.userService).forEach(([commandName, metrics]) => {
            container.appendChild(createCircuitCard(commandName, metrics, 'UserService'));
        });
    }
    
    // Render ProductService circuits
    if (data.productService) {
        Object.entries(data.productService).forEach(([commandName, metrics]) => {
            container.appendChild(createCircuitCard(commandName, metrics, 'ProductService'));
        });
    }
}

function createCircuitCard(commandName, metrics, serviceName) {
    const card = document.createElement('div');
    const isOpen = metrics.circuitOpen === true;
    const status = isOpen ? 'open' : 'closed';
    const badgeText = isOpen ? 'OPEN' : 'CLOSED';
    
    card.className = `circuit-card ${status}`;
    
    const errorPercentage = metrics.errorPercentage || 0;
    const totalRequests = metrics.totalRequests || 0;
    const errorCount = metrics.errorCount || 0;
    
    card.innerHTML = `
        <div class="circuit-header">
            <span class="circuit-name">${serviceName}: ${commandName}</span>
            <span class="circuit-badge ${status}">${badgeText}</span>
        </div>
        <div class="circuit-metrics">
            <div class="metric">
                <span class="metric-label">Total Requests</span>
                <span class="metric-value">${totalRequests}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Errors</span>
                <span class="metric-value" style="color: ${errorCount > 0 ? 'var(--danger)' : 'inherit'}">${errorCount}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Error %</span>
                <span class="metric-value" style="color: ${errorPercentage > 50 ? 'var(--danger)' : 'inherit'}">${errorPercentage.toFixed(1)}%</span>
            </div>
            <div class="metric">
                <span class="metric-label">Concurrent</span>
                <span class="metric-value">${metrics.currentConcurrentExecutionCount || 0}</span>
            </div>
        </div>
    `;
    
    return card;
}

function updateSimulationStatus(simulation) {
    const userStatus = document.getElementById('user-failure-status');
    const productStatus = document.getElementById('product-failure-status');
    const userButton = document.getElementById('toggle-user-failure');
    const productButton = document.getElementById('toggle-product-failure');
    
    if (simulation) {
        if (userStatus) {
            userStatus.textContent = simulation.userServiceFailure ? 'Enabled' : 'Disabled';
            userStatus.style.color = simulation.userServiceFailure ? 'var(--danger)' : 'var(--muted)';
        }
        if (userButton) {
            userButton.textContent = simulation.userServiceFailure 
                ? 'Disable UserService Failures' 
                : 'Enable UserService Failures';
        }
        
        if (productStatus) {
            productStatus.textContent = simulation.productServiceFailure ? 'Enabled' : 'Disabled';
            productStatus.style.color = simulation.productServiceFailure ? 'var(--danger)' : 'var(--muted)';
        }
        if (productButton) {
            productButton.textContent = simulation.productServiceFailure 
                ? 'Disable ProductService Failures' 
                : 'Enable ProductService Failures';
        }
    }
}

async function toggleUserServiceFailure() {
    try {
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/simulate/user-service-failure`, {
            method: 'POST'
        });
        const data = await response.json();
        console.log('UserService failure simulation:', data.message);
        loadCircuitBreakerStatus();
    } catch (error) {
        console.error('Error toggling user service failure:', error);
    }
}

async function toggleProductServiceFailure() {
    try {
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/simulate/product-service-failure`, {
            method: 'POST'
        });
        const data = await response.json();
        console.log('ProductService failure simulation:', data.message);
        loadCircuitBreakerStatus();
    } catch (error) {
        console.error('Error toggling product service failure:', error);
    }
}

async function testUserSearch() {
    const button = document.getElementById('test-user-search');
    button.classList.add('loading');
    button.disabled = true;
    
    try {
        // Check if failure simulation is enabled
        const statusResponse = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        const statusData = await statusResponse.json();
        const forceFailure = statusData.simulation?.userServiceFailure || false;
        
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/test/user-search?query=test&forceFailure=${forceFailure}`, {
            method: 'POST'
        });
        const data = await response.json();
        addTestResult('User Search', data);
        loadCircuitBreakerStatus();
    } catch (error) {
        addTestResult('User Search', { success: false, error: error.message });
    } finally {
        button.classList.remove('loading');
        button.disabled = false;
    }
}

async function testProducts() {
    const button = document.getElementById('test-products');
    button.classList.add('loading');
    button.disabled = true;
    
    try {
        // Check if failure simulation is enabled
        const statusResponse = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        const statusData = await statusResponse.json();
        const forceFailure = statusData.simulation?.productServiceFailure || false;
        
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/test/products?forceFailure=${forceFailure}`, {
            method: 'POST'
        });
        const data = await response.json();
        addTestResult('Get Products', data);
        loadCircuitBreakerStatus();
    } catch (error) {
        addTestResult('Get Products', { success: false, error: error.message });
    } finally {
        button.classList.remove('loading');
        button.disabled = false;
    }
}

async function stressTestUserService() {
    const button = document.getElementById('stress-test-user');
    button.classList.add('loading');
    button.disabled = true;
    button.textContent = 'Running stress test...';
    
    try {
        // Check if failure simulation is enabled
        const statusResponse = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        const statusData = await statusResponse.json();
        const forceFailure = statusData.simulation?.userServiceFailure || false;
        
        const requestCount = 15;
        let successCount = 0;
        let failureCount = 0;
        
        addTestResult('Stress Test', { 
            success: true, 
            message: `Starting ${requestCount} requests to UserService...` 
        });
        
        // Make multiple requests in quick succession
        const promises = [];
        for (let i = 0; i < requestCount; i++) {
            promises.push(
                fetch(`${API_BASE}/api/debug/circuit-breaker/test/user-search?query=test&forceFailure=${forceFailure}`, {
                    method: 'POST'
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    return data;
                })
                .catch(error => {
                    failureCount++;
                    return { success: false, error: error.message };
                })
            );
        }
        
        // Wait for all requests to complete
        await Promise.all(promises);
        
        // Refresh status to see updated metrics
        await loadCircuitBreakerStatus();
        
        addTestResult('Stress Test Complete', { 
            success: true, 
            message: `Completed ${requestCount} requests: ${successCount} succeeded, ${failureCount} failed` 
        });
        
    } catch (error) {
        addTestResult('Stress Test', { success: false, error: error.message });
    } finally {
        button.classList.remove('loading');
        button.disabled = false;
        button.textContent = 'Stress Test UserService (15 requests)';
    }
}

async function stressTestProductService() {
    const button = document.getElementById('stress-test-product');
    button.classList.add('loading');
    button.disabled = true;
    button.textContent = 'Running stress test...';
    
    try {
        // Check if failure simulation is enabled
        const statusResponse = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        const statusData = await statusResponse.json();
        const forceFailure = statusData.simulation?.productServiceFailure || false;
        
        const requestCount = 15;
        let successCount = 0;
        let failureCount = 0;
        
        addTestResult('Stress Test', { 
            success: true, 
            message: `Starting ${requestCount} requests to ProductService...` 
        });
        
        // Make multiple requests in quick succession
        const promises = [];
        for (let i = 0; i < requestCount; i++) {
            promises.push(
                fetch(`${API_BASE}/api/debug/circuit-breaker/test/products?forceFailure=${forceFailure}`, {
                    method: 'POST'
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                    return data;
                })
                .catch(error => {
                    failureCount++;
                    return { success: false, error: error.message };
                })
            );
        }
        
        // Wait for all requests to complete
        await Promise.all(promises);
        
        // Refresh status to see updated metrics
        await loadCircuitBreakerStatus();
        
        addTestResult('Stress Test Complete', { 
            success: true, 
            message: `Completed ${requestCount} requests: ${successCount} succeeded, ${failureCount} failed` 
        });
        
    } catch (error) {
        addTestResult('Stress Test', { success: false, error: error.message });
    } finally {
        button.classList.remove('loading');
        button.disabled = false;
        button.textContent = 'Stress Test ProductService (15 requests)';
    }
}

function addTestResult(testName, result) {
    const resultsContainer = document.getElementById('test-results');
    const resultsContent = document.getElementById('test-results-content');
    
    resultsContainer.style.display = 'block';
    
    const resultDiv = document.createElement('div');
    resultDiv.className = `test-result ${result.success ? 'success' : 'failure'}`;
    
    const timestamp = new Date().toLocaleTimeString();
    const circuitStatus = result.circuitOpen ? ' (Circuit OPEN)' : ' (Circuit CLOSED)';
    
    resultDiv.innerHTML = `
        <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
            <strong>${testName}</strong>
            <span style="color: var(--muted); font-size: 0.9rem;">${timestamp}</span>
        </div>
        <div>
            ${result.message ? `<div style="margin-bottom: 0.5rem;"><strong>${result.message}</strong></div>` : ''}
            <div>Status: <strong>${result.success ? 'SUCCESS' : 'FAILURE'}${circuitStatus}</strong></div>
            ${result.duration ? `<div>Duration: ${result.duration}</div>` : ''}
            ${result.error ? `<div style="color: var(--danger); margin-top: 0.5rem;">Error: ${result.error}</div>` : ''}
            ${result.users ? `<div>Users returned: ${result.users.length}</div>` : ''}
            ${result.products ? `<div>Products returned: ${result.products.length}</div>` : ''}
        </div>
    `;
    
    resultsContent.insertBefore(resultDiv, resultsContent.firstChild);
    
    // Keep only last 10 results
    while (resultsContent.children.length > 10) {
        resultsContent.removeChild(resultsContent.lastChild);
    }
}

