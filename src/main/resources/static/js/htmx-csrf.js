document.addEventListener('DOMContentLoaded', function () {
    // Get CSRF token from meta tags
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // Add CSRF token to all HTMX requests
    htmx.config.headers = htmx.config.headers || {};
    htmx.config.headers[csrfHeader] = csrfToken;
});
