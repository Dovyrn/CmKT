import React, { useState } from 'react';
import { Settings, Moon, Sun, Save, RotateCcw, ChevronRight, BellRing, Database, User, Zap } from 'lucide-react';

const ConfigInterface = () => {
  // State for all configuration options
  const [config, setConfig] = useState({
    // Theme settings
    darkMode: true,
    accentColor: '#8b5cf6', // Purple
    fontSize: 16,
    borderRadius: 12,
    
    // Feature toggles
    enableNotifications: true,
    enableAutosave: true,
    enableAnalytics: false,
    
    // Performance settings
    animationSpeed: 300,
    cacheSize: 50,
    
    // User settings
    username: 'User123',
    apiKey: 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx',
  });

  // Handle different input changes
  const handleToggleChange = (key) => {
    setConfig({...config, [key]: !config[key]});
  };
  
  const handleSliderChange = (key, value) => {
    setConfig({...config, [key]: value});
  };
  
  const handleColorChange = (key, value) => {
    setConfig({...config, [key]: value});
  };
  
  const handleTextChange = (key, value) => {
    setConfig({...config, [key]: value});
  };
  
  // Demo reset function
  const resetConfig = () => {
    alert('Settings would be reset to defaults');
  };
  
  // Demo save function
  const saveConfig = () => {
    alert('Settings saved successfully!');
  };

  // Section data with icons
  const sections = [
    { name: 'Theme', icon: <Moon size={18} /> },
    { name: 'Features', icon: <BellRing size={18} /> },
    { name: 'Performance', icon: <Zap size={18} /> },
    { name: 'User', icon: <User size={18} /> }
  ];

  return (
    <div className="flex h-screen w-full bg-black text-gray-300 overflow-hidden">
      {/* Sidebar */}
      <div className="hidden md:flex w-64 flex-col backdrop-blur-lg bg-gray-900/50 border-r border-white/5 overflow-hidden">
        <div className="p-6 pb-0">
          <div className="flex items-center gap-3 mb-8">
            <div 
              className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{background: `linear-gradient(135deg, ${config.accentColor}, rgba(139, 92, 246, 0.5))`}}
            >
              <Settings size={16} color="white" />
            </div>
            <h1 className="text-lg font-semibold text-white">Configurator</h1>
          </div>
        </div>
        
        <div className="px-3 py-4">
          <nav className="space-y-1">
            {sections.map((section) => (
              <a 
                key={section.name} 
                href="#" 
                className="flex items-center justify-between py-3 px-4 rounded-xl hover:bg-white/5 transition-all hover:translate-x-1 hover:scale-105"
                style={{transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`}}
              >
                <div className="flex items-center gap-3">
                  <div className="text-gray-400">
                    {section.icon}
                  </div>
                  <span className="font-medium">{section.name}</span>
                </div>
                <ChevronRight size={14} className="text-gray-500" />
              </a>
            ))}
          </nav>
        </div>
        
        <div className="mt-auto p-4">
          <div 
            className="rounded-xl p-4 backdrop-blur-md"
            style={{
              background: "linear-gradient(to bottom right, rgba(30, 30, 40, 0.4), rgba(10, 10, 15, 0.2))",
              borderTop: "1px solid rgba(255, 255, 255, 0.05)",
              boxShadow: "0 4px 24px -8px rgba(0, 0, 0, 0.3)"
            }}
          >
            <div className="flex items-center gap-3">
              <div 
                className="w-10 h-10 rounded-full flex items-center justify-center text-white"
                style={{background: `linear-gradient(135deg, ${config.accentColor}, rgba(139, 92, 246, 0.5))`}}
              >
                <span className="font-medium">U</span>
              </div>
              <div>
                <p className="text-sm font-medium text-white">{config.username}</p>
                <p className="text-xs text-gray-400">Administrator</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="flex-1 flex flex-col bg-gradient-to-br from-gray-950 to-black">
        {/* Header */}
        <header 
          className="h-16 flex items-center justify-between px-6 backdrop-blur-md"
          style={{
            background: "rgba(10, 10, 15, 0.3)",
            borderBottom: "1px solid rgba(255, 255, 255, 0.03)"
          }}
        >
          <div className="flex items-center gap-3 md:hidden">
            <div 
              className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{background: `linear-gradient(135deg, ${config.accentColor}, rgba(139, 92, 246, 0.5))`}}
            >
              <Settings size={16} color="white" />
            </div>
            <h1 className="text-lg font-semibold text-white">Configurator</h1>
          </div>
          
          <div className="flex items-center gap-4">
            <button 
              className="p-2 rounded-full backdrop-blur-md transition-all hover:bg-white/10 hover:scale-110 active:scale-95"
              style={{
                background: "rgba(30, 30, 40, 0.4)",
                border: "1px solid rgba(255, 255, 255, 0.05)",
                transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
              }}
              onClick={() => handleToggleChange('darkMode')}
            >
              {config.darkMode ? 
                <Sun size={18} color="#f9fafb" className="animate-spin-slow" style={{animationDuration: '10s'}} /> : 
                <Moon size={18} color="#f9fafb" className="animate-pulse" />
              }
            </button>
            
            <style jsx>{`
              @keyframes spin-slow {
                from {
                  transform: rotate(0deg);
                }
                to {
                  transform: rotate(360deg);
                }
              }
              .animate-spin-slow {
                animation: spin-slow 10s linear infinite;
              }
            `}</style>
          </div>
        </header>

        {/* Content area */}
        <div className="flex-1 overflow-auto p-6 pb-20">
          <div className="max-w-5xl mx-auto">
            <div className="flex items-center justify-between mb-8">
              <h2 className="text-2xl font-semibold text-white">Configuration</h2>
              
              <div className="flex items-center gap-3">
                <button 
                  onClick={resetConfig}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg backdrop-blur-md hover:bg-white/10 transition-all hover:scale-105 active:scale-95"
                  style={{
                    background: "rgba(30, 30, 40, 0.4)",
                    border: "1px solid rgba(255, 255, 255, 0.05)",
                    borderRadius: `${config.borderRadius}px`,
                    transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                  }}
                >
                  <RotateCcw size={14} className="transition-transform hover:rotate-180" style={{transition: `transform ${config.animationSpeed}ms ease`}} />
                  <span className="text-sm font-medium">Reset</span>
                </button>
                <button 
                  onClick={saveConfig}
                  className="flex items-center gap-2 px-4 py-2 text-white rounded-lg transition-all hover:scale-105 active:scale-95"
                  style={{
                    background: `linear-gradient(135deg, ${config.accentColor}, rgba(139, 92, 246, 0.8))`,
                    borderRadius: `${config.borderRadius}px`,
                    boxShadow: `0 4px 14px -4px ${config.accentColor}`,
                    transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                  }}
                >
                  <Save size={14} className="transition-transform" />
                  <span className="text-sm font-medium">Save</span>
                </button>
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Theme Settings Card */}
              <div 
                className="rounded-xl overflow-hidden backdrop-blur-lg"
                style={{
                  background: "linear-gradient(to bottom right, rgba(30, 30, 40, 0.5), rgba(10, 10, 15, 0.3))",
                  border: "1px solid rgba(255, 255, 255, 0.05)",
                  boxShadow: "0 8px 32px -4px rgba(0, 0, 0, 0.1)"
                }}
              >
                <div className="p-6 border-b border-white/5 flex items-center gap-3">
                  <Moon size={18} className="text-gray-400" />
                  <h3 className="text-lg font-medium text-white">Theme</h3>
                </div>
                <div className="p-6 space-y-6">
                  <div className="space-y-3">
                    <label className="block text-sm font-medium text-gray-300">Accent Color</label>
                    <div className="flex items-center gap-4">
                      <input 
                        type="color" 
                        value={config.accentColor}
                        onChange={(e) => handleColorChange('accentColor', e.target.value)}
                        className="w-12 h-12 rounded-lg cursor-pointer bg-transparent overflow-hidden"
                        style={{borderRadius: `${config.borderRadius}px`}}
                      />
                      <div 
                        className="px-4 py-2 rounded-lg font-mono text-sm"
                        style={{
                          background: "rgba(20, 20, 30, 0.5)",
                          border: "1px solid rgba(255, 255, 255, 0.05)"
                        }}
                      >
                        {config.accentColor}
                      </div>
                    </div>
                  </div>
                  
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <label className="text-sm font-medium text-gray-300">Font Size</label>
                      <span className="text-sm font-medium text-white">{config.fontSize}px</span>
                    </div>
                    <input 
                      type="range" 
                      min="12" 
                      max="24" 
                      value={config.fontSize}
                      onChange={(e) => handleSliderChange('fontSize', parseInt(e.target.value))}
                      className="w-full h-2 rounded-full appearance-none bg-black/30 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:bg-white"
                      style={{accentColor: config.accentColor}}
                    />
                  </div>
                  
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <label className="text-sm font-medium text-gray-300">Border Radius</label>
                      <span className="text-sm font-medium text-white">{config.borderRadius}px</span>
                    </div>
                    <input 
                      type="range" 
                      min="0" 
                      max="20" 
                      value={config.borderRadius}
                      onChange={(e) => handleSliderChange('borderRadius', parseInt(e.target.value))}
                      className="w-full h-2 rounded-full appearance-none bg-black/30 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:bg-white"
                      style={{accentColor: config.accentColor}}
                    />
                  </div>
                </div>
              </div>
              
              {/* Feature Toggles Card */}
              <div 
                className="rounded-xl overflow-hidden backdrop-blur-lg"
                style={{
                  background: "linear-gradient(to bottom right, rgba(30, 30, 40, 0.5), rgba(10, 10, 15, 0.3))",
                  border: "1px solid rgba(255, 255, 255, 0.05)",
                  boxShadow: "0 8px 32px -4px rgba(0, 0, 0, 0.1)"
                }}
              >
                <div className="p-6 border-b border-white/5 flex items-center gap-3">
                  <BellRing size={18} className="text-gray-400" />
                  <h3 className="text-lg font-medium text-white">Features</h3>
                </div>
                <div className="divide-y divide-white/5">
                  <div className="flex items-center justify-between p-6">
                    <div>
                      <h4 className="text-sm font-medium text-white">Notifications</h4>
                      <p className="text-xs text-gray-400 mt-1">Receive alerts and updates</p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input 
                        type="checkbox" 
                        className="sr-only peer"
                        checked={config.enableNotifications}
                        onChange={() => handleToggleChange('enableNotifications')}
                      />
                      <div 
                        className={`w-12 h-6 rounded-full peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:rounded-full after:h-5 after:w-5 after:transition-all ${config.enableNotifications ? 'after:bg-white after:animate-pulse' : 'after:bg-gray-400'}`}
                        style={{
                          backgroundColor: config.enableNotifications ? config.accentColor : 'rgba(30, 30, 40, 0.5)',
                          boxShadow: config.enableNotifications ? `0 0 8px ${config.accentColor}` : 'none',
                          transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                        }}
                      ></div>
                    </label>
                  </div>
                  
                  <div className="flex items-center justify-between p-6">
                    <div>
                      <h4 className="text-sm font-medium text-white">Autosave</h4>
                      <p className="text-xs text-gray-400 mt-1">Automatically save changes</p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input 
                        type="checkbox" 
                        className="sr-only peer"
                        checked={config.enableAutosave}
                        onChange={() => handleToggleChange('enableAutosave')}
                      />
                      <div 
                        className={`w-12 h-6 rounded-full peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:rounded-full after:h-5 after:w-5 after:transition-all ${config.enableAutosave ? 'after:bg-white after:animate-pulse' : 'after:bg-gray-400'}`}
                        style={{
                          backgroundColor: config.enableAutosave ? config.accentColor : 'rgba(30, 30, 40, 0.5)',
                          boxShadow: config.enableAutosave ? `0 0 8px ${config.accentColor}` : 'none',
                          transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                        }}
                      ></div>
                    </label>
                  </div>
                  
                  <div className="flex items-center justify-between p-6">
                    <div>
                      <h4 className="text-sm font-medium text-white">Analytics</h4>
                      <p className="text-xs text-gray-400 mt-1">Collect usage data</p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input 
                        type="checkbox" 
                        className="sr-only peer"
                        checked={config.enableAnalytics}
                        onChange={() => handleToggleChange('enableAnalytics')}
                      />
                      <div 
                        className={`w-12 h-6 rounded-full peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:rounded-full after:h-5 after:w-5 after:transition-all ${config.enableAnalytics ? 'after:bg-white after:animate-pulse' : 'after:bg-gray-400'}`}
                        style={{
                          backgroundColor: config.enableAnalytics ? config.accentColor : 'rgba(30, 30, 40, 0.5)',
                          boxShadow: config.enableAnalytics ? `0 0 8px ${config.accentColor}` : 'none',
                          transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                        }}
                      ></div>
                    </label>
                  </div>
                </div>
              </div>
              
              {/* Performance Settings Card */}
              <div 
                className="rounded-xl overflow-hidden backdrop-blur-lg"
                style={{
                  background: "linear-gradient(to bottom right, rgba(30, 30, 40, 0.5), rgba(10, 10, 15, 0.3))",
                  border: "1px solid rgba(255, 255, 255, 0.05)",
                  boxShadow: "0 8px 32px -4px rgba(0, 0, 0, 0.1)"
                }}
              >
                <div className="p-6 border-b border-white/5 flex items-center gap-3">
                  <Zap size={18} className="text-gray-400" />
                  <h3 className="text-lg font-medium text-white">Performance</h3>
                </div>
                <div className="p-6 space-y-6">
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <label className="text-sm font-medium text-gray-300">Animation Speed</label>
                      <span className="text-sm font-medium text-white">{config.animationSpeed}ms</span>
                    </div>
                    <input 
                      type="range" 
                      min="100" 
                      max="1000" 
                      step="50"
                      value={config.animationSpeed}
                      onChange={(e) => handleSliderChange('animationSpeed', parseInt(e.target.value))}
                      className="w-full h-2 rounded-full appearance-none bg-black/30 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:bg-white"
                      style={{accentColor: config.accentColor}}
                    />
                  </div>
                  
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <label className="text-sm font-medium text-gray-300">Cache Size</label>
                      <span className="text-sm font-medium text-white">{config.cacheSize}MB</span>
                    </div>
                    <input 
                      type="range" 
                      min="10" 
                      max="100" 
                      step="5"
                      value={config.cacheSize}
                      onChange={(e) => handleSliderChange('cacheSize', parseInt(e.target.value))}
                      className="w-full h-2 rounded-full appearance-none bg-black/30 [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:bg-white"
                      style={{accentColor: config.accentColor}}
                    />
                  </div>
                  
                  <div className="pt-4 flex justify-end">
                    <button 
                      className="px-4 py-2 text-sm rounded-lg backdrop-blur-md hover:bg-white/10 transition-all hover:scale-105 active:scale-95"
                      style={{
                        background: "rgba(30, 30, 40, 0.4)",
                        border: "1px solid rgba(255, 255, 255, 0.05)",
                        borderRadius: `${config.borderRadius}px`,
                        transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                      }}
                    >
                      <span className="relative inline-block">
                        Optimize
                        <span className="absolute inset-0 rounded-lg opacity-0 hover:opacity-10 bg-white transition-opacity" style={{animation: "pulse 2s infinite"}}></span>
                      </span>
                    </button>
                  </div>
                </div>
              </div>
              
              {/* User Settings Card */}
              <div 
                className="rounded-xl overflow-hidden backdrop-blur-lg"
                style={{
                  background: "linear-gradient(to bottom right, rgba(30, 30, 40, 0.5), rgba(10, 10, 15, 0.3))",
                  border: "1px solid rgba(255, 255, 255, 0.05)",
                  boxShadow: "0 8px 32px -4px rgba(0, 0, 0, 0.1)"
                }}
              >
                <div className="p-6 border-b border-white/5 flex items-center gap-3">
                  <User size={18} className="text-gray-400" />
                  <h3 className="text-lg font-medium text-white">User</h3>
                </div>
                <div className="p-6 space-y-6">
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-gray-300">Username</label>
                    <input 
                      type="text" 
                      value={config.username}
                      onChange={(e) => handleTextChange('username', e.target.value)}
                      className="w-full px-4 py-2.5 bg-black/20 border border-white/10 text-white focus:outline-none focus:ring-2 focus:border-transparent focus:ring-opacity-50"
                      style={{
                        borderRadius: `${config.borderRadius}px`,
                        focusRingColor: config.accentColor
                      }}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-gray-300">API Key</label>
                    <input 
                      type="text" 
                      value={config.apiKey}
                      onChange={(e) => handleTextChange('apiKey', e.target.value)}
                      className="w-full px-4 py-2.5 bg-black/20 border border-white/10 text-white focus:outline-none focus:ring-2 focus:border-transparent focus:ring-opacity-50"
                      style={{
                        borderRadius: `${config.borderRadius}px`,
                        focusRingColor: config.accentColor
                      }}
                    />
                  </div>
                </div>
              </div>
            </div>
            
            {/* Preview Section */}
            <div 
              className="mt-6 rounded-xl overflow-hidden backdrop-blur-lg"
              style={{
                background: "linear-gradient(to bottom right, rgba(30, 30, 40, 0.5), rgba(10, 10, 15, 0.3))",
                border: "1px solid rgba(255, 255, 255, 0.05)",
                boxShadow: "0 8px 32px -4px rgba(0, 0, 0, 0.1)"
              }}
            >
              <div className="p-6 border-b border-white/5">
                <h3 className="text-lg font-medium text-white">Preview</h3>
              </div>
              <div className="p-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div 
                    className="p-6 rounded-lg text-center backdrop-blur-md"
                    style={{
                      background: "rgba(0, 0, 0, 0.2)",
                      border: "1px solid rgba(255, 255, 255, 0.03)",
                      borderRadius: `${config.borderRadius}px`
                    }}
                  >
                    <p className="text-sm font-medium text-gray-400 mb-4">Button</p>
                    <button 
                      className="px-6 py-2.5 text-white shadow-md transition-all hover:shadow-lg hover:scale-110 active:scale-95 group"
                      style={{
                        background: `linear-gradient(135deg, ${config.accentColor}, rgba(139, 92, 246, 0.8))`,
                        borderRadius: `${config.borderRadius}px`,
                        fontSize: `${config.fontSize}px`,
                        boxShadow: `0 4px 14px -4px ${config.accentColor}`,
                        transition: `all ${config.animationSpeed}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
                      }}
                    >
                      <span className="relative inline-flex overflow-hidden items-center">
                        <span className="z-10">Button</span>
                        <span className="absolute inset-0 bg-white opacity-0 group-hover:opacity-20 group-active:opacity-10" 
                          style={{
                            transform: "translateX(-100%)",
                            animation: "shimmer 2s infinite",
                            transition: `all ${config.animationSpeed}ms ease`
                          }}>
                        </span>
                      </span>
                    </button>
                    
                    <style jsx>{`
                      @keyframes shimmer {
                        0% {
                          transform: translateX(-100%);
                        }
                        50% {
                          transform: translateX(100%);
                        }
                        100% {
                          transform: translateX(100%);
                        }
                      }
                      @keyframes pulse {
                        0% {
                          box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.4);
                        }
                        70% {
                          box-shadow: 0 0 0 10px rgba(255, 255, 255, 0);
                        }
                        100% {
                          box-shadow: 0 0 0 0 rgba(255, 255, 255, 0);
                        }
                      }
                    `}</style>
                  </div>
                  
                  <div 
                    className="p-6 rounded-lg text-center backdrop-blur-md"
                    style={{
                      background: "rgba(0, 0, 0, 0.2)",
                      border: "1px solid rgba(255, 255, 255, 0.03)",
                      borderRadius: `${config.borderRadius}px`
                    }}
                  >
                    <p className="text-sm font-medium text-gray-400 mb-4">Color</p>
                    <div 
                      className="w-16 h-16 mx-auto rounded-full shadow-lg"
                      style={{
                        background: `linear-gradient(135deg, ${config.accentColor}, rgba(139, 92, 246, 0.5))`,
                        boxShadow: `0 8px 16px -4px ${config.accentColor}80`
                      }}
                    ></div>
                  </div>
                  
                  <div 
                    className="p-6 rounded-lg text-center backdrop-blur-md"
                    style={{
                      background: "rgba(0, 0, 0, 0.2)",
                      border: "1px solid rgba(255, 255, 255, 0.03)",
                      borderRadius: `${config.borderRadius}px`
                    }}
                  >
                    <p className="text-sm font-medium text-gray-400 mb-4">Typography</p>
                    <p style={{fontSize: `${config.fontSize}px`}} className="text-white font-medium">Sample Text</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConfigInterface;