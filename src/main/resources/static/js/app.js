/**
 * DOM Utilities
 */
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

/**
 * Toast Notification System
 */
function showToast(message, type = 'info') {
    let container = $('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <div class="toast-content">${message}</div>
    `;

    container.appendChild(toast);

    // Trigger reflow
    void toast.offsetWidth;
    toast.classList.add('show');

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

/**
 * Modal Management
 */
function openModal(id) {
    const modal = $(`#${id}`);
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(id) {
    const modal = $(`#${id}`);
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
}

// Close modal when clicking outside
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
        document.body.style.overflow = '';
    }
});

/**
 * API Helpers
 */
const api = {
    async request(url, options = {}) {
        try {
            const defaultHeaders = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };

            const config = {
                ...options,
                headers: {
                    ...defaultHeaders,
                    ...options.headers
                }
            };

            if (config.body && typeof config.body === 'object') {
                config.body = JSON.stringify(config.body);
            }

            const response = await fetch(url, config);
            
            // Check if response has content
            const text = await response.text();
            let data = null;
            if (text) {
                try {
                    data = JSON.parse(text);
                } catch (e) {
                    data = text;
                }
            }

            if (!response.ok) {
                throw new Error(data?.message || data || `Error ${response.status}`);
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            showToast(error.message, 'error');
            throw error;
        }
    }
};

const apiGet = (url) => api.request(url, { method: 'GET' });
const apiPost = (url, data) => api.request(url, { method: 'POST', body: data });
const apiPut = (url, data) => api.request(url, { method: 'PUT', body: data });
const apiDelete = (url) => api.request(url, { method: 'DELETE' });

/**
 * Theme Management
 */
function initTheme() {
    // Premium dark theme is now the default
    const savedTheme = localStorage.getItem('theme') || 'dark';
    document.body.setAttribute('data-theme', 'dark'); // Force dark for now to ensure consistency with premium UI
    
    const themeToggle = $('#theme-toggle');
    if (themeToggle) {
        themeToggle.checked = savedTheme === 'dark';
        themeToggle.addEventListener('change', (e) => {
            const newTheme = e.target.checked ? 'dark' : 'light';
            document.body.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        });
    }
}

/**
 * Date Formatter
 */
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString; // Invalid date
    const dd = String(date.getDate()).padStart(2, '0');
    const MM = String(date.getMonth() + 1).padStart(2, '0');
    const yyyy = date.getFullYear();
    return `${dd}/${MM}/${yyyy}`;
}

/**
 * Mobile Menu Toggle
 */
function initMobileMenu() {
    const toggle = $('.mobile-menu');
    const nav = $('.nav-links');
    if (toggle && nav) {
        toggle.addEventListener('click', () => {
            nav.classList.toggle('active');
        });
    }
}

/**
 * Tab Management
 */
function initTabs() {
    document.addEventListener('click', (e) => {
        if (e.target.classList.contains('tab-btn')) {
            const targetId = e.target.getAttribute('data-target');
            if (!targetId) return;

            const tabContainer = e.target.closest('.tabs').parentElement;
            
            // Remove active from all buttons in group
            const buttons = tabContainer.querySelectorAll('.tab-btn');
            buttons.forEach(btn => btn.classList.remove('active'));
            
            // Remove active from all content in group
            const contents = tabContainer.querySelectorAll('.tab-content');
            contents.forEach(content => content.classList.remove('active'));
            
            // Add active to current
            e.target.classList.add('active');
            const targetContent = tabContainer.querySelector(`#${targetId}`);
            if (targetContent) {
                targetContent.classList.add('active');
            }
        }
    });
}

/**
 * Initialize Application
 */
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initMobileMenu();
    initTabs();
    
    // Global close button handling
    document.querySelectorAll('.close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal-overlay');
            if (modal) {
                modal.classList.remove('active');
                document.body.style.overflow = '';
            }
            const panel = e.target.closest('.side-panel');
            if (panel) {
                panel.classList.remove('active');
            }
        });
    });
});
