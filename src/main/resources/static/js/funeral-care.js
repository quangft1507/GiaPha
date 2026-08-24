/**
 * Funeral Care Management
 */
class FuneralCareManager {
    constructor() {
        this.panelId = 'funeral-care-panel';
        this.currentDeceasedId = null;
        
        // Listen to person select to potentially load care data
        document.addEventListener('person:select', (e) => {
            const person = e.detail;
            if (person.isDeceased) {
                $('#funeral-care-tab').style.display = 'block';
                this.loadCareData(person.id);
            } else {
                $('#funeral-care-tab').style.display = 'none';
                // Switch to info tab if care tab was active
                if ($('#funeral-care-tab').classList.contains('active')) {
                    $('#info-tab').click();
                }
            }
        });
        
        // Form submission
        const form = $('#funeral-care-form');
        if (form) {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.assignCare();
            });
        }
    }
    
    async loadCareData(personId) {
        this.currentDeceasedId = personId;
        try {
            const cares = await apiGet(`/api/persons/${personId}/funeral-cares`);
            this.renderCareList(cares);
            
            // Also populate the caretaker select dropdown with potential candidates
            // (In a real app, this might search across the tree, for now we assume a simple GET)
            await this.populateCaretakerDropdown(personId);
            
        } catch (error) {
            console.error("Error loading funeral care:", error);
        }
    }
    
    async populateCaretakerDropdown(personId) {
        // Fetch all persons in the same tree to assign as caretakers
        // For efficiency, backend should provide a lightweight list
        try {
            // Assume we can get treeId from the global tree instance
            const data = await apiGet(`/api/persons/list-alive`); // Example endpoint
            const select = $('#caretaker-select');
            if (select && data) {
                select.innerHTML = '<option value="">-- Chọn người phụ trách (Select caretaker) --</option>';
                data.forEach(p => {
                    if (p.id !== personId) { // Can't assign to self
                        const option = document.createElement('option');
                        option.value = p.id;
                        option.textContent = p.fullName;
                        select.appendChild(option);
                    }
                });
            }
        } catch (e) {
            console.error(e);
        }
    }
    
    async assignCare() {
        if (!this.currentDeceasedId) return;
        
        const careType = $('#care-type').value;
        const caretakerId = $('#caretaker-select').value;
        const notes = $('#care-notes').value;
        
        if (!careType || !caretakerId) {
            showToast("Vui lòng chọn loại chăm sóc và người phụ trách", "warning");
            return;
        }
        
        const request = {
            careType: careType,
            caretakerId: parseInt(caretakerId),
            notes: notes
        };
        
        try {
            await apiPost(`/api/persons/${this.currentDeceasedId}/funeral-cares`, request);
            showToast("Đã phân công thành công (Assigned successfully)", "success");
            $('#funeral-care-form').reset();
            
            // Reload list and refresh tree to show badges
            this.loadCareData(this.currentDeceasedId);
            document.dispatchEvent(new CustomEvent('tree:refresh'));
        } catch (error) {
            showToast("Lỗi khi phân công (Error assigning)", "error");
        }
    }
    
    async removeCare(careId) {
        if (!confirm("Xóa phân công này? (Remove this assignment?)")) return;
        
        try {
            await apiDelete(`/api/funeral-cares/${careId}`);
            showToast("Đã xóa thành công (Removed)", "success");
            this.loadCareData(this.currentDeceasedId);
            document.dispatchEvent(new CustomEvent('tree:refresh'));
        } catch (error) {
            showToast("Lỗi khi xóa (Error removing)", "error");
        }
    }
    
    renderCareList(cares) {
        const listContainer = $('#funeral-care-list');
        if (!listContainer) return;
        
        listContainer.innerHTML = '';
        
        if (!cares || cares.length === 0) {
            listContainer.innerHTML = '<div class="text-center text-muted p-4">Chưa có phân công (No assignments yet)</div>';
            return;
        }
        
        cares.forEach(care => {
            const item = document.createElement('div');
            item.className = 'funeral-care-item mb-2';
            
            let badgeClass = '';
            let label = '';
            switch(care.type) {
                case 'CUNG_DUONG': badgeClass = 'badge-cung-duong'; label = 'Cúng dường'; break;
                case 'HAU_SU': badgeClass = 'badge-hau-su'; label = 'Hậu sự'; break;
                case 'CHIU_TANG': badgeClass = 'badge-chiu-tang'; label = 'Chịu tang'; break;
            }
            
            item.innerHTML = `
                <div class="care-info">
                    <span class="care-badge ${badgeClass}">${label}</span>
                    <strong class="mt-1">${care.caretakerName || 'Người phụ trách'}</strong>
                    <div class="text-xs text-muted">${care.notes || ''}</div>
                </div>
                <button class="btn-icon btn-danger" onclick="funeralCareManager.removeCare(${care.id})" title="Xóa">🗑️</button>
            `;
            listContainer.appendChild(item);
        });
    }
}

// Expose globally
window.funeralCareManager = new FuneralCareManager();
