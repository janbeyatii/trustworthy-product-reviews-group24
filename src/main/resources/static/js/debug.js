/**
 * Circuit Breaker Debug Dashboard - Frontend JavaScript
 * 
 * This module provides the UI functionality for monitoring and testing Hystrix circuit breakers.
 * It communicates with the CircuitBreakerDebugController REST API to:
 * - Display real-time circuit breaker status and metrics
 * - Enable/disable failure simulation
 * - Trigger test requests
 * - Run stress tests to observe circuit breaker behavior
 * 
 * @fileoverview Frontend for Circuit Breaker Debug Dashboard
 */

/** Base URL for API calls - uses current origin (localhost or deployed URL) */
const API_BASE = window.location.origin;

/** Interval ID for auto-refresh timer - can be cleared if needed */
let statusInterval = null;

/**
 * Initialize the dashboard when DOM is loaded.
 * Sets up event listeners and starts auto-refresh of circuit breaker status.
 */
document.addEventListener('DOMContentLoaded', () => {
    initializeControls();
    loadCircuitBreakerStatus();
    
    // Auto-refresh circuit breaker status every 2 seconds to show real-time updates
    statusInterval = setInterval(loadCircuitBreakerStatus, 2000);
});

/**
 * Initialize all UI control event listeners.
 * Attaches click handlers to all buttons on the dashboard.
 */
function initializeControls() {
    document.getElementById('toggle-user-failure')?.addEventListener('click', toggleUserServiceFailure);
    document.getElementById('toggle-product-failure')?.addEventListener('click', toggleProductServiceFailure);
    document.getElementById('test-user-search')?.addEventListener('click', () => testUserSearch());
    document.getElementById('test-products')?.addEventListener('click', () => testProducts());
    document.getElementById('stress-test-user')?.addEventListener('click', () => stressTestUserService());
    document.getElementById('stress-test-product')?.addEventListener('click', () => stressTestProductService());
    document.getElementById('refresh-status')?.addEventListener('click', loadCircuitBreakerStatus);
}

/**
 * Loads circuit breaker status from the backend API.
 * Fetches current metrics for all circuit breakers and updates the UI.
 * Called automatically every 2 seconds and manually when refresh button is clicked.
 */
async function loadCircuitBreakerStatus() {
    try {
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        if (!response.ok) {
            console.error('Failed to load circuit breaker status');
            return;
        }
        
        const data = await response.json();
        // Render the circuit breaker cards with metrics
        renderCircuitBreakerStatus(data);
        // Update simulation toggle button states
        updateSimulationStatus(data.simulation);
    } catch (error) {
        console.error('Error loading circuit breaker status:', error);
    }
}

/**
 * Renders circuit breaker status cards for all commands.
 * Creates visual cards showing the state and metrics for each circuit breaker.
 * 
 * @param {Object} data - Response from /api/debug/circuit-breaker/status
 * @param {Object} data.userService - Metrics for UserService commands
 * @param {Object} data.productService - Metrics for ProductService commands
 */
function renderCircuitBreakerStatus(data) {
    const container = document.getElementById('circuit-status');
    container.innerHTML = '';
    
    // Render UserService circuit breakers (SearchUsers, GetUserById, GetFollowing, GetFollowers)
    if (data.userService) {
        Object.entries(data.userService).forEach(([commandName, metrics]) => {
            container.appendChild(createCircuitCard(commandName, metrics, 'UserService'));
        });
    }
    
    // Render ProductService circuit breakers (GetAllProducts, GetProductById, SearchProducts)
    if (data.productService) {
        Object.entries(data.productService).forEach(([commandName, metrics]) => {
            container.appendChild(createCircuitCard(commandName, metrics, 'ProductService'));
        });
    }
}

/**
 * Creates a visual card displaying circuit breaker status and metrics.
 * 
 * The card shows:
 * - Circuit state (OPEN/CLOSED) with color coding
 * - Total requests made
 * - Error count and percentage
 * - Current concurrent executions
 * 
 * @param {string} commandName - Name of the Hystrix command (e.g., "SearchUsers")
 * @param {Object} metrics - Circuit breaker metrics object
 * @param {boolean} metrics.circuitOpen - Whether circuit is open
 * @param {number} metrics.totalRequests - Total requests made
 * @param {number} metrics.errorCount - Number of failed requests
 * @param {number} metrics.errorPercentage - Percentage of requests that failed
 * @param {number} metrics.currentConcurrentExecutionCount - Currently executing requests
 * @param {string} serviceName - Service name for display (e.g., "UserService")
 * @returns {HTMLElement} The created circuit card element
 */
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

/**
 * Updates the UI to reflect current failure simulation state.
 * Updates button text and status indicators based on simulation flags.
 * 
 * @param {Object} simulation - Simulation flags from API response
 * @param {boolean} simulation.userServiceFailure - Whether UserService failures are enabled
 * @param {boolean} simulation.productServiceFailure - Whether ProductService failures are enabled
 */
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

/**
 * Toggles UserService failure simulation on/off.
 * When enabled, test requests will include a failure marker that causes exceptions.
 * This allows testing circuit breaker behavior without actual database failures.
 */
async function toggleUserServiceFailure() {
    try {
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/simulate/user-service-failure`, {
            method: 'POST'
        });
        const data = await response.json();
        console.log('UserService failure simulation:', data.message);
        // Refresh status to update UI with new simulation state
        loadCircuitBreakerStatus();
    } catch (error) {
        console.error('Error toggling user service failure:', error);
    }
}

/**
 * Toggles ProductService failure simulation on/off.
 * When enabled, test requests will set a system property that causes exceptions.
 * This allows testing circuit breaker behavior without actual database failures.
 */
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

/**
 * Triggers a single test request to UserService.searchUsers().
 * Makes one request through the circuit breaker to observe its behavior.
 * If failure simulation is enabled, the request will fail to test circuit breaker response.
 */
async function testUserSearch() {
    const button = document.getElementById('test-user-search');
    button.classList.add('loading');
    button.disabled = true;
    
    try {
        // Check current simulation state to determine if we should force failure
        const statusResponse = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        const statusData = await statusResponse.json();
        const forceFailure = statusData.simulation?.userServiceFailure || false;
        
        // Make test request - will fail if simulation is enabled
        const response = await fetch(`${API_BASE}/api/debug/circuit-breaker/test/user-search?query=test&forceFailure=${forceFailure}`, {
            method: 'POST'
        });
        const data = await response.json();
        addTestResult('User Search', data);
        // Refresh status to see updated metrics
        loadCircuitBreakerStatus();
    } catch (error) {
        addTestResult('User Search', { success: false, error: error.message });
    } finally {
        button.classList.remove('loading');
        button.disabled = false;
    }
}

/**
 * Triggers a single test request to ProductService.getAllProducts().
 * Makes one request through the circuit breaker to observe its behavior.
 * If failure simulation is enabled, the request will fail to test circuit breaker response.
 */
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

/**
 * Runs a stress test on UserService by sending 15 requests in parallel.
 * 
 * Purpose:
 * - Tests circuit breaker behavior under load
 * - With failure simulation enabled, 15 failures will trigger circuit to open
 *   (circuit opens after 10+ requests with 50%+ error rate)
 * - Observes how circuit breaker handles multiple concurrent failures
 * 
 * Process:
 * 1. Checks if failure simulation is enabled
 * 2. Sends 15 parallel requests
 * 3. Tracks success/failure counts
 * 4. Refreshes status to show updated metrics and circuit state
 */
async function stressTestUserService() {
    const button = document.getElementById('stress-test-user');
    button.classList.add('loading');
    button.disabled = true;
    button.textContent = 'Running stress test...';
    
    try {
        // Check current simulation state
        const statusResponse = await fetch(`${API_BASE}/api/debug/circuit-breaker/status`);
        const statusData = await statusResponse.json();
        const forceFailure = statusData.simulation?.userServiceFailure || false;
        
        // Send 15 requests - enough to trigger circuit breaker threshold (10 requests)
        const requestCount = 15;
        let successCount = 0;
        let failureCount = 0;
        
        addTestResult('Stress Test', { 
            success: true, 
            message: `Starting ${requestCount} requests to UserService...` 
        });
        
        // Create array of promises for parallel execution
        // All requests fire simultaneously to test concurrent load
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
        
        // Wait for all requests to complete (parallel execution)
        await Promise.all(promises);
        
        // Refresh status to see updated metrics and circuit breaker state
        // If failures occurred, circuit may have opened
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

/**
 * Runs a stress test on ProductService by sending 15 requests in parallel.
 * 
 * Purpose:
 * - Tests circuit breaker behavior under load
 * - With failure simulation enabled, 15 failures will trigger circuit to open
 *   (circuit opens after 10+ requests with 50%+ error rate)
 * - Observes how circuit breaker handles multiple concurrent failures
 * 
 * Process:
 * 1. Checks if failure simulation is enabled
 * 2. Sends 15 parallel requests
 * 3. Tracks success/failure counts
 * 4. Refreshes status to show updated metrics and circuit state
 */
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

/**
 * Adds a test result to the results display area.
 * 
 * Creates a visual result card showing:
 * - Test name and timestamp
 * - Success/failure status
 * - Circuit breaker state at time of test
 * - Duration, error messages, and result data
 * 
 * Results are displayed in reverse chronological order (newest first).
 * Only the last 10 results are kept to prevent UI clutter.
 * 
 * @param {string} testName - Name of the test (e.g., "User Search", "Stress Test")
 * @param {Object} result - Test result object
 * @param {boolean} result.success - Whether the test succeeded
 * @param {boolean} result.circuitOpen - Circuit breaker state at test time
 * @param {string} [result.duration] - Request duration in milliseconds
 * @param {string} [result.error] - Error message if test failed
 * @param {string} [result.message] - Additional message (for stress tests)
 * @param {Array} [result.users] - Users returned (if applicable)
 * @param {Array} [result.products] - Products returned (if applicable)
 */
function addTestResult(testName, result) {
    const resultsContainer = document.getElementById('test-results');
    const resultsContent = document.getElementById('test-results-content');
    
    resultsContainer.style.display = 'block';
    
    // Create result card with appropriate styling (green for success, red for failure)
    const resultDiv = document.createElement('div');
    resultDiv.className = `test-result ${result.success ? 'success' : 'failure'}`;
    
    const timestamp = new Date().toLocaleTimeString();
    const circuitStatus = result.circuitOpen ? ' (Circuit OPEN)' : ' (Circuit CLOSED)';
    
    // Build HTML for result card
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
    
    // Insert at the beginning (newest first)
    resultsContent.insertBefore(resultDiv, resultsContent.firstChild);
    
    // Keep only last 10 results to prevent UI from becoming too cluttered
    while (resultsContent.children.length > 10) {
        resultsContent.removeChild(resultsContent.lastChild);
    }
}

