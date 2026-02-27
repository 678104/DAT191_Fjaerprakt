function validateForm(event) {
    const form = document.getElementById('duepaamelding');
    const hannerUng = Number(form.querySelector('input[name="hannerUng"]').value) || 0;
    const hannerEldre = Number(form.querySelector('input[name="hannerEldre"]').value) || 0;
    const hunnerUng = Number(form.querySelector('input[name="hunnerUng"]').value) || 0;
    const hunnerEldre = Number(form.querySelector('input[name="hunnerEldre"]').value) || 0;

    const errorMessage = document.getElementById('error-message');
    errorMessage.style.display = 'none';

    if (hannerUng === 0 && hannerEldre === 0 && hunnerUng === 0 && hunnerEldre === 0) {
        event.preventDefault();
        errorMessage.style.display = 'block';
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
