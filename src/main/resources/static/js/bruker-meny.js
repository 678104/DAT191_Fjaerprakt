function toggleUserMenu() {
    const dropdown = document.getElementById('bruker-nedtrekk');
    const btn = document.getElementById('bruker-meny-knp');
    const isOpen = dropdown.style.display === 'block';

    dropdown.style.display = isOpen ? 'none' : 'block';
    btn.setAttribute('aria-expanded', String(!isOpen));
}

document.addEventListener('click', function (e) {
    const menu = document.getElementById('bruker-nedtrekk');
    const btn = document.getElementById('bruker-meny-knp');
    if (menu && btn && !btn.contains(e.target) && !menu.contains(e.target)) {
        menu.style.display = 'none';
        btn.setAttribute('aria-expanded', 'false');
    }
});

document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        const menu = document.getElementById('bruker-nedtrekk');
        const btn = document.getElementById('bruker-meny-knp');
        if (menu) menu.style.display = 'none';
        if (btn) btn.setAttribute('aria-expanded', 'false');
    }
});