// Simple configuration viewer
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

// State for storing loaded data
const state = {
    config: null,
    metadata: null,
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
        modulesContainer.classList.remove('hidden');
        settingsContainer.classList.add('hidden');
        renderModulesOverview(modulesContainer);
    } else {
        // Show category settings
        modulesContainer.classList.add('hidden');
        settingsContainer.classList.remove('hidden');
        renderCategorySettings(settingsContainer, category);
    }

    // Update state
    state.currentCategory = category;
}

// Render modules overview
function renderModulesOverview(container) {
    // Loop through each module category
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

            // Toggle switch
            const toggle = document.createElement('label');
            toggle.className = 'switch';

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.checked = enabled;

            const slider = document.createElement('span');
            slider.className = 'slider round';

            toggle.appendChild(checkbox);
            toggle.appendChild(slider);

            // Add components to card
            card.appendChild(info);
            card.appendChild(toggle);

            // Add card to grid
            grid.appendChild(card);
        }

        // Add grid to container
        container.appendChild(grid);
    }
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

    container.appendChild(select);
}

// Set up event listeners
function setupEventListeners() {
    // Save button
    document.getElementById('save-config').addEventListener('click', saveConfig);

    // Reset button
    document.getElementById('reset-config').addEventListener('click', resetConfig);

    // Search
    document.getElementById('search').addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        filterSettings(searchTerm);
    });
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

// Save configuration changes
async function saveConfig() {
    // Collect changes
    const changes = {};
    let hasChanges = false;

    // Switches
    document.querySelectorAll('.switch-setting').forEach(setting => {
        const key = setting.dataset.key;
        const checked = setting.querySelector('input[type="checkbox"]').checked;

        if (state.config[key] !== checked) {
            changes[key] = checked;
            hasChanges = true;
        }
    });

    // Sliders
    document.querySelectorAll('.slider-setting').forEach(setting => {
        const key = setting.dataset.key;
        const value = parseFloat(setting.querySelector('.range-slider').value);

        if (state.config[key] !== value) {
            changes[key] = value;
            hasChanges = true;
        }
    });

    // Text inputs
    document.querySelectorAll('.text-setting').forEach(setting => {
        const key = setting.dataset.key;
        const value = setting.querySelector('.text-input').value;

        if (state.config[key] !== value) {
            changes[key] = value;
            hasChanges = true;
        }
    });

    // Colors
    document.querySelectorAll('.color-setting').forEach(setting => {
        const key = setting.dataset.key;
        const picker = setting.querySelector('.color-picker');
        const alpha = setting.querySelector('.alpha-slider');

        const rgb = hexToRgb(picker.value);
        const color = {
            r: rgb.r,
            g: rgb.g,
            b: rgb.b,
            a: parseInt(alpha.value)
        };

        // Check if color changed
        const currentColor = state.config[key];
        if (!currentColor ||
            currentColor.r !== color.r ||
            currentColor.g !== color.g ||
            currentColor.b !== color.b ||
            currentColor.a !== color.a) {

            changes[key] = color;
            hasChanges = true;
        }
    });

    // Selectors
    document.querySelectorAll('.selector-setting').forEach(setting => {
        const key = setting.dataset.key;
        const value = parseInt(setting.querySelector('select').value);

        if (state.config[key] !== value) {
            changes[key] = value;
            hasChanges = true;
        }
    });

    // If no changes, show message and return
    if (!hasChanges) {
        setStatus('No changes to save', 'info');
        return;
    }

    // Save changes
    setStatus('Saving changes...', 'info');

    try {
        const response = await fetch('/api/config', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(changes)
        });

        if (!response.ok) {
            throw new Error(`Server returned ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.status === 'success') {
            // Update local state
            for (const key in changes) {
                state.config[key] = changes[key];
            }

            setStatus('Changes saved successfully!', 'success');

            // If we're in the Overview, refresh it to show updated toggles
            if (state.currentCategory === 'Overview') {
                showCategory('Overview');
            }
        } else {
            throw new Error(result.message || 'Unknown error');
        }
    } catch (error) {
        setStatus('Error saving changes: ' + error.message, 'error');
    }
}

// Reset configuration to defaults
async function resetConfig() {
    // Confirmation
    if (!confirm('Are you sure you want to reset all settings to defaults?')) {
        return;
    }

    setStatus('Resetting configuration...', 'info');

    try {
        const response = await fetch('/api/config/reset', {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`Server returned ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.status === 'success') {
            setStatus('Configuration reset successfully. Reloading...', 'success');

            // Reload the page after a short delay
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            throw new Error(result.message || 'Unknown error');
        }
    } catch (error) {
        setStatus('Error resetting configuration: ' + error.message, 'error');
    }
}

// Set status message
function setStatus(message, type = 'info') {
    const statusElement = document.getElementById('status-message');
    statusElement.textContent = message;
    statusElement.className = 'status-message ' + type;

    // Auto-clear success messages
    if (type === 'success') {
        setTimeout(() => {
            if (statusElement.textContent === message) {
                clearStatus();
            }
        }, 3000);
    }
}

// Clear status message
function clearStatus() {
    const statusElement = document.getElementById('status-message');
    statusElement.className = 'status-message';
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