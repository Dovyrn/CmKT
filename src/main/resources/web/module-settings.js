// Global state to store our configuration and metadata
let configData = {};
let metadataData = {};
let currentValues = {};

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    // Load configuration and metadata
    Promise.all([
        fetch('/api/config').then(response => response.json()),
        fetch('/api/metadata').then(response => response.json())
    ]).then(([config, metadata]) => {
        configData = config;
        metadataData = metadata;
        currentValues = structuredClone(config); // Create a deep copy for tracking changes
        
        // Build UI with the loaded data
        initializeUI();
        
        // Attach event listeners
        setupEventListeners();
    }).catch(error => {
        console.error('Error loading configuration:', error);
        showStatusMessage('Failed to load configuration', 'error');
    });
});

// Initialize the user interface
function initializeUI() {
    // Build modules for each category
    buildModules('combat', 'combat-modules');
    buildModules('render', 'render-modules');
    buildModules('utilities', 'utilities-modules');
    buildModules('developer', 'developer-modules');
}

// Build modules for a specific category
function buildModules(category, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    
    // Clear the container
    container.innerHTML = '';
    
    // Get modules for this category
    const modules = Object.entries(metadataData.modules)
        .filter(([_, moduleInfo]) => moduleInfo.category === category);
    
    // Create module elements
    modules.forEach(([moduleId, moduleInfo]) => {
        const moduleElement = createModuleElement(moduleId, moduleInfo);
        container.appendChild(moduleElement);
    });
}

// Create a module element
function createModuleElement(moduleId, moduleInfo) {
    // Create module container
    const moduleDiv = document.createElement('div');
    moduleDiv.classList.add('module');
    moduleDiv.dataset.moduleId = moduleId;
    
    // Add enable/disable toggle if the module has a main toggle
    const mainToggleProperty = findMainToggleProperty(moduleId);
    
    // Create module header
    const headerDiv = document.createElement('div');
    headerDiv.classList.add('module-header');
    headerDiv.innerHTML = `
        <div class="module-info">
            <i class="${moduleInfo.icon} module-icon"></i>
            <div class="module-title-area">
                <div class="module-title">${moduleInfo.name}</div>
                <div class="module-subtitle">${moduleInfo.description}</div>
            </div>
        </div>
        <div class="module-controls">
            ${mainToggleProperty ? `
                <label class="toggle">
                    <input type="checkbox" class="module-toggle" data-property="${mainToggleProperty}" ${getPropertyValue(moduleId, mainToggleProperty) ? 'checked' : ''}>
                    <span class="toggle-slider"></span>
                </label>
            ` : ''}
            <button class="module-expand">
                <i class="fas fa-chevron-down"></i>
            </button>
        </div>
    `;
    
    // Create settings container
    const settingsDiv = document.createElement('div');
    settingsDiv.classList.add('module-settings');
    
    // Add settings
    const moduleProperties = metadataData.properties[moduleId];
    if (moduleProperties) {
        Object.entries(moduleProperties).forEach(([propertyName, propertyInfo]) => {
            // Skip the main toggle property (it's in the header)
            if (propertyName === mainToggleProperty) return;
            
            const settingRow = createSettingElement(moduleId, propertyName, propertyInfo);
            settingsDiv.appendChild(settingRow);
        });
    }
    
    // Assemble the module
    moduleDiv.appendChild(headerDiv);
    moduleDiv.appendChild(settingsDiv);
    
    // Set disabled class if there's a main toggle and it's off
    if (mainToggleProperty && !getPropertyValue(moduleId, mainToggleProperty)) {
        moduleDiv.classList.add('disabled');
    }
    
    return moduleDiv;
}

// Find the main toggle property for a module (usually ends with "Enabled")
function findMainToggleProperty(moduleId) {
    const moduleProperties = metadataData.properties[moduleId];
    if (!moduleProperties) return null;
    
    // Look for property with "SWITCH" type and name ending with "Enabled"
    const mainToggle = Object.entries(moduleProperties).find(([name, info]) => 
        info.type === 'SWITCH' && 
        (name.endsWith('Enabled') || name === moduleId)
    );
    
    return mainToggle ? mainToggle[0] : null;
}

// Create setting element based on property type
function createSettingElement(moduleId, propertyName, propertyInfo) {
    const settingRow = document.createElement('div');
    settingRow.classList.add('setting-row');
    
    // Label section
    const labelDiv = document.createElement('div');
    labelDiv.classList.add('setting-label');
    labelDiv.innerHTML = `
        <span>${propertyInfo.name}</span>
        <small>${propertyInfo.description}</small>
    `;
    
    // Control section (varies by type)
    const controlDiv = document.createElement('div');
    controlDiv.classList.add('setting-control');
    
    // Different controls by property type
    switch (propertyInfo.type) {
        case 'SWITCH':
            controlDiv.innerHTML = `
                <label class="toggle">
                    <input type="checkbox" class="setting-toggle" data-module="${moduleId}" data-property="${propertyName}" ${getPropertyValue(moduleId, propertyName) ? 'checked' : ''}>
                    <span class="toggle-slider"></span>
                </label>
            `;
            break;
            
        case 'SLIDER':
            const sliderValue = getPropertyValue(moduleId, propertyName);
            let suffix = '';
            
            // Try to determine the appropriate suffix
            if (propertyName.includes('Delay')) suffix = 'ms';
            else if (propertyName.includes('Height')) suffix = ' blocks';
            else if (propertyName.includes('Speed')) suffix = '×';
            else if (propertyName.includes('FOV')) suffix = '°';
            
            controlDiv.innerHTML = `
                <div class="slider-with-value">
                    <input type="range" class="slider" data-module="${moduleId}" data-property="${propertyName}" 
                        min="${propertyInfo.min}" max="${propertyInfo.max}" value="${sliderValue}">
                    <div class="slider-value">${sliderValue}${suffix}</div>
                </div>
            `;
            break;
            
        case 'DECIMAL_SLIDER':
            const decimalValue = getPropertyValue(moduleId, propertyName);
            let decimalSuffix = '';
            
            controlDiv.innerHTML = `
                <div class="slider-with-value">
                    <input type="range" class="slider" data-module="${moduleId}" data-property="${propertyName}" 
                        min="${propertyInfo.minF}" max="${propertyInfo.maxF}" step="0.1" value="${decimalValue}">
                    <div class="slider-value">${decimalValue.toFixed(1)}${decimalSuffix}</div>
                </div>
            `;
            break;
            
        case 'PERCENT_SLIDER':
            const percentValue = getPropertyValue(moduleId, propertyName);
            
            controlDiv.innerHTML = `
                <div class="slider-with-value">
                    <input type="range" class="slider" data-module="${moduleId}" data-property="${propertyName}" 
                        min="0" max="1" step="0.01" value="${percentValue}">
                    <div class="slider-value">${Math.round(percentValue * 100)}%</div>
                </div>
            `;
            break;
            
        case 'SELECTOR':
            const selectorValue = getPropertyValue(moduleId, propertyName);
            
            let optionsHtml = '';
            propertyInfo.options.forEach((option, index) => {
                optionsHtml += `<option value="${index}" ${selectorValue === index ? 'selected' : ''}>${option}</option>`;
            });
            
            controlDiv.innerHTML = `
                <select class="select-control" data-module="${moduleId}" data-property="${propertyName}">
                    ${optionsHtml}
                </select>
            `;
            break;
            
        case 'TEXT':
            const textValue = getPropertyValue(moduleId, propertyName);
            
            controlDiv.innerHTML = `
                <input type="text" class="text-input" data-module="${moduleId}" data-property="${propertyName}" value="${textValue}">
            `;
            break;
            
        case 'COLOR':
            const colorValue = getPropertyValue(moduleId, propertyName);
            const hexColor = colorToHex(colorValue);
            
            controlDiv.innerHTML = `
                <div class="color-with-value">
                    <input type="color" class="color-picker" data-module="${moduleId}" data-property="${propertyName}" value="${hexColor}">
                    <div class="color-value">${hexColor}</div>
                </div>
            `;
            break;
            
        default:
            controlDiv.innerHTML = `<div>Unsupported control: ${propertyInfo.type}</div>`;
    }
    
    // Assemble the setting row
    settingRow.appendChild(labelDiv);
    settingRow.appendChild(controlDiv);
    
    return settingRow;
}

// Get property value from current state
function getPropertyValue(moduleId, propertyName) {
    try {
        return currentValues[moduleId][propertyName];
    } catch (error) {
        console.error(`Error getting value for ${moduleId}.${propertyName}`, error);
        return null;
    }
}

// Update property value in current state
function updatePropertyValue(moduleId, propertyName, value) {
    try {
        // Ensure the module exists
        if (!currentValues[moduleId]) {
            currentValues[moduleId] = {};
        }
        
        // Update the value
        currentValues[moduleId][propertyName] = value;
        
        // If this is a main toggle, update the UI
        if (propertyName.endsWith('Enabled') || propertyName === moduleId) {
            const moduleElement = document.querySelector(`.module[data-module-id="${moduleId}"]`);
            if (moduleElement) {
                if (value) {
                    moduleElement.classList.remove('disabled');
                } else {
                    moduleElement.classList.add('disabled');
                }
            }
        }
    } catch (error) {
        console.error(`Error updating value for ${moduleId}.${propertyName}`, error);
    }
}

// Color utilities
function colorToHex(color) {
    if (!color) return '#000000';
    
    try {
        return `#${componentToHex(color.r)}${componentToHex(color.g)}${componentToHex(color.b)}`;
    } catch (error) {
        console.error('Error converting color to hex:', error);
        return '#000000';
    }
}

function componentToHex(c) {
    const hex = c.toString(16);
    return hex.length === 1 ? '0' + hex : hex;
}

function hexToColor(hex) {
    // Remove # if present
    hex = hex.replace('#', '');
    
    // Parse the hex values
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    
    // Create color object
    return { r, g, b, a: 255 };
}

// Event listeners
function setupEventListeners() {
    // Category tab switching
    document.querySelectorAll('.category-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            // Remove active class from all tabs and content sections
            document.querySelectorAll('.category-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.category-content').forEach(c => c.classList.remove('active'));
            
            // Add active class to clicked tab
            this.classList.add('active');
            
            // Show corresponding content
            const category = this.getAttribute('data-category');
            document.getElementById(`${category}-content`).classList.add('active');
        });
    });
    
    // Module expand/collapse
    document.addEventListener('click', function(event) {
        if (event.target.classList.contains('module-expand') || 
            event.target.closest('.module-expand')) {
            
            const button = event.target.classList.contains('module-expand') ? 
                            event.target : event.target.closest('.module-expand');
            const module = button.closest('.module');
            module.classList.toggle('expanded');
            
            // Change the icon based on expanded state
            const icon = button.querySelector('i');
            if (module.classList.contains('expanded')) {
                icon.classList.replace('fa-chevron-down', 'fa-chevron-up');
            } else {
                icon.classList.replace('fa-chevron-up', 'fa-chevron-down');
            }
        }
    });
    
    // Hide all button
    document.getElementById('hideAll').addEventListener('click', function() {
        document.querySelectorAll('.module.expanded').forEach(module => {
            module.classList.remove('expanded');
            const icon = module.querySelector('.module-expand i');
            icon.classList.replace('fa-chevron-up', 'fa-chevron-down');
        });
    });
    
    // Save config button
    document.getElementById('saveConfig').addEventListener('click', function() {
        saveConfig();
    });
    
    // Setting change event delegations
    document.addEventListener('change', function(event) {
        // Toggle switch
        if (event.target.classList.contains('setting-toggle') || 
            event.target.classList.contains('module-toggle')) {
            
            const moduleId = event.target.dataset.module || event.target.closest('.module').dataset.moduleId;
            const property = event.target.dataset.property;
            const value = event.target.checked;
            
            updatePropertyValue(moduleId, property, value);
        }
        
        // Select control
        if (event.target.classList.contains('select-control')) {
            const moduleId = event.target.dataset.module;
            const property = event.target.dataset.property;
            const value = parseInt(event.target.value, 10);
            
            updatePropertyValue(moduleId, property, value);
        }
        
        // Text input
        if (event.target.classList.contains('text-input')) {
            const moduleId = event.target.dataset.module;
            const property = event.target.dataset.property;
            const value = event.target.value;
            
            updatePropertyValue(moduleId, property, value);
        }
        
        // Color picker
        if (event.target.classList.contains('color-picker')) {
            const moduleId = event.target.dataset.module;
            const property = event.target.dataset.property;
            const hexValue = event.target.value;
            const colorValue = hexToColor(hexValue);
            
            // Update the hex display
            const colorValueElement = event.target.nextElementSibling;
            if (colorValueElement) {
                colorValueElement.textContent = hexValue;
            }
            
            updatePropertyValue(moduleId, property, colorValue);
        }
    });
    
    // Slider input events
    document.addEventListener('input', function(event) {
        if (event.target.classList.contains('slider')) {
            const moduleId = event.target.dataset.module;
            const property = event.target.dataset.property;
            const valueElement = event.target.parentElement.querySelector('.slider-value');
            
            // Get the property metadata
            const propertyInfo = metadataData.properties[moduleId][property];
            let value;
            
            switch (propertyInfo.type) {
                case 'SLIDER':
                    value = parseInt(event.target.value, 10);
                    
                    // Determine suffix
                    let suffix = '';
                    if (property.includes('Delay')) suffix = 'ms';
                    else if (property.includes('Height')) suffix = ' blocks';
                    else if (property.includes('Speed')) suffix = '×';
                    else if (property.includes('FOV')) suffix = '°';
                    
                    if (valueElement) {
                        valueElement.textContent = `${value}${suffix}`;
                    }
                    break;
                    
                case 'DECIMAL_SLIDER':
                    value = parseFloat(event.target.value);
                    if (valueElement) {
                        valueElement.textContent = value.toFixed(1);
                    }
                    break;
                    
                case 'PERCENT_SLIDER':
                    value = parseFloat(event.target.value);
                    if (valueElement) {
                        valueElement.textContent = `${Math.round(value * 100)}%`;
                    }
                    break;
                    
                default:
                    value = event.target.value;
            }
            
            updatePropertyValue(moduleId, property, value);
        }
    });
}

// Save configuration to server
function saveConfig() {
    fetch('/api/config', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(currentValues)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showStatusMessage('Configuration saved successfully', 'success');
        } else {
            showStatusMessage('Failed to save configuration', 'error');
        }
    })
    .catch(error => {
        console.error('Error saving configuration:', error);
        showStatusMessage('Error saving configuration', 'error');
    });
}

// Show status message
function showStatusMessage(message, type = 'success') {
    // Create or get existing status message element
    let statusElement = document.querySelector('.status-message');
    
    if (!statusElement) {
        statusElement = document.createElement('div');
        statusElement.classList.add('status-message');
        document.body.appendChild(statusElement);
    }
    
    // Set message content
    statusElement.innerHTML = `
        <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
        <span>${message}</span>
    `;
    
    // Apply styles
    statusElement.className = 'status-message';
    statusElement.classList.add(type);
    
    // Show the message
    setTimeout(() => {
        statusElement.classList.add('visible');
    }, 10);
    
    // Hide the message after a delay
    setTimeout(() => {
        statusElement.classList.remove('visible');
    }, 3000);
}