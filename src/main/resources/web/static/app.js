// Simple configuration viewer
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

// State for storing loaded data
const state = {
    config: null,
    metadata: null,
    dependencies: {},
    currentCategory: 'Overview',
    modules: null,
    loading: true
};

// Main initialization
async function initApp() {
    // Show loading state
    setStatus('Loading configuration...', 'info');

    try {
        // Load config and modules data
        const [configData, modulesData] = await Promise.all([
            fetchConfig(),
            fetchModules()
        ]);

        state.config = configData.values;
        state.metadata = configData.metadata;
        state.modules = modulesData;
        state.loading = false;

        // Determine dependencies
        determineDependencies();

        // Build the navigation
        buildNavigation();

        // Show the overview by default
        showCategory('Overview');

        // Set up event listeners
        setupEventListeners();

        // Clear loading message
        clearStatus();
    } catch (error) {
        console.error('Failed to initialize app:', error);
        setStatus('Error loading configuration: ' + error.message, 'error');
    }
}

// Fetch configuration data
async function fetchConfig() {
    const response = await fetch('/api/config');
    if (!response.ok) {
        throw new Error(`Server returned ${response.status}: ${response.statusText}`);
    }
    return await response.json();
}

// Fetch modules data
async function fetchModules() {
    const response = await fetch('/api/modules');
    if (!response.ok) {
        throw new Error(`Server returned ${response.status}: ${response.statusText}`);
    }
    return await response.json();
}

// Determine module dependencies
function determineDependencies() {
    state.dependencies = {};

    // Look through all metadata to find dependencies
    for (const key in state.metadata) {
        const meta = state.metadata[key];
        if (meta && meta.category) {
            // Find the main module for this setting
            const mainModules = findMainModulesForSetting(key);

            // Associate this setting with its main modules
            mainModules.forEach(mainModule => {
                if (!state.dependencies[mainModule]) {
                    state.dependencies[mainModule] = [];
                }
                state.dependencies[mainModule].push(key);
            });
        }
    }
}

// Find main modules for a given setting
function findMainModulesForSetting(settingKey) {
    const mainModules = [];
    const specialMappings = {
        'targetHudToggled': ['Target HUD'],
        'maceDiveEnabled': ['Mace Dive'],
        'backtrackEnabled': ['Backtrack'],
        'aimAssistEnabled': ['Aim Assist'],
        'HitboxEnabled': ['Hitbox'],
        'chamsEnabled': ['Chams'],
        'storageEspEnabled': ['Storage ESP'],
        'espEnabled': ['ESP'],
        'sprint': ['Sprint'],
        'noJumpDelay': ['No Jump Delay'],
        'fullBright': ['Full Bright'],
        'weaponSwapper': ['Weapon Swapper'],
        'maceDTap': ['Mace D-Tap']
    };

    // Check special mappings first
    if (specialMappings[settingKey]) {
        return specialMappings[settingKey];
    }

    // If no direct mapping, try finding by substring
    for (const [moduleName, moduleKey] of Object.entries(specialMappings)) {
        if (settingKey.includes(moduleKey[0].replace(/\s+/g, '').toLowerCase())) {
            return moduleKey;
        }
    }

    return [];
}

// Build the navigation sidebar
function buildNavigation() {
    const navElement = document.getElementById('category-nav');
    navElement.innerHTML = '';

    // Add Overview first
    addNavItem(navElement, 'Overview');

    // Add categories from metadata
    const categories = new Set();

    for (const key in state.metadata) {
        const meta = state.metadata[key];
        if (meta && meta.category) {
            categories.add(meta.category);
        }
    }

    // Add other categories in alphabetical order
    Array.from(categories).sort().forEach(category => {
        if (category !== 'Overview') {
            addNavItem(navElement, category);
        }
    });
}

// Add a navigation item
function addNavItem(parent, category) {
    const li = document.createElement('li');
    const a = document.createElement('a');
    a.href = '#';
    a.textContent = category;
    a.dataset.category = category;
    a.addEventListener('click', (e) => {
        e.preventDefault();
        showCategory(category);
    });

    li.appendChild(a);
    parent.appendChild(li);
}

// Show a specific category
function showCategory(category) {
    // Update active state in navigation
    document.querySelectorAll('#category-nav a').forEach(a => {
        if (a.dataset.category === category) {
            a.classList.add('active');
        } else {
            a.classList.remove('active');
        }
    });

    // Update category title
    document.getElementById('current-category').textContent = category;

    // Get containers
    const modulesContainer = document.getElementById('modules-container');
    const settingsContainer = document.getElementById('settings-container');

    // Clear previous content
    modulesContainer.innerHTML = '';
    settingsContainer.innerHTML = '';

    if (category === 'Overview') {
        // Show modules overview
        renderModulesOverview(modulesContainer);
    } else {
        // Show category settings
        renderCategorySettings(settingsContainer, category);
    }

    // Update state
    state.currentCategory = category;
}

// Render modules overview
function renderModulesOverview(container) {
    for (const category in state.modules) {
        // Create category heading
        const heading = document.createElement('h3');
        heading.textContent = category;
        heading.className = 'subcategory-title';
        container.appendChild(heading);

        // Create grid for modules
        const grid = document.createElement('div');
        grid.className = 'modules-overview';

        // Add module cards
        for (const moduleName in state.modules[category]) {
            const enabled = state.modules[category][moduleName];
            const configKey = findConfigKeyForModule(moduleName);

            // Create module card
            const card = document.createElement('div');
            card.className = 'module-card';

            // Module info
            const info = document.createElement('div');
            info.className = 'module-info';

            const name = document.createElement('h4');
            name.textContent = moduleName;

            const status = document.createElement('p');
            status.textContent = enabled ? 'Enabled' : 'Disabled';

            info.appendChild(name);
            info.appendChild(status);

            // Controls container
            const controls = document.createElement('div');
            controls.className = 'module-controls';

            // Toggle switch
            const toggle = document.createElement('label');
            toggle.className = 'switch';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.checked = enabled;

            if (configKey) {
                checkbox.dataset.configKey = configKey;
                checkbox.addEventListener('change', function() {
                    pendingChanges[configKey] = this.checked;
                    updateChangesCount();
                });
            }

            const slider = document.createElement('span');
            slider.className = 'slider round';

            toggle.appendChild(checkbox);
            toggle.appendChild(slider);

            // Settings icon (if module has settings)
            const hasSettings = configKey && state.dependencies[moduleName];
            if (hasSettings) {
                const settingsIcon = document.createElement('button');
                settingsIcon.className = 'settings-toggle';
                settingsIcon.innerHTML = '<i class="fas fa-cog"></i>';
                settingsIcon.title = 'Module Settings';
                settingsIcon.addEventListener('click', () => toggleModuleSettings(card, moduleName));
                controls.appendChild(settingsIcon);
            }

            controls.appendChild(toggle);

            // Module settings panel
            if (hasSettings) {
                const settingsPanel = document.createElement('div');
                settingsPanel.className = 'module-settings';

                // Add settings for this module
                state.dependencies[moduleName].forEach(settingKey => {
                    const settingMeta = state.metadata[settingKey];
                    if (settingMeta) {
                        const settingElement = createSettingElement(settingKey, settingMeta, state.config[settingKey]);
                        if (settingElement) {
                            settingsPanel.appendChild(settingElement);
                        }
                    }
                });

                card.appendChild(settingsPanel);
            }

            // Add components to card
            card.appendChild(info);
            card.appendChild(controls);

            // Add card to grid
            grid.appendChild(card);
        }

        // Add grid to container
        container.appendChild(grid);
    }

    // Set up settings toggle functionality
    setupSettingsToggles();
}

// Render settings for a category
function renderCategorySettings(container, category) {
    // Group settings by subcategory
    const subcategories = {};

    // Collect settings for this category
    for (const key in state.metadata) {
        const meta = state.metadata[key];
        if (meta && meta.category === category) {
            const subcategory = meta.subcategory || 'General';

            if (!subcategories[subcategory]) {
                subcategories[subcategory] = [];
            }

            subcategories[subcategory].push({
                key,
                meta,
                value: state.config[key]
            });
        }
    }

    // Render each subcategory
    Object.keys(subcategories).sort().forEach(subcategory => {
        const settings = subcategories[subcategory];

        // Skip empty subcategories
        if (settings.length === 0) return;

        // Create subcategory section
        const section = document.createElement('div');
        section.className = 'subcategory';

        // Add heading
        const heading = document.createElement('h3');
        heading.className = 'subcategory-title';
        heading.textContent = subcategory;
        section.appendChild(heading);

        // Add settings list
        const settingsList = document.createElement('div');
        settingsList.className = 'settings-list';

        // Add each setting
        settings.forEach(setting => {
            const settingItem = createSettingElement(setting.key, setting.meta, setting.value);
            if (settingItem) {
                settingsList.appendChild(settingItem);
            }
        });

        section.appendChild(settingsList);
        container.appendChild(section);
    });
}

// Create a setting element
function createSettingElement(key, meta, value) {
    // Create setting container
    const setting = document.createElement('div');
    setting.className = 'setting-item';
    setting.dataset.key = key;

    // Create setting info
    const info = document.createElement('div');
    info.className = 'setting-info';

    const name = document.createElement('div');
    name.className = 'setting-name';
    name.textContent = meta.name || key;

    const description = document.createElement('div');
    description.className = 'setting-description';
    description.textContent = meta.description || '';

    info.appendChild(name);
    info.appendChild(description);
    setting.appendChild(info);

    // Create control based on type
    const type = meta.type || 'unknown';

    switch (type) {
        case 'switch':
            setting.classList.add('switch-setting');
            addSwitchControl(setting, value);
            break;

        case 'slider':
            setting.classList.add('slider-setting');
            addSliderControl(setting, value, meta);
            break;

        case 'text':
            setting.classList.add('text-setting');
            addTextControl(setting, value, meta);
            break;

        case 'color':
            setting.classList.add('color-setting');
            addColorControl(setting, value);
            break;

        case 'selector':
            setting.classList.add('selector-setting');
            addSelectorControl(setting, value, meta);
            break;

        default:
            // For unknown types, just show the value
            const valueDisplay = document.createElement('div');
            valueDisplay.textContent = value;
            setting.appendChild(valueDisplay);
    }

    return setting;
}

// Toggle module settings panel
function toggleModuleSettings(card, moduleName) {
    const settingsPanel = card.querySelector('.module-settings');
    const settingsIcon = card.querySelector('.settings-toggle');

    if (settingsPanel) {
        settingsPanel.classList.toggle('expanded');
        settingsIcon.classList.toggle('active');
    }
}

// Setup settings toggles
function setupSettingsToggles() {
    // Add click event listener to any existing settings icons
    const settingsToggles = document.querySelectorAll('.settings-toggle');
    settingsToggles.forEach(toggle => {
        toggle.addEventListener('click', function() {
            const card = this.closest('.module-card');
            const moduleName = card.querySelector('.module-info h4').textContent;
            toggleModuleSettings(
         card, moduleName);
                             });
                         });
                     }


// Add switch control
function addSwitchControl(container, value) {
    const label = document.createElement('label');
    label.className = 'switch';

    const input = document.createElement('input');
    input.type = 'checkbox';
    input.checked = value;

    const slider = document.createElement('span');
    slider.className = 'slider round';

    label.appendChild(input);
    label.appendChild(slider);
    container.appendChild(label);
}

// Add slider control
function addSliderControl(container, value, meta) {
    const sliderContainer = document.createElement('div');
    sliderContainer.className = 'slider-container';

    const input = document.createElement('input');
    input.type = 'range';
    input.className = 'range-slider';
    input.min = meta.min || 0;
    input.max = meta.max || 100;
    input.step = meta.step || 1;
    input.value = value;

    const display = document.createElement('div');
    display.className = 'value-display';
    display.textContent = meta.format === 'percent' ? `${Math.round(value * 100)}%` : value;

    // Add input event for live updates
    input.addEventListener('input', function() {
        const numValue = parseFloat(this.value);
        display.textContent = meta.format === 'percent' ? `${Math.round(numValue * 100)}%` : numValue;
        pendingChanges[container.dataset.key] = numValue;
        updateChangesCount();
    });

    sliderContainer.appendChild(input);
    sliderContainer.appendChild(display);
    container.appendChild(sliderContainer);
}

// Add text control
function addTextControl(container, value, meta) {
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'text-input';
    input.value = value || '';
    input.placeholder = meta.placeholder || '';

    // Add change listener
    input.addEventListener('change', function() {
        pendingChanges[container.dataset.key] = this.value;
        updateChangesCount();
    });

    container.appendChild(input);
}

// Add color control
function addColorControl(container, value) {
    if (!value) {
        value = { r: 255, g: 255, b: 255, a: 255 };
    }

    const colorContainer = document.createElement('div');
    colorContainer.className = 'color-picker-container';

    // Color picker
    const picker = document.createElement('input');
    picker.type = 'color';
    picker.className = 'color-picker';
    picker.value = rgbToHex(value.r, value.g, value.b);

    // Alpha slider
    const alpha = document.createElement('input');
    alpha.type = 'range';
    alpha.className = 'alpha-slider';
    alpha.min = 0;
    alpha.max = 255;
    alpha.value = value.a;

    // Color preview
    const preview = document.createElement('div');
    preview.className = 'color-preview';
    preview.style.backgroundColor = `rgba(${value.r}, ${value.g}, ${value.b}, ${value.a / 255})`;

    // Update handler
    function updateColor() {
        const rgb = hexToRgb(picker.value);
        const alpha = parseInt(alpha.value);

        preview.style.backgroundColor = `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha / 255})`;

        pendingChanges[container.dataset.key] = {
            r: rgb.r,
            g: rgb.g,
            b: rgb.b,
            a: alpha
        };

        updateChangesCount();
    }

    // Add event listeners
    picker.addEventListener('input', updateColor);
    alpha.addEventListener('input', updateColor);

    colorContainer.appendChild(picker);
    colorContainer.appendChild(alpha);
    colorContainer.appendChild(preview);
    container.appendChild(colorContainer);
}

// Add selector control
function addSelectorControl(container, value, meta) {
    const select = document.createElement('select');
    select.className = 'selector';

    // Add options
    const options = meta.options || [];
    options.forEach((option, index) => {
        const opt = document.createElement('option');
        opt.value = index;
        opt.textContent = option;
        opt.selected = index === value;
        select.appendChild(opt);
    });

    // Add change listener
    select.addEventListener('change', function() {
        pendingChanges[container.dataset.key] = parseInt(this.value);
        updateChangesCount();
    });

    container.appendChild(select);
}

// Find config key for a module name
function findConfigKeyForModule(moduleName) {
    // Common patterns
    const patterns = [
        moduleName.toLowerCase() + 'Enabled',
        moduleName.replace(/\s+/g, '') + 'Enabled',
        moduleName.replace(/\s+/g, '').toLowerCase() + 'Enabled',
        moduleName.replace(/\s+/g, '') + 'Toggled',
        moduleName.toLowerCase().replace(/\s+/g, ''),
    ];

    // Check each pattern
    for (const pattern of patterns) {
        if (pattern in state.config) {
            return pattern;
        }
    }

    // Special cases
    const specialCases = {
        'Target HUD': 'targetHudToggled',
        'ESP': 'espEnabled',
        'Storage ESP': 'storageEspEnabled',
        'Sprint': 'sprint',
        'No Jump Delay': 'noJumpDelay',
        'Full Bright': 'fullBright',
        'Mace Dive': 'maceDiveEnabled',
        'Mace D-Tap': 'maceDTap',
        'Hitbox': 'HitboxEnabled',
        'Weapon Swapper': 'weaponSwapper',
        'Aim Assist': 'aimAssistEnabled',
        'Chams': 'chamsEnabled'
    };

    return specialCases[moduleName] || null;
}

// Set up event listeners
function setupEventListeners() {
    // Save changes buttons (top)
    document.getElementById('save-changes').addEventListener('click', saveChanges);

    // Reset config button
    document.getElementById('reset-config').addEventListener('click', resetConfig);

    // Search input
    document.getElementById('search').addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        filterSettings(searchTerm);
    });
}

// Update the changes counter
function updateChangesCount() {
    const count = Object.keys(pendingChanges).length;
    const changesElement = document.getElementById('changes-count');
    if (changesElement) {
        changesElement.textContent = count === 1 ? '1 change' : `${count} changes`;
    }
}

// Filter settings based on search
function filterSettings(searchTerm) {
    if (!searchTerm) {
        // Show all items if search is empty
        document.querySelectorAll('.setting-item').forEach(item => {
            item.style.display = '';
        });
        document.querySelectorAll('.subcategory').forEach(sub => {
            sub.style.display = '';
        });
        return;
    }

    // Hide/show settings based on search
    document.querySelectorAll('.setting-item').forEach(item => {
        const name = item.querySelector('.setting-name').textContent.toLowerCase();
        const desc = item.querySelector('.setting-description').textContent.toLowerCase();

        if (name.includes(searchTerm) || desc.includes(searchTerm)) {
            item.style.display = '';
        } else {
            item.style.display = 'none';
        }
    });

    // Hide subcategories with no visible settings
    document.querySelectorAll('.subcategory').forEach(sub => {
        const visibleSettings = sub.querySelectorAll('.setting-item[style=""]').length;
        sub.style.display = visibleSettings > 0 ? '' : 'none';
    });
}

// Save changes
async function saveChanges() {
    const count = Object.keys(pendingChanges).length;

    if (count === 0) {
        setStatus('No changes to save', 'info');
        return;
    }

    setStatus(`Saving ${count} change${count === 1 ? '' : 's'}...`, 'info');

    try {
        const response = await fetch('/api/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(pendingChanges)
        });

        if (!response.ok) {
            throw new Error(`Server returned ${response.status} ${response.statusText}`);
        }

        const result = await response.json();

        if (result.status === 'success') {
            // Update local config
            for (const key in pendingChanges) {
                state.config[key] = pendingChanges[key];
            }

            // Clear pending changes
            pendingChanges = {};
            updateChangesCount();

            setStatus(`${count} change${count === 1 ? '' : 's'} saved successfully!`, 'success');

            // Reload modules to refresh the UI
            await fetchModules();
            showCategory(state.currentCategory);
        } else {
            throw new Error(result.message || 'Unknown error');
        }
    } catch (error) {
        setStatus(`Error saving changes: ${error.message}`, 'error');
    }
}

// Reset configuration
async function resetConfig() {
    if (!confirm('Are you sure you want to reset ALL settings to their default values? This cannot be undone.')) {
        return;
    }

    setStatus('Resetting all settings to defaults...', 'info');

    try {
        const response = await fetch('/api/config/reset', {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`Server returned ${response.status} ${response.statusText}`);
        }

        const result = await response.json();

        if (result.status === 'success') {
            setStatus('All settings have been reset to defaults. Reloading...', 'success');

            // Clear pending changes
            pendingChanges = {};
            updateChangesCount();

            // Reload everything after a brief delay
            setTimeout(async () => {
                await initApp();
            }, 1500);
        } else {
            throw new Error(result.message || 'Unknown error');
        }
    } catch (error) {
        setStatus(`Error resetting settings: ${error.message}`, 'error');
    }
}

// Helper function to set status
function setStatus(message, type) {
    const statusElement = document.getElementById('status-message');
    statusElement.textContent = message;
    statusElement.className = `status ${type}`;
    statusElement.style.display = 'block';

    // Auto-hide success messages
    if (type === 'success') {
        setTimeout(() => {
            if (statusElement.textContent === message) {
                statusElement.style.display = 'none';
            }
        }, 3000);
    }
}

// Helper: RGB to Hex
function rgbToHex(r, g, b) {
    return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
}

// Helper: Hex to RGB
function hexToRgb(hex) {
    // Remove # if present
    hex = hex.replace(/^#/, '');

    // Parse hex string
    const bigint = parseInt(hex, 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;

    return { r, g, b };
}

// Initialize pending changes
let pendingChanges = {};