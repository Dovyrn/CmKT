package com.dov.cm.web

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import fi.iki.elonen.NanoHTTPD
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import kotlin.text.Charsets

/**
 * Simple web server for Logical Zoom mod configuration
 * Uses NanoHTTPD for lightweight embedded server
 */
class WebServer(port: Int = 8080) : NanoHTTPD(port) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    init {
        start(SOCKET_READ_TIMEOUT, false)
        UChat.mChat("§aWeb server started on http://localhost:$port")
    }
    
    override fun serve(session: IHTTPSession): Response {
        try {
            val uri = session.uri
            val method = session.method
            
            // Add CORS headers for development 
            var response: Response
            
            // Main page
            if (uri == "/" || uri.isEmpty()) {
                response = newResponse(Response.Status.OK, "text/html", getResource("index.html"))
            }
            // Module settings page
            else if (uri == "/modules") {
                response = newResponse(Response.Status.OK, "text/html", getResource("module-settings.html"))
            }
            // Diagnostic page
            else if (uri == "/diagnostic") {
                response = newResponse(Response.Status.OK, "text/html", getResource("diagnostic.html"))
            }
            // Static resources
            else if (uri.startsWith("/static/")) {
                val resourcePath = uri.substring(8)
                val mimeType = getMimeTypeForResource(resourcePath)
                val resource = getResource(resourcePath)
                
                if (resource != null) {
                    response = newResponse(Response.Status.OK, mimeType, resource)
                } else {
                    UChat.mChat("§cRequested resource not found: $resourcePath")
                    response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Resource not found: $resourcePath")
                }
            }
            // API endpoints for configuration
            else if (uri.startsWith("/api/")) {
                try {
                    when {
                        uri == "/api/config" && method == Method.GET -> {
                            response = newFixedLengthResponse(Response.Status.OK, "application/json", getConfigJson())
                        }
                        uri == "/api/config" && method == Method.POST -> {
                            val files = HashMap<String, String>()
                            session.parseBody(files)
                            val postData = files["postData"] ?: "{}"
                            response = updateConfig(postData)
                        }
                        uri == "/api/modules" && method == Method.GET -> {
                            response = newFixedLengthResponse(Response.Status.OK, "application/json", getModulesJson())
                        }
                        uri == "/api/config/reset" && method == Method.POST -> {
                            response = resetConfig()
                        }
                        else -> {
                            UChat.mChat("§cUnknown API endpoint: $uri with method $method")
                            response = newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Endpoint not found\"}")
                        }
                    }
                } catch (e: Exception) {
                    UChat.mChat("§cAPI error: ${e.message}")
                    e.printStackTrace(java.lang.System.err)
                    response = newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR, 
                        "application/json", 
                        "{\"error\":\"API processing error: ${e.message?.replace("\"", "\\\"") ?: "Unknown error"}\"}"
                    )
                }
            }
            // Default - not found
            else {
                UChat.mChat("§cResource not found: $uri")
                response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found: $uri")
            }
            
            // Add CORS headers to every response
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type")
            
            return response
            
        } catch (e: Exception) {
            UChat.mChat("§cWeb server error: ${e.message}")
            e.printStackTrace()
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error: ${e.message}")
        }
    }
    
    /**
     * Create a new response with the specified content
     */
    private fun newResponse(status: Response.Status, mimeType: String, data: InputStream?): Response {
        return if (data != null) {
            newChunkedResponse(status, mimeType, data)
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Resource not found")
        }
    }
    
    /**
     * Get a resource stream from resources folder
     */
    private fun getResource(path: String): InputStream? {
        // Load from web resources directory
        val resourcePath = "web/$path"
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
        
        // If resource not found in jar, generate it on the fly
        if (stream == null && path == "index.html") {
            return ByteArrayInputStream(generateIndexHtml().toByteArray(Charsets.UTF_8))
        } else if (stream == null && path == "module-settings.html") {
            return ByteArrayInputStream(generateModuleSettingsHtml().toByteArray(Charsets.UTF_8))
        } else if (stream == null && path == "diagnostic.html") {
            return ByteArrayInputStream(generateDiagnosticHtml().toByteArray(Charsets.UTF_8))
        } else if (stream == null && path == "logo.svg") {
            return ByteArrayInputStream(generateLogoSvg().toByteArray(Charsets.UTF_8))
        } else if (stream == null && path == "styles.css") {
            return ByteArrayInputStream(generateCss().toByteArray(Charsets.UTF_8))
        } else if (stream == null && path == "app.js") {
            return ByteArrayInputStream(generateJs().toByteArray(Charsets.UTF_8))
        }
        
        return stream
    }
    
    /**
     * Get mime type based on file extension
     */
    private fun getMimeTypeForResource(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") -> "image/jpeg"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".ico") -> "image/x-icon"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * Convert Config object to JSON with metadata
     */
    private fun getConfigJson(): String {
        val resultObject = JsonObject()
        val valuesObject = JsonObject()
        val metadataObject = JsonObject()
        
        // Get all fields in the Config object
        val fields = Config::class.java.declaredFields
        
        for (field in fields) {
            // Skip static fields, private fields, or fields with synthetic modifiers
            if (Modifier.isStatic(field.modifiers) || field.name.contains("$")) {
                continue
            }
            
            field.isAccessible = true
            try {
                // Add the field value to values object
                when (val value = field.get(Config)) {
                    is Boolean -> valuesObject.addProperty(field.name, value)
                    is Int -> valuesObject.addProperty(field.name, value)
                    is Float -> valuesObject.addProperty(field.name, value)
                    is Double -> valuesObject.addProperty(field.name, value)
                    is String -> valuesObject.addProperty(field.name, value)
                    is Color -> {
                        val colorObj = JsonObject()
                        colorObj.addProperty("r", value.red)
                        colorObj.addProperty("g", value.green)
                        colorObj.addProperty("b", value.blue)
                        colorObj.addProperty("a", value.alpha)
                        valuesObject.add(field.name, colorObj)
                    }
                    else -> {
                        if (value != null) {
                            valuesObject.addProperty(field.name, value.toString())
                        }
                    }
                }
                
                // Extract metadata from annotations
                val propertyAnnotation = field.getAnnotation(gg.essential.vigilance.data.Property::class.java)
                if (propertyAnnotation != null) {
                    val fieldMetadata = JsonObject()
                    
                    // Get basic properties from annotation
                    fieldMetadata.addProperty("name", propertyAnnotation.name)
                    fieldMetadata.addProperty("description", propertyAnnotation.description)
                    fieldMetadata.addProperty("category", propertyAnnotation.category)
                    fieldMetadata.addProperty("subcategory", propertyAnnotation.subcategory ?: "General")
                    
                    // Get type-specific properties
                    when (propertyAnnotation.type) {
                        gg.essential.vigilance.data.PropertyType.SWITCH -> {
                            fieldMetadata.addProperty("type", "switch")
                        }
                        gg.essential.vigilance.data.PropertyType.SLIDER -> {
                            fieldMetadata.addProperty("type", "slider")
                            fieldMetadata.addProperty("min", propertyAnnotation.min)
                            fieldMetadata.addProperty("max", propertyAnnotation.max)
                        }
                        gg.essential.vigilance.data.PropertyType.DECIMAL_SLIDER -> {
                            fieldMetadata.addProperty("type", "slider")
                            fieldMetadata.addProperty("min", propertyAnnotation.minF)
                            fieldMetadata.addProperty("max", propertyAnnotation.maxF)
                            fieldMetadata.addProperty("step", Math.pow(10.0, -propertyAnnotation.decimalPlaces.toDouble()))
                        }
                        gg.essential.vigilance.data.PropertyType.PERCENT_SLIDER -> {
                            fieldMetadata.addProperty("type", "slider")
                            fieldMetadata.addProperty("min", 0)
                            fieldMetadata.addProperty("max", 1)
                            fieldMetadata.addProperty("step", 0.01)
                            fieldMetadata.addProperty("format", "percent")
                        }
                        gg.essential.vigilance.data.PropertyType.TEXT -> {
                            fieldMetadata.addProperty("type", "text")
                            fieldMetadata.addProperty("placeholder", propertyAnnotation.placeholder)
                        }
                        gg.essential.vigilance.data.PropertyType.SELECTOR -> {
                            fieldMetadata.addProperty("type", "selector")
                            val optionsArray = gson.toJsonTree(propertyAnnotation.options).asJsonArray
                            fieldMetadata.add("options", optionsArray)
                        }
                        gg.essential.vigilance.data.PropertyType.COLOR -> {
                            fieldMetadata.addProperty("type", "color")
                        }
                        else -> {
                            fieldMetadata.addProperty("type", "unknown")
                        }
                    }
                    
                    metadataObject.add(field.name, fieldMetadata)
                }
            } catch (e: Exception) {
                // Log the error for debugging
                UChat.mChat("§cError processing field ${field.name}: ${e.message}")
            }
        }
        
        // We'll just create empty objects for subcategory and category descriptions
        // since the methods don't exist in your Config class
        val subcategoryDescriptions = JsonObject()
        val categoryDescriptions = JsonObject()
        
        // Build the final result
        resultObject.add("values", valuesObject)
        resultObject.add("metadata", metadataObject)
        resultObject.add("subcategoryDescriptions", subcategoryDescriptions)
        resultObject.add("categoryDescriptions", categoryDescriptions)
        
        return gson.toJson(resultObject)
    }
    
    /**
     * Create a simplified modules list
     */
    private fun getModulesJson(): String {
        val rootJson = JsonObject()
        
        // Combat modules
        val combatModules = JsonObject()
        combatModules.addProperty("Mace Dive", Config.maceDiveEnabled)
        combatModules.addProperty("Mace D-Tap", Config.maceDTap)
        combatModules.addProperty("Hitbox", Config.HitboxEnabled)
        combatModules.addProperty("Weapon Swapper", Config.weaponSwapper)
        combatModules.addProperty("Aim Assist", Config.aimAssistEnabled)
        
        // Render modules
        val renderModules = JsonObject()
        renderModules.addProperty("Target HUD", Config.targetHudToggled)
        renderModules.addProperty("ESP", Config.espEnabled)
        renderModules.addProperty("Chams", Config.chamsEnabled)
        renderModules.addProperty("Storage ESP", Config.storageEspEnabled)
        
        // Utility modules
        val utilityModules = JsonObject()
        utilityModules.addProperty("Sprint", Config.sprint)
        utilityModules.addProperty("No Jump Delay", Config.noJumpDelay)
        utilityModules.addProperty("Full Bright", Config.fullBright)
        
        rootJson.add("Combat", combatModules)
        rootJson.add("Render", renderModules)
        rootJson.add("Utility", utilityModules)
        
        return gson.toJson(rootJson)
    }
    
    /**
     * Update config settings from JSON
     */
    private fun updateConfig(postData: String): Response {
        try {
            val jsonObject = gson.fromJson(postData, JsonObject::class.java)
            
            // Update each setting
            for (entry in jsonObject.entrySet()) {
                val propName = entry.key
                
                try {
                    val field = findField(propName)
                    
                    if (field != null) {
                        field.isAccessible = true
                        
                        when (field.type) {
                            Boolean::class.java -> field.setBoolean(Config, entry.value.asBoolean)
                            Int::class.java -> field.setInt(Config, entry.value.asInt)
                            Float::class.java -> field.setFloat(Config, entry.value.asFloat)
                            Double::class.java -> field.setDouble(Config, entry.value.asDouble)
                            String::class.java -> field.set(Config, entry.value.asString)
                            Color::class.java -> {
                                val colorObj = entry.value.asJsonObject
                                val r = colorObj.get("r").asInt
                                val g = colorObj.get("g").asInt
                                val b = colorObj.get("b").asInt
                                val a = colorObj.get("a").asInt
                                field.set(Config, Color(r, g, b, a))
                            }
                        }
                    }
                } catch (e: Exception) {
                    UChat.mChat("§cError updating $propName: ${e.message}")
                }
            }
            
            // Save config changes
            Config.markDirty()
            Config.writeData()
            
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
        } catch (e: Exception) {
            UChat.mChat("§cError updating config: ${e.message}")
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"status\":\"error\",\"message\":\"${e.message?.replace("\"", "\\\"") ?: "Unknown error"}\"}"
            )
        }
    }
    
    /**
     * Reset config to defaults
     */
    private fun resetConfig(): Response {
        try {
            Config.initialize()
            
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
        } catch (e: Exception) {
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"status\":\"error\",\"message\":\"${e.message?.replace("\"", "\\\"") ?: "Unknown error"}\"}"
            )
        }
    }
    
    /**
     * Find a field by name in the Config class
     */
    private fun findField(name: String): Field? {
        return try {
            val field = Config::class.java.getDeclaredField(name)
            field.isAccessible = true
            field
        } catch (e: NoSuchFieldException) {
            null
        }
    }
    
    /**
     * Generate basic HTML content on the fly if resource files aren't available
     */
    private fun generateIndexHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="refresh" content="0;url=/modules">
                <title>Logical Zoom Config</title>
                <style>
                    body {
                        font-family: sans-serif;
                        background-color: #1f2937;
                        color: white;
                        text-align: center;
                        padding-top: 100px;
                    }
                    p {
                        margin-top: 20px;
                    }
                    a {
                        color: #4f46e5;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                </style>
            </head>
            <body>
                <h1>Redirecting to Module Settings...</h1>
                <p>If you are not redirected automatically, <a href="/modules">click here</a>.</p>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Generate module settings HTML content on the fly
     */
    private fun generateModuleSettingsHtml(): String {
        // This returns the content of the improved-module-settings.html file
        // I'm not including it here due to its size, but it's the HTML file I provided
        // This is just a simplified placeholder
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="refresh" content="0;url=/diagnostic">
                <title>Logical Zoom Config</title>
            </head>
            <body>
                <p>Please use the static resources for the full module settings UI.</p>
                <p>Redirecting to diagnostic page...</p>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Generate diagnostic HTML content on the fly
     */
    private fun generateDiagnosticHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Logical Zoom Config - Diagnostic</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background-color: #1f2937;
                        color: #f3f4f6;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    h1 {
                        color: #4f46e5;
                    }
                    .card {
                        background-color: #111827;
                        border-radius: 8px;
                        padding: 20px;
                        margin-bottom: 20px;
                    }
                    pre {
                        background-color: #374151;
                        padding: 10px;
                        border-radius: 4px;
                        overflow-x: auto;
                        white-space: pre-wrap;
                    }
                    .module-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
                        gap: 10px;
                    }
                    button {
                        background-color: #4f46e5;
                        color: white;
                        border: none;
                        border-radius: 4px;
                        padding: 8px 16px;
                        cursor: pointer;
                    }
                    .status {
                        margin-top: 10px;
                        padding: 8px;
                        border-radius: 4px;
                    }
                    .success {
                        background-color: rgba(16, 185, 129, 0.2);
                        color: #10b981;
                    }
                    .error {
                        background-color: rgba(239, 68, 68, 0.2);
                        color: #ef4444;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Logical Zoom Config - Diagnostic</h1>
                    
                    <div class="card">
                        <h2>Configuration Viewer</h2>
                        <p>This is a simple diagnostic page to view your configuration.</p>
                        <div>
                            <button id="load-config">Load Config</button>
                            <button id="load-modules">Load Modules</button>
                        </div>
                        <div id="status" class="status"></div>
                        <pre id="output">Click a button above to load data...</pre>
                    </div>
                </div>
                
                <script>
                    document.getElementById('load-config').addEventListener('click', async () => {
                        const output = document.getElementById('output');
                        const status = document.getElementById('status');
                        
                        status.textContent = 'Loading config...';
                        status.className = 'status';
                        
                        try {
                            const response = await fetch('/api/config');
                            const data = await response.json();
                            
                            output.textContent = JSON.stringify(data, null, 2);
                            status.textContent = 'Config loaded successfully!';
                            status.className = 'status success';
                        } catch (error) {
                            output.textContent = 'Error: ' + error.message;
                            status.textContent = 'Failed to load config';
                            status.className = 'status error';
                        }
                    });
                    
                    document.getElementById('load-modules').addEventListener('click', async () => {
                        const output = document.getElementById('output');
                        const status = document.getElementById('status');
                        
                        status.textContent = 'Loading modules...';
                        status.className = 'status';
                        
                        try {
                            const response = await fetch('/api/modules');
                            const data = await response.json();
                            
                            output.textContent = JSON.stringify(data, null, 2);
                            status.textContent = 'Modules loaded successfully!';
                            status.className = 'status success';
                        } catch (error) {
                            output.textContent = 'Error: ' + error.message;
                            status.textContent = 'Failed to load modules';
                            status.className = 'status error';
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Generate logo SVG on the fly
     */
    private fun generateLogoSvg(): String {
        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
              <circle cx="50" cy="50" r="40" fill="#4f46e5" />
              <circle cx="50" cy="50" r="30" fill="none" stroke="#ffffff" stroke-width="4" />
              <circle cx="50" cy="50" r="17" fill="none" stroke="#ffffff" stroke-width="2.5" />
              <circle cx="50" cy="50" r="8" fill="#ffffff" />
            </svg>
        """.trimIndent()
    }
    
    /**
     * Generate CSS on the fly if resource file isn't available
     */
    private fun generateCss(): String {
        // For brevity, returning just a minimal set of styles
        return """
            /* Basic styles */
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                margin: 0;
                padding: 0;
                display: flex;
                background-color: #1f2937;
                color: #f3f4f6;
            }
            
            .sidebar {
                width: 260px;
                background-color: #111827;
                border-right: 1px solid rgba(255,255,255,0.1);
                height: 100vh;
                position: fixed;
                display: flex;
                flex-direction: column;
            }
            
            .main-content {
                margin-left: 260px;
                padding: 20px;
                width: calc(100% - 260px);
            }
            
            .logo {
                padding: 20px;
                display: flex;
                align-items: center;
            }
            
            .logo img {
                width: 32px;
                height: 32px;
                margin-right: 10px;
            }
            
            /* Add more styles as needed */
        """.trimIndent()
    }
    
    /**
     * Generate JavaScript on the fly if resource file isn't available
     */
    private fun generateJs(): String {
        // For brevity, returning just a basic script that shows loading message
        return """
            document.addEventListener('DOMContentLoaded', () => {
                document.getElementById('status-message').textContent = 
                    'Loading configuration... If this persists, please restart the game.';
                document.getElementById('status-message').style.display = 'block';
                
                // Try to fetch config
                fetch('/api/config')
                    .then(response => {
                        if (!response.ok) throw new Error('Failed to load configuration');
                        return response.json();
                    })
                    .then(data => {
                        document.getElementById('status-message').textContent = 
                            'Configuration loaded! Refresh the page to see all options.';
                    })
                    .catch(error => {
                        document.getElementById('status-message').textContent = 
                            'Error loading configuration: ' + error.message;
                    });
            });
        """.trimIndent()
    }
    
    companion object {
        private var instance: WebServer? = null
        private const val DEFAULT_PORT = 8080
        
        fun start() {
            if (instance == null) {
                try {
                    // Try the default port first
                    try {
                        instance = WebServer(DEFAULT_PORT)
                        UChat.chat("§aWeb configuration started at §b§nhttp://localhost:$DEFAULT_PORT§r")
                        UChat.chat("§7Module UI: §b§nhttp://localhost:$DEFAULT_PORT/modules§r")
                        UChat.chat("§7Diagnostic page: §b§nhttp://localhost:$DEFAULT_PORT/diagnostic§r")
                    } catch (e: Exception) {
                        // If default port fails, try a few alternatives
                        val alternativePorts = listOf(8081, 8082, 8090, 8000)
                        var success = false
                        
                        for (port in alternativePorts) {
                            try {
                                instance = WebServer(port)
                                UChat.chat("§aWeb configuration started at §b§nhttp://localhost:$port§r")
                                UChat.chat("§7Module UI: §b§nhttp://localhost:$port/modules§r")
                                UChat.chat("§7Diagnostic page: §b§nhttp://localhost:$port/diagnostic§r")
                                success = true
                                break
                            } catch (e2: Exception) {
                                // Continue trying other ports
                            }
                        }
                        
                        if (!success) {
                            // If all ports fail, log the original error
                            UChat.chat("§cFailed to start web server: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Throwable) {
                    // Catch any other errors
                    UChat.chat("§cCritical error starting web server: ${e.message}")
                }
            }
        }
        
        fun stop() {
            try {
                instance?.stop()
                instance = null
                UChat.chat("§cWeb server stopped")
            } catch (e: Exception) {
                UChat.chat("§cError stopping web server: ${e.message}")
            }
        }
    }
}