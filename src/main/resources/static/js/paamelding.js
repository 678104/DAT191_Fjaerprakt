function validateForm(event) {
    const form = document.getElementById('duepaamelding');
    if (!form) {
        return true;
    }

    const hannerUng = Number(form.querySelector('input[name="hannerUng"]').value) || 0;
    const hannerEldre = Number(form.querySelector('input[name="hannerEldre"]').value) || 0;
    const hunnerUng = Number(form.querySelector('input[name="hunnerUng"]').value) || 0;
    const hunnerEldre = Number(form.querySelector('input[name="hunnerEldre"]').value) || 0;

    const errorMessage = document.getElementById('error-message');
    if (errorMessage) {
        errorMessage.style.display = 'none';
    }

    if (hannerUng === 0 && hannerEldre === 0 && hunnerUng === 0 && hunnerEldre === 0) {
        event.preventDefault();
        if (errorMessage) {
            errorMessage.style.display = 'block';
        }
        return false;
    }

    return true;
}

function sjekkNegativ(input) {
    input.value = Math.max(0, input.value);
}

function valgtUtstilling(element) {
    document.querySelectorAll('.radio-card').forEach(card => card.classList.remove('selected'));

    element.classList.add('selected');

    const valgtUtstillingInput = document.getElementById('valgtUtstilling');
    valgtUtstillingInput.value = element.getAttribute('data-id');
}

function initPaameldingHtmxHandlers() {
    if (window.__paameldingHtmxHandlersInitialized) {
        return;
    }

    window.__paameldingHtmxHandlersInitialized = true;

    document.body.addEventListener('htmx:afterSwap', function (event) {
        const target = event.detail && event.detail.target;
        if (!target) {
            return;
        }

        if (target.classList && target.classList.contains('content')) {
            window.scrollTo({top: 0, behavior: 'smooth'});
        }

        if (target.id === 'duetabell') {
            const form = document.getElementById('duepaamelding');
            const radId = document.getElementById('radId');

            if (form && radId) {
                form.reset();
                const id = parseInt(radId.value || '0', 10);
                radId.value = Number.isNaN(id) ? '1' : String(id + 1);
            }
        }

        if (target.tagName && target.tagName.toUpperCase() === 'MAIN') {
            window.scrollTo({top: 0, behavior: 'smooth'});
        }
    });
}

initPaameldingHtmxHandlers();

