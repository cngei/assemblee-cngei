(() => {
    function initialize() {
        document.querySelectorAll('form[data-max-selected]').forEach(form => {
            const maxScelte = Number(form.dataset.maxSelected);
            const groups = [
                {delega: 'false', statusId: 'limite-proprio', label: 'In proprio'},
                {delega: 'true', statusId: 'limite-delega', label: 'Per delega'}
            ].map(group => ({
                ...group,
                inputs: Array.from(form.querySelectorAll(`input[data-delega='${group.delega}']`)),
                status: form.querySelector(`#${group.statusId}`)
            })).filter(group => group.inputs.length > 0);

            function update(group) {
                const selected = group.inputs.filter(input => input.checked).length;
                const invalid = selected > maxScelte;
                const limitMessage = `Puoi selezionare al massimo ${maxScelte} ${maxScelte === 1 ? 'opzione' : 'opzioni'}.`;
                group.inputs.forEach(input => {
                    input.disabled = input.type === 'checkbox' && !input.checked && selected >= maxScelte;
                    input.setCustomValidity(invalid ? limitMessage : '');
                });
                if (group.status) {
                    group.status.textContent = `${group.label}: ${selected} di ${maxScelte} ${maxScelte === 1 ? 'scelta selezionata' : 'scelte selezionate'}.`;
                    if (invalid) {
                        group.status.textContent += ` ${limitMessage} Deseleziona le opzioni in eccesso.`;
                    } else if (selected === maxScelte && maxScelte > 1) {
                        group.status.textContent += " Limite raggiunto. Deseleziona un'opzione per sceglierne un'altra.";
                    }
                }
                return !invalid;
            }

            groups.forEach(group => {
                group.inputs.forEach(input => input.addEventListener('change', () => update(group)));
                update(group);
            });
            window.addEventListener('pageshow', () => groups.forEach(update));
            form.addEventListener('submit', event => {
                const valid = groups.map(update).every(Boolean);
                if (!valid) {
                    event.preventDefault();
                    form.reportValidity();
                }
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initialize);
    } else {
        initialize();
    }
})();
