(function () {
    function applyResponsiveTable(table) {
        const headers = Array.from(table.querySelectorAll("thead th"));
        const bodyRows = table.querySelectorAll("tbody tr");

        if (!headers.length || !bodyRows.length) {
            return;
        }

        table.classList.add("stacked-table");

        bodyRows.forEach((row) => {
            const cells = row.querySelectorAll("td");
            cells.forEach((cell, index) => {
                const headerText = headers[index]?.textContent?.trim();
                if (!headerText) {
                    return;
                }
                cell.setAttribute("data-label", headerText);
            });
        });
    }

    function getTablesFromRoot(root) {
        if (!root) {
            return [];
        }

        if (root.matches && root.matches("table")) {
            return [root];
        }

        return Array.from(root.querySelectorAll("table"));
    }

    function initResponsiveTables(root = document) {
        const tables = getTablesFromRoot(root);
        tables.forEach((table) => applyResponsiveTable(table));
    }

    function bindHtmxListeners() {
        const onHtmxUpdate = (event) => {
            initResponsiveTables(event?.target || document);
        };

        document.body.addEventListener("htmx:afterSwap", onHtmxUpdate);
        document.body.addEventListener("htmx:load", onHtmxUpdate);
    }

    function bootstrap() {
        initResponsiveTables(document);

        if (document.body) {
            bindHtmxListeners();
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bootstrap, { once: true });
    } else {
        bootstrap();
    }
})();

