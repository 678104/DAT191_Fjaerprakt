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

    function initResponsiveTables() {
        const tables = document.querySelectorAll("table");
        tables.forEach((table) => applyResponsiveTable(table));
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initResponsiveTables, { once: true });
    } else {
        initResponsiveTables();
    }
})();

