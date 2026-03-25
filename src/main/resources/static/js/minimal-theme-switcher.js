/*!
 * Minimal theme switcher
 *
 * Pico.css - https://picocss.com
 * Copyright 2019-2024 - Licensed under MIT
 */

const themeSwitcher = {
    icons: {
        dark: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="4.5"/><path d="M12 1.75a.75.75 0 0 1 .75.75V5a.75.75 0 0 1-1.5 0V2.5a.75.75 0 0 1 .75-.75ZM12 19a.75.75 0 0 1 .75.75v2.5a.75.75 0 0 1-1.5 0v-2.5A.75.75 0 0 1 12 19ZM1.75 12a.75.75 0 0 1 .75-.75H5a.75.75 0 0 1 0 1.5H2.5a.75.75 0 0 1-.75-.75ZM19 12a.75.75 0 0 1 .75-.75h2.5a.75.75 0 0 1 0 1.5h-2.5A.75.75 0 0 1 19 12ZM4.75 4.75a.75.75 0 0 1 1.06 0L7.58 6.5a.75.75 0 1 1-1.06 1.06L4.75 5.81a.75.75 0 0 1 0-1.06ZM16.42 16.42a.75.75 0 0 1 1.06 0l1.77 1.77a.75.75 0 1 1-1.06 1.06l-1.77-1.77a.75.75 0 0 1 0-1.06ZM19.25 4.75a.75.75 0 0 1 0 1.06L17.48 7.58a.75.75 0 1 1-1.06-1.06l1.77-1.77a.75.75 0 0 1 1.06 0ZM7.58 16.42a.75.75 0 0 1 0 1.06l-1.77 1.77a.75.75 0 0 1-1.06-1.06l1.77-1.77a.75.75 0 0 1 1.06 0Z"/></svg>',
        light: '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M21 12.79A9 9 0 1 1 11.21 3c.27 0 .53.11.72.3a1 1 0 0 1 .22.77A7 7 0 0 0 19.93 11a1 1 0 0 1 1.07 1.07Z"/></svg>',
    },
    // Config
    _scheme: "auto",
    menuTarget: "details.dropdown",
    buttonsTarget: "a[data-theme-switcher]",
    toggleTarget: "#theme-toggle",
    buttonAttribute: "data-theme-switcher",
    rootAttribute: "data-theme",
    localStorageKey: "picoPreferredColorScheme",

    // Init
    init() {
        this.scheme = this.schemeFromLocalStorage;
        this.initSwitchers();
        this.initToggle();
        this.syncToggleState();
    },

    // Get color scheme from local storage
    get schemeFromLocalStorage() {
        return window.localStorage?.getItem(this.localStorageKey) ?? this._scheme;
    },

    // Preferred color scheme
    get preferredColorScheme() {
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    },

    // Init switchers
    initSwitchers() {
        const buttons = document.querySelectorAll(this.buttonsTarget);
        buttons.forEach((button) => {
            button.addEventListener(
                "click",
                (event) => {
                    event.preventDefault();
                    // Set scheme
                    this.scheme = button.getAttribute(this.buttonAttribute);
                    // Close dropdown
                    document.querySelector(this.menuTarget)?.removeAttribute("open");
                },
                false
            );
        });
    },

    // Init icon toggle
    initToggle() {
        const toggle = document.querySelector(this.toggleTarget);
        if (!toggle) {
            return;
        }

        toggle.addEventListener(
            "click",
            () => {
                this.scheme = this.scheme === "dark" ? "light" : "dark";
            },
            false
        );
    },

    // Set scheme
    set scheme(scheme) {
        if (scheme === "auto") {
            this._scheme = this.preferredColorScheme;
        } else if (scheme === "dark" || scheme === "light") {
            this._scheme = scheme;
        }
        this.applyScheme();
        this.schemeToLocalStorage();
        this.syncToggleState();
    },

    // Get scheme
    get scheme() {
        return this._scheme;
    },

    // Apply scheme
    applyScheme() {
        document.querySelector("html")?.setAttribute(this.rootAttribute, this.scheme);
    },

    // Store scheme to local storage
    schemeToLocalStorage() {
        window.localStorage?.setItem(this.localStorageKey, this.scheme);
    },

    // Keep button state/icon in sync with active theme
    syncToggleState() {
        const toggle = document.querySelector(this.toggleTarget);
        if (!toggle) {
            return;
        }

        const isDark = this.scheme === "dark";
        const nextThemeLabel = isDark ? "Bytt til lyst tema" : "Bytt til morkt tema";

        toggle.setAttribute("aria-pressed", String(isDark));
        toggle.setAttribute("aria-label", nextThemeLabel);
        toggle.setAttribute("title", nextThemeLabel);

        const icon = toggle.querySelector("[data-theme-icon]");
        if (icon) {
            icon.innerHTML = isDark ? this.icons.dark : this.icons.light;
        }
    },
};

// Init
themeSwitcher.init();
