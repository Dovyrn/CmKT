package com.dov.cm.web

import com.dov.cm.config.Config
import com.dov.cm.modules.UChat
import fi.iki.elonen.NanoHTTPD
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import kotlin.reflect.jvm.javaField
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

/**
 * WebServerManager - Provides a web interface for mod configuration
 * Uses direct Java reflection to reliably access Config properties and annotations
 */
class WebServerManager(private val port: Int = 8080) : NanoHTTPD(port) {

    companion object {
        private var instance: WebServerManager? = null

        fun init() {
            if (instance == null) {
                instance = WebServerManager()
                try {
                    instance?.start(SOCKET_READ_TIMEOUT, false)
                    UChat.mChat("§aWeb config server started on port 8080")
                    UChat.mChat("§aAccess at http://localhost:8080")
                } catch (e: IOException) {
                    UChat.mChat("§cFailed to start web server: ${e.message}")
                }
            }
        }

        fun shutdown() {
            instance?.stop()
            instance = null
        }
    }

    private val gson = Gson()

    // Cache for property metadata
    private val propertyMetadata = ConcurrentHashMap<String, PropertyMetadata>()

    // Module mapping (properties grouped by module)
    private val moduleMapping = mapOf(
        "aimAssist" to listOf(
            "aimAssistEnabled", "aimAssistMode", "stopOnEdge", "aimAssistVisibleTime",
            "aimAssistSmoothing", "aimAssistFOV", "aimAssistRange", "aimAssistRandom",
            "aimAssistHitbox", "aimAssistWeaponOnly", "aimAssistStickyTarget",
            "aimAssistTargetPlayers", "aimAssistTargetCrystals", "aimAssistTargetEntities"
        ),
        "backtrack" to listOf(
            "backtrackEnabled", "backtrackMinDistance", "backtrackMaxDistance",
            "backtrackMaxDelay", "backtrackMaxHurtTime", "backtrackCooldown",
            "backtrackDisableOnHit", "backtrackWeaponOnly"
        ),
        "maceDive" to listOf(
            "maceDiveEnabled", "SwapPacket", "maceDiveKey", "groundDetectionHeight",
            "attackMode", "autoEquipElytra", "autoSwapChestplate", "boostStrength", "maxHeight"
        ),
        "hitbox" to listOf("HitboxEnabled", "hitboxExpand", "hitboxTargets"),
        "weaponSwapper" to listOf("weaponSwapper", "firstWeapon", "secondWeapon", "weaponSwapBack"),
        "maceDTap" to listOf("maceDTap", "axePriority", "maceFirstWeapon", "maceSecondWeapon", "switchOnly"),
        "esp" to listOf("espEnabled", "espRenderPlayers", "espPlayerColor"),
        "chams" to listOf("chamsEnabled"),
        "storageEsp" to listOf(
            "storageEspEnabled", "storageEspOpacity", "storageEspTracers",
            "chestEspEnabled", "chestEspColor", "enderChestEspEnabled", "enderChestEspColor",
            "trappedChestEspEnabled", "trappedChestEspColor", "hopperEspEnabled", "hopperEspColor",
            "furnaceEspEnabled", "furnaceEspColor", "shulkerEspEnabled", "shulkerEspColor"
        ),
        "targetHud" to listOf(
            "targetHudToggled", "animations", "offsetX", "offsetY",
            "showHead", "background"
        ),
        "utilities" to listOf("sprint", "noJumpDelay", "fullBright"),
        "developer" to listOf("developerMode", "debugMessages")
    )

    // Module metadata for UI rendering
    private val moduleInfo = mapOf(
        "aimAssist" to ModuleInfo("Aim Assist", "Targeting enhancement", "fas fa-crosshairs", "combat"),
        "backtrack" to ModuleInfo("Backtrack", "Delay movement packets", "fas fa-history", "combat"),
        "maceDive" to ModuleInfo("Mace Dive", "Aerial dive attacks", "fas fa-bomb", "combat"),
        "hitbox" to ModuleInfo("Hitbox", "Expand entity hitboxes", "fas fa-expand", "combat"),
        "weaponSwapper" to ModuleInfo("Weapon Swapper", "Automatic weapon switching", "fas fa-exchange-alt", "combat"),
        "maceDTap" to ModuleInfo("Mace D-Tap", "Two-hit attack combo", "fas fa-bolt", "combat"),
        "esp" to ModuleInfo("ESP", "Entity wallhacks", "fas fa-eye", "render"),
        "chams" to ModuleInfo("Chams", "Entity highlighting", "fas fa-user", "render"),
        "storageEsp" to ModuleInfo("Storage ESP", "Find containers", "fas fa-box", "render"),
        "targetHud" to ModuleInfo("TargetHUD", "Combat information", "fas fa-info-circle", "render"),
        "utilities" to ModuleInfo("Utilities", "Quality of life", "fas fa-tools", "utilities"),
        "developer" to ModuleInfo("Developer", "Debug options", "fas fa-code", "developer")
    )

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        // Handle API requests
        if (uri.startsWith("/api/")) {
            return handleApiRequest(uri, method, session)
        }

        // Serve static files
        return when {
            uri == "/" || uri == "/index.html" -> serveStaticFile("module-settings.html", "text/html")
            uri == "/module-settings.css" -> serveStaticFile("module-settings.css", "text/css")
            uri == "/module-settings.js" -> serveStaticFile("module-settings.js", "application/javascript")
            else -> notFound()
        }
    }

    private fun handleApiRequest(uri: String, method: Method, session: IHTTPSession): Response {
        when {
            uri == "/api/config" && method == Method.GET -> {
                // Return the current configuration as JSON
                val configJson = buildConfigJson()
                return newFixedLengthResponse(Response.Status.OK, "application/json", configJson)
            }
            uri == "/api/config" && method == Method.POST -> {
                // Update the configuration with the provided values
                val files = HashMap<String, String>()
                try {
                    session.parseBody(files)
                    val postData = files["postData"] ?: return badRequest("Missing post data")

                    // Parse the JSON and update the config
                    updateConfig(postData)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", """{"success":true}""")
                } catch (e: Exception) {
                    UChat.mChat("§cError updating config: ${e.message}")
                    return badRequest("Failed to update config: ${e.message}")
                }
            }
            uri == "/api/metadata" && method == Method.GET -> {
                // Return module and property metadata
                val metadataJson = buildMetadataJson()
                return newFixedLengthResponse(Response.Status.OK, "application/json", metadataJson)
            }
            else -> return notFound()
        }
    }

    private fun buildConfigJson(): String {
        val result = JsonObject()

        // Add all property values
        for ((module, properties) in moduleMapping) {
            val moduleObj = JsonObject()

            properties.forEach { prop ->
                try {
                    val value = getConfigValue(prop)

                    when (value) {
                        is Boolean -> moduleObj.addProperty(prop, value)
                        is Int -> moduleObj.addProperty(prop, value)
                        is Float -> moduleObj.addProperty(prop, value)
                        is String -> moduleObj.addProperty(prop, value)
                        is Color -> {
                            val colorObj = JsonObject()
                            colorObj.addProperty("r", value.red)
                            colorObj.addProperty("g", value.green)
                            colorObj.addProperty("b", value.blue)
                            colorObj.addProperty("a", value.alpha)
                            moduleObj.add(prop, colorObj)
                        }
                        else -> moduleObj.addProperty(prop, value.toString())
                    }
                } catch (e: Exception) {
                    UChat.mChat("§cError getting config value for $prop: ${e.message}")
                }
            }

            result.add(module, moduleObj)
        }

        return gson.toJson(result)
    }

    private fun buildMetadataJson(): String {
        val result = JsonObject()

        // Add module metadata
        val modulesObj = JsonObject()
        moduleInfo.forEach { (id, info) ->
            val moduleObj = JsonObject()
            moduleObj.addProperty("name", info.name)
            moduleObj.addProperty("description", info.description)
            moduleObj.addProperty("icon", info.icon)
            moduleObj.addProperty("category", info.category)
            modulesObj.add(id, moduleObj)
        }
        result.add("modules", modulesObj)

        // Add property metadata
        val propertiesObj = JsonObject()
        for ((module, props) in moduleMapping) {
            val moduleProps = JsonObject()

            props.forEach { prop ->
                try {
                    val metadata = getPropertyMetadata(prop)
                    if (metadata != null) {
                        val propObj = JsonObject()
                        propObj.addProperty("type", metadata.type.name)
                        propObj.addProperty("name", metadata.name)
                        propObj.addProperty("description", metadata.description)

                        if (metadata.options.isNotEmpty()) {
                            val optionsArray = gson.toJsonTree(metadata.options).asJsonArray
                            propObj.add("options", optionsArray)
                        }

                        if (metadata.min != null) propObj.addProperty("min", metadata.min)
                        if (metadata.max != null) propObj.addProperty("max", metadata.max)
                        if (metadata.minF != null) propObj.addProperty("minF", metadata.minF)
                        if (metadata.maxF != null) propObj.addProperty("maxF", metadata.maxF)

                        moduleProps.add(prop, propObj)
                    }
                } catch (e: Exception) {
                    UChat.mChat("§cError getting metadata for $prop: ${e.message}")
                }
            }

            propertiesObj.add(module, moduleProps)
        }
        result.add("properties", propertiesObj)

        return gson.toJson(result)
    }

    private fun updateConfig(jsonData: String) {
        try {
            val json = JsonParser.parseString(jsonData).asJsonObject

            // Process each module
            for ((module, properties) in moduleMapping) {
                if (json.has(module)) {
                    val moduleObj = json.getAsJsonObject(module)

                    // Process each property in the module
                    properties.forEach { prop ->
                        if (moduleObj.has(prop)) {
                            try {
                                val metadata = getPropertyMetadata(prop)

                                when (metadata?.type) {
                                    PropertyType.SWITCH -> {
                                        val value = moduleObj.get(prop).asBoolean
                                        setConfigValue(prop, value)
                                    }
                                    PropertyType.SLIDER -> {
                                        val value = moduleObj.get(prop).asInt
                                        setConfigValue(prop, value)
                                    }
                                    PropertyType.DECIMAL_SLIDER -> {
                                        val value = moduleObj.get(prop).asFloat
                                        setConfigValue(prop, value)
                                    }
                                    PropertyType.PERCENT_SLIDER -> {
                                        val value = moduleObj.get(prop).asFloat
                                        setConfigValue(prop, value)
                                    }
                                    PropertyType.SELECTOR -> {
                                        val value = moduleObj.get(prop).asInt
                                        setConfigValue(prop, value)
                                    }
                                    PropertyType.TEXT -> {
                                        val value = moduleObj.get(prop).asString
                                        setConfigValue(prop, value)
                                    }
                                    PropertyType.COLOR -> {
                                        val colorObj = moduleObj.getAsJsonObject(prop)
                                        val r = colorObj.get("r").asInt
                                        val g = colorObj.get("g").asInt
                                        val b = colorObj.get("b").asInt
                                        val a = colorObj.get("a").asInt
                                        setConfigValue(prop, Color(r, g, b, a))
                                    }
                                    else -> {
                                        // Handle other property types as needed
                                        UChat.mChat("§eUnsupported property type for $prop")
                                    }
                                }
                            } catch (e: Exception) {
                                UChat.mChat("§cError updating $prop: ${e.message}")
                            }
                        }
                    }
                }
            }

            // Save the updated config
            Config.writeData()
            UChat.mChat("§aConfiguration saved successfully")
        } catch (e: Exception) {
            UChat.mChat("§cError parsing JSON: ${e.message}")
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getConfigValue(property: String): Any? {
        try {
            val prop = Config::class.objectInstance!!::class.declaredMemberProperties.find { it.name == property }
            return prop?.getter?.call(Config::class.objectInstance)
        } catch (e: Exception) {
            UChat.mChat("§cError getting config value for $property: ${e.message}")
            return null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setConfigValue(property: String, value: Any) {
        try {
            val prop = Config::class.objectInstance!!::class.declaredMemberProperties.find { it.name == property } as? KMutableProperty<*>
            prop?.setter?.call(Config::class.objectInstance, value)
        } catch (e: Exception) {
            UChat.mChat("§cError setting config value for $property: ${e.message}")
        }
    }

    /**
     * Detects the appropriate PropertyType based on the actual value type in Config
     */
    private fun detectPropertyTypeFromValue(property: String): PropertyType {
        try {
            val value = getConfigValue(property)

            return when (value) {
                is Boolean -> PropertyType.SWITCH
                is Int -> PropertyType.SLIDER
                is Float -> if (value <= 1.0f) PropertyType.PERCENT_SLIDER else PropertyType.DECIMAL_SLIDER
                is String -> PropertyType.TEXT
                is Color -> PropertyType.COLOR
                else -> PropertyType.SWITCH  // Default to SWITCH
            }
        } catch (e: Exception) {
            // If we can't get the value, fall back to name-based detection
            return when {
                // Boolean properties (usually toggles)
                property.startsWith("is") ||
                        property.endsWith("Enabled") ||
                        property.endsWith("Toggled") ||
                        property == "sprint" ||
                        property == "noJumpDelay" ||
                        property == "fullBright" ||
                        property == "developerMode" ||
                        property == "debugMessages" -> PropertyType.SWITCH

                // Integer ranges
                property.contains("Delay") ||
                        property.contains("Time") ||
                        property.contains("Size") ||
                        property.contains("Height") -> PropertyType.SLIDER

                // Float ranges
                property.contains("Speed") ||
                        property.contains("Strength") ||
                        property.contains("Expand") -> PropertyType.DECIMAL_SLIDER

                // Percentage values
                property == "background" ||
                        property.contains("Opacity") -> PropertyType.PERCENT_SLIDER

                // Text inputs
                property.endsWith("Key") -> PropertyType.TEXT

                // Color values
                property.endsWith("Color") -> PropertyType.COLOR

                // Selection options
                property.startsWith("mode") ||
                        property.endsWith("Mode") ||
                        property.endsWith("Type") ||
                        property.startsWith("first") ||
                        property.startsWith("second") -> PropertyType.SELECTOR

                // Default to switch
                else -> PropertyType.SWITCH
            }
        }
    }

    private fun getPropertyMetadata(property: String): PropertyMetadata? {
        // Check cache first
        if (propertyMetadata.containsKey(property)) {
            return propertyMetadata[property]
        }

        try {
            // Get property using the more direct approach with Java reflection
            val propertyMember = Config::class.objectInstance!!::class.declaredMemberProperties.find { it.name == property }

            if (propertyMember == null) {
                UChat.mChat("§cProperty not found: $property")
                return createDefaultMetadata(property)
            }

            // Get annotation directly from the Java field
            val annotation = propertyMember.javaField?.getAnnotation(Property::class.java)

            // If no annotation is found, create default metadata
            if (annotation == null) {
                UChat.mChat("§6Property $property has no annotation, using default metadata")
                return createDefaultMetadata(property)
            }

            // Create metadata from annotation
            val metadata = PropertyMetadata(
                type = annotation.type,
                name = annotation.name,
                description = annotation.description,
                options = annotation.options.toList(),
                min = if (annotation.type == PropertyType.SLIDER) annotation.min else null,
                max = if (annotation.type == PropertyType.SLIDER) annotation.max else null,
                minF = if (annotation.type == PropertyType.DECIMAL_SLIDER) annotation.minF else null,
                maxF = if (annotation.type == PropertyType.DECIMAL_SLIDER) annotation.maxF else null
            )

            // Cache the metadata
            propertyMetadata[property] = metadata
            return metadata
        } catch (e: Exception) {
            UChat.mChat("§cError getting metadata for property $property: ${e.message}")
            return createDefaultMetadata(property)
        }
    }

    /**
     * Create default metadata for properties with missing annotations
     */
    private fun createDefaultMetadata(property: String): PropertyMetadata {
        // Determine property type based on actual value when possible
        val type = detectPropertyTypeFromValue(property)

        // Generate name and description from property name
        val name = property.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { it.uppercase() }

        val description = "Configure $name"

        // Set appropriate min/max values based on property type
        val min: Int? = when {
            type == PropertyType.SLIDER && property.contains("Delay") -> 0
            type == PropertyType.SLIDER && property.contains("FOV") -> 5
            type == PropertyType.SLIDER && property.contains("Height") -> 1
            type == PropertyType.SLIDER -> 0
            else -> null
        }

        val max: Int? = when {
            type == PropertyType.SLIDER && property.contains("Delay") -> 1000
            type == PropertyType.SLIDER && property.contains("FOV") -> 180
            type == PropertyType.SLIDER && property.contains("Height") -> 100
            type == PropertyType.SLIDER -> 100
            else -> null
        }

        val minF: Float? = when {
            type == PropertyType.DECIMAL_SLIDER && property.contains("Range") -> 1.0f
            type == PropertyType.DECIMAL_SLIDER && property.contains("Expand") -> 0.0f
            type == PropertyType.DECIMAL_SLIDER && property.contains("Speed") -> 0.1f
            type == PropertyType.DECIMAL_SLIDER -> 0.0f
            else -> null
        }

        val maxF: Float? = when {
            type == PropertyType.DECIMAL_SLIDER && property.contains("Range") -> 10.0f
            type == PropertyType.DECIMAL_SLIDER && property.contains("Expand") -> 1.0f
            type == PropertyType.DECIMAL_SLIDER && property.contains("Speed") -> 5.0f
            type == PropertyType.DECIMAL_SLIDER -> 1.0f
            else -> null
        }

        // Special case for specific selectors we know about
        val options = when {
            property == "hitboxTargets" -> listOf("Players", "All Entities")
            property == "aimAssistMode" -> listOf("Both", "Horizontal", "Vertical")
            property == "aimAssistHitbox" -> listOf("Eye", "Center", "Bottom")
            property == "attackMode" -> listOf("None", "TriggerBot", "Silent")
            property == "firstWeapon" || property == "secondWeapon" ||
                    property == "maceFirstWeapon" || property == "maceSecondWeapon" -> listOf("Sword", "Axe", "Mace")
            type == PropertyType.SELECTOR -> listOf("Option 1", "Option 2")
            else -> emptyList()
        }

        // Create and cache default metadata
        val metadata = PropertyMetadata(
            type = type,
            name = name,
            description = description,
            options = options,
            min = min,
            max = max,
            minF = minF,
            maxF = maxF
        )

        propertyMetadata[property] = metadata
        return metadata
    }

    /**
     * Serve a static file from resources
     */
    private fun serveStaticFile(fileName: String, mimeType: String): Response {
        try {
            // Get input stream from resources
            val inputStream = javaClass.classLoader.getResourceAsStream("web/$fileName")

            if (inputStream != null) {
                return newChunkedResponse(Response.Status.OK, mimeType, inputStream)
            } else {
                UChat.mChat("§cResource not found: web/$fileName")
                return notFound()
            }
        } catch (e: Exception) {
            UChat.mChat("§cError serving static file $fileName: ${e.message}")
            return notFound()
        }
    }

    private fun notFound(): Response {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }

    private fun badRequest(message: String): Response {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, message)
    }

    // Data class for module info
    data class ModuleInfo(
        val name: String,
        val description: String,
        val icon: String,
        val category: String
    )

    // Data class for property metadata
    data class PropertyMetadata(
        val type: PropertyType,
        val name: String,
        val description: String,
        val options: List<String> = emptyList(),
        val min: Int? = null,
        val max: Int? = null,
        val minF: Float? = null,
        val maxF: Float? = null
    )
}