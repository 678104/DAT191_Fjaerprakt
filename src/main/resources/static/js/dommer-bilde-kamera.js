function initDommerBildeKamera() {
    const forms = document.querySelectorAll('form[data-dommer-bilde-form]');
    if (!forms.length) {
        return;
    }

    forms.forEach((form) => {
        if (form.dataset.kameraInit === 'true') {
            return;
        }
        form.dataset.kameraInit = 'true';

        const fileInput = form.querySelector('[data-dommer-bilde-input]');
        const startButton = form.querySelector('[data-dommer-bilde-start]');
        const captureButton = form.querySelector('[data-dommer-bilde-capture]');
        const cancelButton = form.querySelector('[data-dommer-bilde-cancel]');
        const video = form.querySelector('[data-dommer-bilde-video]');
        const preview = form.querySelector('[data-dommer-bilde-preview]');
        const status = form.querySelector('[data-dommer-bilde-status]');

        if (!fileInput || !startButton || !captureButton || !cancelButton || !video || !preview || !status) {
            return;
        }

        let stream = null;

        const setStatus = (message, isError = false) => {
            status.textContent = message;
            status.style.color = isError ? 'var(--pico-del-color, #b00020)' : 'inherit';
        };

        const stopStream = () => {
            if (stream) {
                stream.getTracks().forEach((track) => track.stop());
                stream = null;
            }
            video.srcObject = null;
            video.hidden = true;
            captureButton.hidden = true;
            cancelButton.hidden = true;
            startButton.hidden = false;
        };

        const setCapturedFile = async (blob) => {
            const fileName = `kamera-${new Date().toISOString().replace(/[:.]/g, '-')}.jpg`;
            const file = new File([blob], fileName, {type: blob.type || 'image/jpeg'});
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            fileInput.files = dataTransfer.files;

            const previewUrl = URL.createObjectURL(blob);
            preview.src = previewUrl;
            preview.hidden = false;
            setStatus('Bilde er tatt og klart til lagring.');
        };

        startButton.addEventListener('click', async () => {
            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                setStatus('Nettleseren støtter ikke kamera.', true);
                return;
            }

            try {
                stream = await navigator.mediaDevices.getUserMedia({
                    video: {facingMode: {ideal: 'environment'}},
                    audio: false,
                });
                video.srcObject = stream;
                video.hidden = false;
                await video.play();
                startButton.hidden = true;
                captureButton.hidden = false;
                cancelButton.hidden = false;
                setStatus('Kamera aktivert. Ta et bilde når du er klar.');
            } catch (error) {
                console.error('Kunne ikke starte kamera', error);
                setStatus('Kunne ikke starte kamera. Sjekk kameratilgang.', true);
                stopStream();
            }
        });

        captureButton.addEventListener('click', async () => {
            if (!video.videoWidth || !video.videoHeight) {
                setStatus('Kameraet er ikke klart ennå.', true);
                return;
            }

            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const context = canvas.getContext('2d');
            if (!context) {
                setStatus('Kunne ikke lese kamera-bildet.', true);
                return;
            }

            context.drawImage(video, 0, 0, canvas.width, canvas.height);

            canvas.toBlob(async (blob) => {
                if (!blob) {
                    setStatus('Kunne ikke lage bilde fra kamera.', true);
                    return;
                }

                await setCapturedFile(blob);
                stopStream();
            }, 'image/jpeg', 0.92);
        });

        cancelButton.addEventListener('click', () => {
            stopStream();
            setStatus('Kamera avsluttet.');
        });

        form.addEventListener('submit', () => {
            stopStream();
        });

        if (fileInput.files && fileInput.files.length > 0) {
            preview.hidden = true;
        }
    });
}

document.addEventListener('DOMContentLoaded', initDommerBildeKamera);
document.addEventListener('htmx:afterSwap', initDommerBildeKamera);

