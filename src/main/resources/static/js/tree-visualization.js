/**
 * Family Tree Visualization using D3.js
 */
class FamilyTreeVisualization {
    constructor(containerId, treeId) {
        this.container = document.getElementById(containerId);
        this.treeId = treeId;
        this.width = this.container.clientWidth || 1000;
        this.height = this.container.clientHeight || 800;
        
        // Node dimensions
        this.nodeWidth = 280;
        this.nodeHeight = 120;
        
        this.svg = d3.select(`#${containerId}`).append("svg")
            .attr("width", "100%")
            .attr("height", "100%")
            .attr("viewBox", `0 0 ${this.width} ${this.height}`)
            .style("background-color", "#F5F0E8");
            
        this.g = this.svg.append("g");
        
        // Add Empty State Overlay
        this.emptyState = document.createElement('div');
        this.emptyState.className = 'empty-state-overlay';
        this.emptyState.innerHTML = '<div style="display:flex; justify-content:center; align-items:center; height:100%; color:#fff;"><div style="text-align:center;"><div style="font-size:40px;margin-bottom:10px;">👤</div><p>Chưa có thành viên nào. Nhấn "Thêm thành viên" để bắt đầu.</p><button class="btn btn-primary" onclick="if(window.personFormManager) window.personFormManager.openAddRootForm()">+ Thêm thành viên đầu tiên</button></div></div>';
        this.emptyState.style.display = 'none';
        this.emptyState.style.height = '100%';
        this.emptyState.style.width = '100%';
        this.emptyState.style.position = 'absolute';
        this.emptyState.style.top = '0';
        this.emptyState.style.left = '0';
        this.container.appendChild(this.emptyState);
        
        // Setup Zoom - allow free zoom with scroll wheel
        this.zoom = d3.zoom()
            .scaleExtent([0.05, 5])
            .on("zoom", (event) => {
                this.g.attr("transform", event.transform);
                // Update zoom indicator
                const pct = Math.round(event.transform.k * 100);
                const indicator = document.getElementById('zoom-indicator');
                if (indicator) indicator.textContent = pct + '%';
            });
            
        this.svg.call(this.zoom);
        
        // Wire up zoom buttons after DOM ready
        setTimeout(() => this.initZoomButtons(), 100);
        
        // Context menu setup
        this.setupContextMenu();
    }
    
    initZoomButtons() {
        const btnIn    = document.getElementById('btn-zoom-in');
        const btnOut   = document.getElementById('btn-zoom-out');
        const btnReset = document.getElementById('btn-zoom-reset');
        
        if (btnIn) btnIn.addEventListener('click', () => {
            this.svg.transition().duration(300)
                .call(this.zoom.scaleBy, 1.5);
        });
        if (btnOut) btnOut.addEventListener('click', () => {
            this.svg.transition().duration(300)
                .call(this.zoom.scaleBy, 0.67);
        });
        if (btnReset) btnReset.addEventListener('click', () => {
            this.fitToScreen();
        });
        
        // Prevent page scroll when wheeling over SVG (D3 zoom handles wheel natively)
        this.container.addEventListener('wheel', (event) => {
            event.preventDefault();
        }, { passive: false });
    }
    
    setupFilters() {
        const filterGeneration = document.getElementById('filter-generation');
        const filterBranch = document.getElementById('filter-branch');
        if (!filterGeneration || !filterBranch || !this.rawPersons) return;
        
        const gens = new Set();
        this.rawPersons.forEach(p => {
            if (p.generation) gens.add(p.generation);
        });
        
        const sortedGens = Array.from(gens).sort((a,b) => a - b);
        const currentGen = filterGeneration.value;
        filterGeneration.innerHTML = '<option value="">-- Chọn đời --</option>';
        sortedGens.forEach(gen => {
            const option = document.createElement('option');
            option.value = gen;
            option.textContent = `Đời thứ ${gen}`;
            filterGeneration.appendChild(option);
        });
        
        if (currentGen && gens.has(parseInt(currentGen))) {
            filterGeneration.value = currentGen;
        }
        
        if (!this.filterEventsBound) {
            filterGeneration.addEventListener('change', () => {
                const gen = filterGeneration.value;
                filterBranch.innerHTML = '<option value="">-- Chọn nhánh --</option>';
                if (gen) {
                    filterBranch.disabled = false;
                    const peopleInGen = this.rawPersons.filter(p => p.generation == gen).sort((a,b) => (a.birthOrder || 99) - (b.birthOrder || 99));
                    peopleInGen.forEach(p => {
                        const option = document.createElement('option');
                        option.value = p.id;
                        option.textContent = `${p.fullName || p.name} ${p.birthDate ? '(' + new Date(p.birthDate).getFullYear() + ')' : ''}`;
                        filterBranch.appendChild(option);
                    });
                } else {
                    filterBranch.disabled = true;
                    this.viewBranchId = null;
                    this.loadData();
                }
            });
            
            filterBranch.addEventListener('change', () => {
                const personId = filterBranch.value;
                if (personId) {
                    this.viewBranchId = personId;
                    this.loadData();
                } else {
                    this.viewBranchId = null;
                    this.loadData();
                }
            });
            
            this.filterEventsBound = true;
        }
    }

    // View toggles are now handled entirely in tree-view.html
    async loadData() {
        try {
            // Fetch data from API
            this.rawPersons = await apiGet(`/api/trees/${this.treeId}/persons`); // flat list for table and dropdowns
            
            // Store caretaker IDs for visualization
            this.caretakerIds = new Set(this.rawPersons.filter(p => p.caretakerId).map(p => p.caretakerId));
            
            this.setupFilters();
            
            let data;
            if (this.viewBranchId) {
                data = await apiGet(`/api/trees/persons/${this.viewBranchId}/branch-data`);
            } else {
                data = await apiGet(`/api/trees/${this.treeId}/data`); // hierarchical data
            }
            if (data && Object.keys(data).length > 0) {
                this.svg.style("display", "block");
                this.emptyState.style.display = "none";
                this.render(data);
            } else {
                // Empty tree handling
                this.svg.style("display", "none");
                this.emptyState.style.display = "block";
                this.g.selectAll("*").remove(); // clear canvas if needed
            }
            
            // Dispatch event for other modules
            document.dispatchEvent(new CustomEvent('tree:dataLoaded', { detail: this.rawPersons }));
            
        } catch (error) {
            console.error("Failed to load tree data:", error);
            showToast("Lỗi khi tải dữ liệu gia phả (Failed to load tree)", "error");
        }
    }
    
    render(treeData) {
        // Clear previous render
        this.g.selectAll("*").remove();
        
        // Process data for d3.hierarchy
        // In a real scenario, the backend should return a hierarchical structure 
        // or a flat list we can stratify. Assuming hierarchical here.
        const root = d3.hierarchy(treeData, d => {
            // Ensure children are sorted by birth order
            if (d.children && d.children.length > 0) {
                return d.children.sort((a, b) => (a.birthOrder || 0) - (b.birthOrder || 0));
            }
            return null;
        });
        
        this.root = root; // Expose root globally for table filtering
        
        // Setup tree layout
        const treeLayout = d3.tree()
            .nodeSize([this.nodeWidth + 60, this.nodeHeight + 100])
            .separation((a, b) => {
                let distance = 1.2;
                if (a.data.spouses && a.data.spouses.length > 0) distance += a.data.spouses.length * 1.1;
                if (b.data.spouses && b.data.spouses.length > 0) distance += b.data.spouses.length * 1.1;
                
                if (a.parent === b.parent) {
                    // Anh em cùng cha mẹ thì để gần nhau
                    return distance;
                } else {
                    // Khác nhánh (anh em họ) thì tạo khoảng trống lớn (cộng thêm 2.0) để các cành không đâm vào nhau
                    return distance + 2.0;
                }
            });
            
        treeLayout(root);
        
        // Post-process layout to align children under their respective spouse
        root.each(d => {
            d.shiftX = 0;
            if (d.parent) {
                d.shiftX += d.parent.shiftX || 0; // inherit shift from parent
                if (d.data.otherParentId && d.parent.data.spouses) {
                    const spouseIndex = d.parent.data.spouses.findIndex(s => s.id == d.data.otherParentId);
                    if (spouseIndex !== -1) {
                        d.shiftX += (spouseIndex + 1) * (this.nodeWidth + 40);
                    }
                }
            }
            d.x += d.shiftX;
        });
        
        // Draw Links
        const link = this.g.selectAll(".link")
            .data(root.links())
            .join("path")
            .attr("class", "link")
            .attr("d", d => {
                let sourceX = d.source.x;
                let sourceY = d.source.y + this.nodeHeight / 2;
                let spouseIndex = -1;
                
                // If this child belongs to a specific spouse, adjust the sourceX to start between the father and the spouse
                if (d.target.data.otherParentId && d.source.data.spouses) {
                    spouseIndex = d.source.data.spouses.findIndex(s => s.id == d.target.data.otherParentId);
                    if (spouseIndex !== -1) {
                        const spouseX = d.source.x + (spouseIndex + 1) * (this.nodeWidth + 40);
                        const prevX = spouseIndex === 0 ? d.source.x : d.source.x + spouseIndex * (this.nodeWidth + 40);
                        sourceX = (prevX + spouseX) / 2;
                        sourceY = d.source.y;
                    }
                }
                
                const tgtX = d.target.x;
                const tgtY = d.target.y - this.nodeHeight / 2;
                // Add a small vertical offset based on spouseIndex so horizontal lines don't merge
                const midY = (sourceY + tgtY) / 2 + (spouseIndex + 1) * 15;
                return `M ${sourceX} ${sourceY} L ${sourceX} ${midY} L ${tgtX} ${midY} L ${tgtX} ${tgtY}`;
            })
            .attr("fill", "none")
            .attr("stroke", "#3b82f6")
            .attr("stroke-width", 2);
            
        // Draw Nodes
        const node = this.g.selectAll(".node")
            .data(root.descendants())
            .enter().append("g")
            .attr("class", "node")
            .attr("transform", d => `translate(${d.x - this.nodeWidth/2},${d.y - this.nodeHeight/2})`)
            .on("click", (event, d) => {
                if(!d.data.id) {
                    showToast("Đây là Thủy Tổ ảo (gom nhóm các nhánh rời rạc). Không thể xem chi tiết.", "info");
                    return;
                }
                if(!event.target.closest('.node-btn-add') && !event.target.closest('.node-btn-menu')) {
                    this.onNodeClick(d.data);
                }
            })
            .on("contextmenu", (event, d) => {
                if(!d.data.id) {
                    event.preventDefault();
                    return;
                }
                this.onContextMenu(event, d.data);
            })
            .on("dblclick", (event, d) => this.centerOnNode(d));
            
        // Add HTML content using foreignObject
        const fo = node.append("foreignObject")
            .attr("width", this.nodeWidth)
            .attr("height", this.nodeHeight);
            
        fo.append("xhtml:div")
            .html(d => this.generateNodeHtml(d.data, d.depth + 1));
            
        // Add quick add buttons as SVG elements layered on top
        const btnGroup = node.append("g").attr("class", "node-actions");
        
        // Add Child Button (bottom)
        const btnAddChild = btnGroup.filter(d => d.data.id).append("g")
            .attr("class", "node-btn-add")
            .attr("transform", `translate(${this.nodeWidth/2}, ${this.nodeHeight})`)
            .on("click", (event, d) => {
                event.stopPropagation();
                if(window.personFormManager && d.data.id) window.personFormManager.openAddChildForm(d.data.id);
            });
        btnAddChild.append("circle").attr("r", 12).attr("class", "node-btn-add-bg").attr("stroke", "#1e1e1e").attr("stroke-width", 2);
        btnAddChild.append("text").text("+").attr("text-anchor", "middle").attr("y", 5).attr("fill", "white").attr("font-weight", "bold");

        // Add Menu Button (top right)
        const btnMenu = btnGroup.filter(d => d.data.id).append("g")
            .attr("class", "node-btn-menu")
            .attr("transform", `translate(${this.nodeWidth - 16}, 24)`)
            .style("cursor", "pointer")
            .style("pointer-events", "all")
            .on("click", (event, d) => {
                event.stopPropagation();
                this.onContextMenu(event, d.data);
            });
        btnMenu.append("rect").attr("width", 24).attr("height", 36).attr("rx", 4).attr("x", -12).attr("y", -18).attr("fill", "black");
        btnMenu.append("text").text("⋮").attr("text-anchor", "middle").attr("y", 5).attr("fill", "white").attr("font-weight", "bold").attr("font-size", "18px");

        // Handle spouses
        const nodesWithSpouse = root.descendants().filter(d => d.data.spouses && d.data.spouses.length > 0);
        
        nodesWithSpouse.forEach(d => {
            d.data.spouses.forEach((spouse, index) => {
                const spouseX = d.x + this.nodeWidth + 20 + (index * (this.nodeWidth + 40));
                
                // Draw spouse link
                this.g.append("path")
                    .attr("class", "link-spouse")
                    .attr("d", `M ${index === 0 ? d.x + this.nodeWidth/2 : d.x + this.nodeWidth/2 + index * (this.nodeWidth + 40)} ${d.y} L ${spouseX - this.nodeWidth/2} ${d.y}`)
                    .attr("stroke", "#e83e8c")
                    .attr("stroke-width", 2);
                    
                // Add heart icon in middle of link
                this.g.append("text")
                    .attr("x", (index === 0 ? d.x + this.nodeWidth/2 : d.x + this.nodeWidth/2 + index * (this.nodeWidth + 40)) + 10)
                    .attr("y", d.y + 5)
                    .attr("fill", "#e83e8c")
                    .style("font-size", "16px")
                    .text("♥");
                    
                // Draw spouse node
                const spouseGroup = this.g.append("g")
                    .attr("transform", `translate(${spouseX - this.nodeWidth/2},${d.y - this.nodeHeight/2})`)
                    .on("click", (event) => {
                        if(!event.target.closest('.node-btn-menu')) this.onNodeClick(spouse);
                    })
                    .on("contextmenu", (event) => this.onContextMenu(event, spouse));
                    
                spouseGroup.append("foreignObject")
                    .attr("width", this.nodeWidth)
                    .attr("height", this.nodeHeight)
                    .append("xhtml:div")
                    .html(this.generateNodeHtml(spouse, d.depth + 1));
                    
                // Spouse Menu Button
                const spouseBtnMenu = spouseGroup.append("g")
                    .attr("class", "node-btn-menu")
                    .attr("transform", `translate(${this.nodeWidth - 16}, 24)`)
                    .style("cursor", "pointer")
                    .style("pointer-events", "all")
                    .on("click", (event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        this.onContextMenu(event, spouse);
                    });
                spouseBtnMenu.append("rect").attr("width", 24).attr("height", 36).attr("rx", 4).attr("x", -12).attr("y", -18).attr("fill", "black");
                spouseBtnMenu.append("text").text("⋮").attr("text-anchor", "middle").attr("y", 5).attr("fill", "white").attr("font-weight", "bold").attr("font-size", "18px");
            });
        });
        
        // Add Spouse Button (right) on nodes without spouse
        const nodesWithoutSpouse = root.descendants().filter(d => (!d.data.spouses || d.data.spouses.length === 0) && d.data.id);
        nodesWithoutSpouse.forEach(d => {
            const btnGroupSelection = d3.select(node.nodes()[d.data.id === root.data.id ? 0 : root.descendants().findIndex(n => n.data.id === d.data.id)]).select(".node-actions");
            const btnAddSpouse = btnGroupSelection.append("g")
                .attr("class", "node-btn-add")
                .attr("transform", `translate(${this.nodeWidth}, ${this.nodeHeight/2})`)
                .on("click", (event) => {
                    event.stopPropagation();
                    if(window.personFormManager && d.data.id) window.personFormManager.openAddSpouseForm(d.data.id);
                });
            btnAddSpouse.append("circle").attr("r", 12).attr("class", "node-btn-add-spouse-bg").attr("stroke", "#1e1e1e").attr("stroke-width", 2);
            btnAddSpouse.append("text").text("+").attr("text-anchor", "middle").attr("y", 5).attr("fill", "white").attr("font-weight", "bold");
        });
        
        // Render Generation Labels
        this.renderGenerationLabels(root);
        
        // Initial centering
        this.fitToScreen();
    }
    
    generateNodeHtml(data, generation) {
        const isMale = data.gender === 'NAM';
        // Light theme colors
        const borderColor = isMale ? '#1a56c4' : '#b5245e';
        const avatarBg   = isMale ? '#dbeafe' : '#fce7f3';
        const badgeBg    = isMale ? '#1a56c4' : '#b5245e';
        const fullName = data.name || [data.ho, data.tenDem, data.ten].filter(Boolean).join(' ');
        const isCaretaker = this.caretakerIds && this.caretakerIds.has(data.id);
        
        return `
            <div style="background-color: #ffffff; border: 2px solid ${borderColor}; border-radius: 10px; width: 100%; height: 100%; display: flex; align-items: center; padding: 8px 10px; position: relative; box-sizing: border-box; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.12);">
                <!-- Black Ribbon for Deceased (top-left) -->
                ${data.isDeceased ? `<div style="position: absolute; top: -15px; left: -25px; width: 70px; height: 35px; background-color: #333; transform: rotate(-45deg); box-shadow: 0 2px 4px rgba(0,0,0,0.3); z-index: 1;"></div>` : ''}
                
                <!-- Gold Ribbon for Caretaker (top-right) -->
                ${isCaretaker ? `<div style="position: absolute; top: -15px; right: -25px; width: 70px; height: 35px; background-color: #d4a017; transform: rotate(45deg); box-shadow: 0 2px 4px rgba(0,0,0,0.3); z-index: 1;"></div>` : ''}
                
                <!-- ID badge top-left -->
                <div style="position: absolute; top: 5px; left: ${data.isDeceased ? '32px' : '7px'}; background: ${badgeBg}; color: white; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: bold; z-index: 2;">#${data.id}</div>
                <!-- Generation badge top-right (shifted left to make room for menu) -->
                <div style="position: absolute; top: 5px; right: ${isCaretaker ? '60px' : '36px'}; background: #f0ede6; color: #5a4a3a; padding: 2px 6px; border-radius: 4px; font-size: 10px; z-index: 2; font-weight: 700;">Đời ${generation}</div>
                
                <!-- Avatar -->
                <div style="width: 56px; height: 56px; border-radius: 50%; background: ${avatarBg}; margin-right: 12px; overflow: hidden; display: flex; align-items: center; justify-content: center; flex-shrink: 0; position: relative; z-index: 2; margin-top: 16px; border: 2px solid ${borderColor};">
                    ${data.avatarUrl ? `<img src="${data.avatarUrl}" style="width:100%;height:100%;object-fit:cover;">` : `<span style="font-size:28px;">${isMale ? '👨' : '👩'}</span>`}
                </div>
                
                <!-- Right details -->
                <div style="flex: 1; padding: 0 10px; display: flex; flex-direction: column; justify-content: center; height: 100%; min-width: 0; box-sizing: border-box;">
                    <div style="color: #111827; font-weight: 700; font-size: 18px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; text-align: center; font-family: 'Outfit', sans-serif; letter-spacing: -0.2px;">${fullName || '(Chưa có tên)'}</div>
                    <div style="color: #6b7280; font-size: 14px; text-align: center; margin-top: 3px;">${isMale ? 'Nam' : 'Nữ'}${data.birthDate ? ' • ' + new Date(data.birthDate).getFullYear() : ''}</div>
                    ${data.isDeceased ? `<div style="color: #6b7280; font-size: 13px; text-align: center; margin-top: 1px;">✝ Đã mất</div>` : ''}
                    ${isCaretaker ? `<div style="color: #b8860b; font-size: 13px; text-align: center; font-weight: bold; margin-top: 1px;">⭐ Cúng dường</div>` : ''}
                </div>
            </div>
        `;
    }
    
    renderGenerationLabels(root) {
        const generations = new Map();
        let minX = Infinity;
        
        root.each(d => {
            if (!generations.has(d.depth)) {
                generations.set(d.depth, d.y);
            }
            if (d.x < minX) {
                minX = d.x;
            }
        });
        
        // Place labels to the left of the leftmost node
        const labelX = minX - this.nodeWidth / 2 - 120;
        
        generations.forEach((y, depth) => {
            this.g.append("rect")
                .attr("x", labelX - 10)
                .attr("y", y - 18)
                .attr("width", 90)
                .attr("height", 26)
                .attr("rx", 6)
                .attr("fill", "#6B4C3B")
                .attr("opacity", 0.85);
            this.g.append("text")
                .attr("x", labelX + 35)
                .attr("y", y)
                .attr("class", "generation-label")
                .attr("fill", "#fff")
                .attr("text-anchor", "middle")
                .style("font-family", "'Outfit', sans-serif")
                .style("font-size", "14px")
                .style("font-weight", "bold")
                .text(`Đời ${depth + 1}`);
        });
    }
    
    onNodeClick(data) {
        // Dispatch custom event to open side panel
        document.dispatchEvent(new CustomEvent('person:select', { detail: data }));
        
        const sidePanel = $('.side-panel');
        if (sidePanel) {
            sidePanel.classList.add('active');
            
            // Populate basic details (handled by another script or directly here)
            $('#panel-name').textContent = data.fullName;
            $('#panel-dates').textContent = `${formatDate(data.birthDate)} - ${data.isDeceased ? formatDate(data.deathDate) : 'Nay'}`;
            // ... load more
        }
    }
    
    setupContextMenu() {
        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.style.position = 'absolute';
        menu.style.display = 'none';
        menu.style.zIndex = '1000';
        
        menu.innerHTML = `
            <div class="context-menu-item" id="ctx-add-child"><i class="fas fa-baby"></i> Thêm Con</div>
            <div class="context-menu-item" id="ctx-add-spouse"><i class="fas fa-heart"></i> Thêm Vợ/Chồng</div>
            <div class="context-menu-divider"></div>
            <div class="context-menu-item" id="ctx-edit"><i class="fas fa-pencil-alt"></i> Sửa thông tin</div>
            <div class="context-menu-item danger" id="ctx-delete"><i class="fas fa-trash"></i> Xóa thành viên</div>
            <div class="context-menu-item danger" id="ctx-delete-branch"><i class="fas fa-trash-alt"></i> Xóa nhánh này</div>
            <div class="context-menu-divider"></div>
            <div class="context-menu-item" id="ctx-copy-branch"><i class="fas fa-copy"></i> Sao chép nhánh</div>
            <div class="context-menu-item" id="ctx-paste-branch"><i class="fas fa-paste"></i> Dán nhánh</div>
            <div class="context-menu-divider"></div>
            <div class="context-menu-item" id="ctx-view-branch"><i class="fas fa-code-branch"></i> Xem riêng nhánh này</div>
            <div class="context-menu-item" id="ctx-view-up"><i class="fas fa-level-up-alt"></i> Xem nhánh ngược lên</div>
            <div class="context-menu-divider"></div>
            <div class="context-menu-item" id="ctx-close"><i class="fas fa-times"></i> Đóng</div>
        `;
        document.body.appendChild(menu);
        this.contextMenu = menu;
        
        document.addEventListener('click', () => {
            this.contextMenu.style.display = 'none';
            this.contextMenu.classList.remove('show');
        });
        
        document.getElementById('ctx-add-child').addEventListener('click', () => {
            if (this.selectedNodeId) window.personFormManager.openAddChildForm(this.selectedNodeId);
        });
        
        document.getElementById('ctx-add-spouse').addEventListener('click', () => {
            if (this.selectedNodeId) window.personFormManager.openAddSpouseForm(this.selectedNodeId);
        });
        
        document.getElementById('ctx-edit').addEventListener('click', () => {
            if (this.selectedNodeId) window.personFormManager.openEditForm(this.selectedNodeId);
        });
        
        document.getElementById('ctx-delete').addEventListener('click', () => {
            if (this.selectedNodeId) this.deleteNode(this.selectedNodeId);
        });
        
        document.getElementById('ctx-delete-branch').addEventListener('click', () => {
            if (this.selectedNodeId) this.deleteBranch(this.selectedNodeId);
        });
        
        document.getElementById('ctx-copy-branch').addEventListener('click', () => {
            if (this.selectedNodeId) {
                window.copiedBranchSourceId = this.selectedNodeId;
                showToast("Đã sao chép nhánh. Vui lòng chọn người khác và Dán nhánh.", "info");
            }
        });
        
        document.getElementById('ctx-paste-branch').addEventListener('click', () => {
            if (this.selectedNodeId && window.copiedBranchSourceId) {
                this.pasteBranch(window.copiedBranchSourceId, this.selectedNodeId);
            } else {
                showToast("Bạn chưa sao chép nhánh nào!", "warning");
            }
        });
        document.getElementById('ctx-view-branch').addEventListener('click', () => {
            if (this.selectedNodeId) {
                this.viewBranchId = this.selectedNodeId;
                this.loadData();
            }
        });
        
        document.getElementById('ctx-view-up').addEventListener('click', () => {
            // Reset to full tree
            this.viewBranchId = null;
            this.loadData();
        });
    }
    
    onContextMenu(event, data) {
        event.preventDefault();
        this.selectedNodeId = data.id;
        
        this.contextMenu.style.left = `${event.pageX}px`;
        this.contextMenu.style.top = `${event.pageY}px`;
        this.contextMenu.style.display = 'block';
        this.contextMenu.classList.add('show');
    }
    
    async deleteNode(id) {
        if(confirm("Bạn có chắc chắn muốn xóa người này?")) {
            try {
                await apiDelete(`/api/persons/${id}`);
                showToast("Đã xóa thành công", "success");
                this.loadData();
            } catch (err) {
                showToast("Lỗi khi xóa", "error");
            }
        }
    }
    
    async deleteBranch(id) {
        if(confirm("CẢNH BÁO: Bạn có chắc chắn muốn xóa người này và toàn bộ con cháu của họ không? Hành động này không thể hoàn tác!")) {
            try {
                await apiDelete(`/api/persons/${id}/branch`);
                showToast("Đã xóa nhánh thành công", "success");
                this.loadData();
            } catch (err) {
                showToast("Lỗi khi xóa nhánh", "error");
            }
        }
    }
    
    async pasteBranch(sourceId, targetId) {
        if(sourceId === targetId) {
            showToast("Không thể dán nhánh vào chính nó!", "error");
            return;
        }
        if(confirm("Bạn có muốn dán nhánh đã chép làm con của người này không?")) {
            try {
                await apiPost(`/api/persons/${targetId}/paste-branch?sourceId=${sourceId}`);
                showToast("Đã sao chép nhánh thành công", "success");
                this.loadData();
                window.copiedBranchSourceId = null;
            } catch (err) {
                showToast("Lỗi khi dán nhánh", "error");
            }
        }
    }
    
    centerOnNode(d) {
        const scale = 1;
        const x = -d.x * scale + this.width / 2;
        const y = -d.y * scale + this.height / 2;
        
        this.svg.transition().duration(750).call(
            this.zoom.transform,
            d3.zoomIdentity.translate(x, y).scale(scale)
        );
    }
    
    zoomIn() {
        this.svg.transition().call(this.zoom.scaleBy, 1.2);
    }
    
    zoomOut() {
        this.svg.transition().call(this.zoom.scaleBy, 0.8);
    }
    
    resetZoom() {
        this.svg.transition().call(this.zoom.transform, d3.zoomIdentity);
    }
    
    fitToScreen() {
        this.width = this.container.clientWidth || 1000;
        this.height = this.container.clientHeight || 800;
        this.svg.attr("viewBox", `0 0 ${this.width} ${this.height}`);
        if (!this.width || !this.height) return;
        const initialScale = this.width < 768 ? 0.45 : 0.8;
        this.svg.transition().duration(750).call(
            this.zoom.transform,
            d3.zoomIdentity.translate(this.width / 2, 80).scale(initialScale)
        );
    }
}


