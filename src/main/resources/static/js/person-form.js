/**
 * Person Form Management
 */
class PersonFormManager {
    constructor(treeId) {
        this.treeId = treeId;
        this.modalId = 'personFormModal';
        this.formId = 'personForm';
        this.form = $(`#${this.formId}`);
        
        // Handle save
        const form = $('#personForm');
        if(form) {
            form.addEventListener('submit', (e) => this.submitForm(e));
        }
        
        const parentIdSelect = $('#personParentId');
        if (parentIdSelect) {
            parentIdSelect.addEventListener('change', () => this.onParentChange());
        }
        
        // Handle gender toggle
        this.initGenderSelector();
        
        const isDeceasedCheckbox = $('#isDeceased');
        const deathDateGroup = $('#deathDateGroup');
        const caretakerGroup = $('#caretakerGroup');
        if (isDeceasedCheckbox && deathDateGroup) {
            isDeceasedCheckbox.addEventListener('change', (e) => {
                deathDateGroup.style.display = e.target.checked ? 'block' : 'none';
                if(caretakerGroup) {
                    if(e.target.checked) {
                        caretakerGroup.style.border = '1px solid rgba(59, 130, 246, 0.6)';
                        caretakerGroup.style.background = 'rgba(59, 130, 246, 0.1)';
                    } else {
                        caretakerGroup.style.border = '1px solid rgba(59, 130, 246, 0.2)';
                        caretakerGroup.style.background = 'rgba(59, 130, 246, 0.05)';
                    }
                }
            });
        }
        
        // Setup initial caretakers
        this.allPersonsCache = [];
        this.fetchAllPersons();
    }
    
    openAddChildForm(parentId) {
        this.resetForm();
        this.populateParentDropdown(parentId);
        $('#personFormTitle').textContent = 'Thêm con (Add Child)';
        
        openModal(this.modalId);
    }
    
    populateParentDropdown(selectedId = null) {
        const parentGroup = $('#parentGroup');
        const parentIdSelect = $('#personParentId');
        if (!parentGroup || !parentIdSelect) return;
        
        parentGroup.style.display = 'block';
        parentIdSelect.innerHTML = '<option value="">-- Không chọn (Thủy Tổ hoặc không rõ) --</option>';
        
        this.allPersonsCache.forEach(p => {
            const option = document.createElement('option');
            option.value = p.id;
            const birthYear = p.birthDate ? new Date(p.birthDate).getFullYear() : '';
            option.textContent = `[ID: ${p.id}] ${p.fullName || p.name} ${birthYear ? '(' + birthYear + ')' : ''}`;
            parentIdSelect.appendChild(option);
        });
        
        if (selectedId) {
            parentIdSelect.value = selectedId;
        }
        this.onParentChange();
    }
    
    onParentChange() {
        const parentId = $('#personParentId').value;
        const otherParentGroup = $('#otherParentGroup');
        const otherParentId = $('#otherParentId');
        if (!otherParentGroup || !otherParentId) return;
        
        otherParentGroup.style.display = 'none';
        otherParentId.innerHTML = '<option value="">-- Thuộc người chính (Chưa rõ) --</option>';
        
        if (!parentId || !window.treeViz || !window.treeViz.root) return;
        
        let foundNode = null;
        window.treeViz.root.each(d => {
            if(d.data.id == parentId) foundNode = d.data;
        });
        
        if (foundNode && foundNode.spouses && foundNode.spouses.length > 0) {
            foundNode.spouses.forEach(sp => {
                const option = document.createElement('option');
                option.value = sp.id;
                option.textContent = `Là con của Mẹ: ${sp.name || sp.fullName}`;
                otherParentId.appendChild(option);
            });
            
            // If there is exactly one spouse, default to it and hide the dropdown
            if (foundNode.spouses.length === 1) {
                otherParentId.value = foundNode.spouses[0].id;
                otherParentGroup.style.display = 'none';
            } else {
                // If there are multiple spouses, force the user to choose
                otherParentGroup.style.display = 'block';
            }
        }
    }
    
    openAddSpouseForm(personId) {
        this.resetForm();
        $('#personSpouseId').value = personId;
        if ($('#parentGroup')) $('#parentGroup').style.display = 'none'; // Hide parent selector for spouse
        $('#personFormTitle').textContent = 'Thêm vợ/chồng (Add Spouse)';
        openModal(this.modalId);
    }

    openAddParentForm(childId) {
        this.resetForm();
        if ($('#personChildId')) $('#personChildId').value = childId;
        
        // Show the parent selector so user can select the Grandpa (if any)
        if ($('#parentGroup')) $('#parentGroup').style.display = 'block';
        if ($('#otherParentGroup')) $('#otherParentGroup').style.display = 'none';
        
        this.populateParentDropdown();
        
        // Try to auto-select current parent if it exists
        let currentParentId = null;
        if (window.treeViz && window.treeViz.root) {
            window.treeViz.root.each(d => {
                if (d.data.id == childId && d.parent && !d.data.spouse) {
                    currentParentId = d.parent.data.id;
                }
            });
        }
        if (currentParentId && $('#personParentId')) {
            $('#personParentId').value = currentParentId;
            // trigger change to load otherParent if any
            this.onParentChange();
        }
        
        let childName = '';
        if (this.allPersonsCache) {
            const child = this.allPersonsCache.find(p => p.id == childId);
            if (child) childName = ` cho ${child.fullName || child.name || ''}`;
        }
        $('#personFormTitle').textContent = `Thêm Thân sinh (Cha/Mẹ)${childName}`;
        openModal(this.modalId);
    }
    
    openAddRootForm(treeId) {
        this.resetForm();
        this.populateParentDropdown();
        // treeId might be passed or use this.treeId
        this.currentTreeId = treeId || this.treeId;
        $('#personFormTitle').textContent = 'Thêm thành viên (Add Member)';
        openModal(this.modalId);
    }
    
    async openEditForm(personId) {
        this.resetForm();
        $('#personFormTitle').textContent = 'Chỉnh sửa thông tin (Edit Person)';
        
        try {
            const data = await apiGet(`/api/persons/${personId}`);
            
            // Re-populate parent dropdown to allow changing parent
            // Find current parentId from treeViz or let backend provide it
            let currentParentId = null;
            if (window.treeViz && window.treeViz.root) {
                window.treeViz.root.each(d => {
                    if(d.data.id == personId && d.parent && !d.data.spouse) {
                        currentParentId = d.parent.data.id;
                    }
                });
            }
            this.populateParentDropdown(currentParentId);
            
            // Populate form
            $('#personId').value = data.id;
            $('#surname').value = data.ho || '';
            $('#middleName').value = data.tenDem || '';
            $('#firstName').value = data.ten || '';
            
            // Set Gender
            const genderVal = data.gender || 'NAM';
            $('#gender').value = genderVal;
            $$('.gender-btn').forEach(b => {
                if(b.getAttribute('data-val') === genderVal) b.classList.add('active');
                else b.classList.remove('active');
            });
            
            $('#birthOrder').value = data.birthOrder || 1;
            
            $('#birthDate').value = data.birthDate ? data.birthDate.split('T')[0] : '';
            $('#birthplace').value = data.birthPlace || '';
            $('#occupation').value = data.occupation || '';
            $('#phoneNumber').value = data.phoneNumber || '';
            $('#biography').value = data.biography || '';
            
            if (data.isDeceased) {
                $('#isDeceased').checked = true;
                $('#deathDateGroup').style.display = 'block';
                $('#deathDate').value = data.deathDate ? data.deathDate.split('T')[0] : '';
                const caretakerGroup = $('#caretakerGroup');
                if(caretakerGroup) {
                    caretakerGroup.style.border = '1px solid rgba(59, 130, 246, 0.6)';
                    caretakerGroup.style.background = 'rgba(59, 130, 246, 0.1)';
                }
            } else {
                $('#isDeceased').checked = false;
                $('#deathDateGroup').style.display = 'none';
                $('#deathDate').value = '';
                const caretakerGroup = $('#caretakerGroup');
                if(caretakerGroup) {
                    caretakerGroup.style.border = '1px solid rgba(59, 130, 246, 0.2)';
                    caretakerGroup.style.background = 'rgba(59, 130, 246, 0.05)';
                }
            }
            
            // Check if this person has a parent with spouses to show otherParentId dropdown
            if (data.parentId && window.treeViz && window.treeViz.root) {
                const otherParentGroup = $('#otherParentGroup');
                const otherParentId = $('#otherParentId');
                if (otherParentGroup && otherParentId) {
                    let parentNode = null;
                    window.treeViz.root.each(d => {
                        if (d.data.id == data.parentId) parentNode = d.data;
                    });
                    
                    if (parentNode && parentNode.spouses && parentNode.spouses.length > 0) {
                        otherParentGroup.style.display = 'block';
                        otherParentId.innerHTML = '<option value="">-- Thuộc người chính (Chưa rõ) --</option>';
                        parentNode.spouses.forEach(sp => {
                            const option = document.createElement('option');
                            option.value = sp.id;
                            option.textContent = `Là con của: ${sp.name || sp.fullName}`;
                            if (data.otherParentId == sp.id) option.selected = true;
                            otherParentId.appendChild(option);
                        });
                    }
                }
            }
            
            // Ensure we have latest persons
            await this.fetchAllPersons();
            this.populateCaretakerDropdown(personId, data.caretakerId);
            
            openModal(this.modalId);
        } catch (error) {
            showToast('Không thể tải thông tin (Failed to load details)', 'error');
        }
    }
    async fetchAllPersons() {
        try {
            const data = await apiGet(`/api/trees/${this.treeId}/persons`);
            this.allPersonsCache = data || [];
        } catch(e) {
            console.error("Failed to fetch persons for dropdown");
            this.allPersonsCache = [];
        }
    }
    
    getDescendants(personId, allPersons) {
        let descendants = [];
        const children = allPersons.filter(p => p.parentId == personId);
        for(let child of children) {
            descendants.push(child);
            descendants = descendants.concat(this.getDescendants(child.id, allPersons));
        }
        return descendants;
    }
    
    populateCaretakerDropdown(personId, selectedId) {
        const select = $('#caretakerId');
        if(!select) return;
        select.innerHTML = '<option value="">-- Không có (Chưa có con cháu) --</option>';
        
        if (!personId) {
            // New person, no descendants
            return;
        }

        const descendants = this.getDescendants(personId, this.allPersonsCache);
        if(descendants.length > 0) {
            select.innerHTML = '<option value="">-- Chọn Người cúng dường --</option>';
            descendants.forEach(p => {
                const option = document.createElement('option');
                option.value = p.id;
                option.textContent = p.fullName || `${p.ho || ''} ${p.tenDem || ''} ${p.ten || ''}`.trim();
                if(selectedId && p.id == selectedId) option.selected = true;
                select.appendChild(option);
            });
        }
    }
    
    validateForm() {
        let isValid = true;
        const firstName = $('#firstName').value.trim();
        
        if (!firstName) {
            showToast('Vui lòng nhập tên (Please enter first name)', 'warning');
            isValid = false;
        }
        
        const birthDate = $('#birthDate').value;
        const isDeceased = $('#isDeceased').checked;
        const deathDate = $('#deathDate').value;
        
        if (isDeceased && birthDate && deathDate) {
            if (new Date(birthDate) > new Date(deathDate)) {
                showToast('Ngày mất không thể trước ngày sinh (Death date cannot be before birth date)', 'warning');
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    async submitForm(e) {
        e.preventDefault();
        
        if (!this.validateForm()) return;
        
        const formData = new FormData(this.form);
        const data = {
            ho: formData.get('surname'),
            tenDem: formData.get('middleName'),
            ten: formData.get('firstName'),
            aliasName: formData.get('alias'),
            gender: formData.get('gender'),
            birthDate: formData.get('birthDate') || null,
            birthOrder: parseInt(formData.get('birthOrder')) || 1,
            isDeceased: formData.get('isDeceased') === 'on',
            deathDate: formData.get('isDeceased') === 'on' ? (formData.get('deathDate') || null) : null,
            birthPlace: formData.get('birthplace'),
            occupation: formData.get('occupation'),
            phoneNumber: formData.get('phoneNumber'),
            biography: formData.get('biography'),
            otherParentId: formData.get('otherParentId') || null
        };
        
        const caretakerIdVal = formData.get('caretakerId');
        if(caretakerIdVal) data.caretakerId = parseInt(caretakerIdVal);
        
        const personId = $('#personId').value;
        const parentId = $('#personParentId').value;
        const spouseId = $('#personSpouseId').value;
        const childId = $('#personChildId') ? $('#personChildId').value : '';
        
        try {
            let savedPerson = null;
            if (personId) {
                // EDIT
                savedPerson = await apiPut(`/api/persons/${personId}`, data);
            } else if (childId) {
                // ADD PARENT (Thêm thân sinh)
                if (parentId) {
                    data.parentId = parentId; // Pass the selected Grandpa ID
                }
                savedPerson = await apiPost(`/api/persons/${childId}/parent`, data);
            } else if (parentId) {
                // ADD CHILD
                data.parentId = parentId;
                savedPerson = await apiPost(`/api/persons/${parentId}/children`, data);
            } else if (spouseId) {
                // ADD SPOUSE
                data.spouseId = spouseId;
                savedPerson = await apiPost(`/api/persons/${spouseId}/spouse`, data);
            } else {
                // ADD ROOT
                data.treeId = this.currentTreeId || this.treeId;
                savedPerson = await apiPost(`/api/trees/${data.treeId}/persons`, data);
            }
            
            showToast('Lưu thành công', 'success');
            closeModal(this.modalId);
            
            // Dispatch event to refresh tree
            document.dispatchEvent(new CustomEvent('tree:refresh'));
            
            // Automatically show the newly added/edited person in the info panel
            if (savedPerson) {
                setTimeout(() => {
                    document.dispatchEvent(new CustomEvent('personSelected', { detail: savedPerson }));
                }, 300); // slight delay to allow tree to refresh visually
            }
            
        } catch (error) {
            showToast('Lưu thất bại (Failed to save)', 'error');
        }
    }
    
    resetForm() {
        if(this.form) this.form.reset();
        $('#personId').value = '';
        $('#personParentId').value = '';
        $('#personSpouseId').value = '';
        if ($('#personChildId')) $('#personChildId').value = '';
        
        const deathDateGroup = $('#deathDateGroup');
        if(deathDateGroup) deathDateGroup.style.display = 'none';
        
        const otherParentGroup = $('#otherParentGroup');
        if(otherParentGroup) otherParentGroup.style.display = 'none';
        
        this.populateCaretakerDropdown(null, null);
        
        $$('.form-error').forEach(el => el.classList.remove('active'));
    }
    
    initGenderSelector() {
        const genderBtns = $$('.gender-btn');
        const genderInput = $('#gender');
        if(!genderBtns || !genderInput) return;
        
        genderBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                genderBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                genderInput.value = btn.getAttribute('data-val');
            });
        });
    }
}
