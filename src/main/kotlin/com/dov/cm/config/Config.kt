package com.dov.cm.config

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.awt.Color
import java.io.File

/**
 * Configuration for the mod features
 */
object Config : Vigilant(File("./config/CmKt/config.toml")) {

    // TargetHUD Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "TargetHUD",
        description = "Displays information about your current target",
        category = "Render",
        subcategory = "TargetHUD"
    )
    var targetHudToggled: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Animations",
        description = "Enable smooth animations for the TargetHUD",
        category = "Render",
        subcategory = "TargetHUD"
    )
    var animations: Boolean = true

    @Property(
        type = PropertyType.SLIDER,
        name = "X Offset",
        description = "Horizontal position adjustment for the TargetHUD",
        category = "Render",
        subcategory = "TargetHUD",
        min = -500,
        max = 500
    )
    var offsetX: Int = 0

    @Property(
        type = PropertyType.SLIDER,
        name = "Y Offset",
        description = "Vertical position adjustment for the TargetHUD",
        category = "Render",
        subcategory = "TargetHUD",
        min = -500,
        max = 500
    )
    var offsetY: Int = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Player Head",
        description = "Display the target's head in the HUD",
        category = "Render",
        subcategory = "TargetHUD"
    )
    var showHead: Boolean = true

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Background Opacity",
        description = "Adjust the transparency of the TargetHUD background",
        category = "Render",
        subcategory = "TargetHUD"
    )
    var background: Float = 0.5F

    // Mace Dive Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Mace Dive",
        description = "Enables the Mace Dive module for quick aerial attacks",
        category = "Combat",
        subcategory = "Mace Dive"
    )
    var maceDiveEnabled: Boolean = false

    @Property(
        type = PropertyType.TEXT,
        name = "Keybind",
        description = "Key to activate Mace Dive (single character)",
        category = "Combat",
        subcategory = "Mace Dive",
        placeholder = "Enter a key"
    )
    var maceDiveKey: String = "V"

    @Property(
        type = PropertyType.SLIDER,
        name = "Ground Detection Height",
        description = "Distance from ground to trigger chestplate swap and attack (in blocks)",
        category = "Combat",
        subcategory = "Mace Dive",
        min = 1,
        max = 25
    )
    var groundDetectionHeight: Int = 3

    @Property(
        type = PropertyType.SELECTOR,
        name = "Attack Mode",
        description = "How to attack with the mace when diving",
        category = "Combat",
        subcategory = "Mace Dive",
        options = ["None", "TriggerBot", "Silent"]
    )
    var attackMode: Int = 1

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto-Equip Elytra",
        description = "Automatically equip elytra from hotbar when activating",
        category = "Combat",
        subcategory = "Mace Dive"
    )
    var autoEquipElytra: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto-Swap to Chestplate",
        description = "Automatically swap back to chestplate when diving/landing",
        category = "Combat",
        subcategory = "Mace Dive"
    )
    var autoSwapChestplate: Boolean = true

    @Property(
        type = PropertyType.SLIDER,
        name = "Boost Strength",
        description = "How many fireworks to use.",
        category = "Combat",
        subcategory = "Mace Dive",
        min = 1,
        max = 5
    )
    var boostStrength: Int = 3

    @Property(
        type = PropertyType.SLIDER,
        name = "Max Height",
        description = "Maximum height to reach before beginning dive (blocks above launch)",
        category = "Combat",
        subcategory = "Mace Dive",
        min = 10,
        max = 100
    )
    var maxHeight: Int = 50
    @Property(
        type = PropertyType.SWITCH,
        name = "Hitbox",
        description = "Expands hitbox of players",
        category = "Combat",
        subcategory = "Hitbox"
    )
    var HitboxEnabled: Boolean = false
    // Hitbox settings
    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Expand",
        decimalPlaces = 1,
        description = "How much to expand the Hitbox",
        category = "Combat",
        subcategory = "Hitbox",
        minF = 0F,
        maxF = 1F
    )
    var hitboxExpand: Float = 0.1F
    @Property(
        type = PropertyType.SWITCH,
        name = "Players",
        description = "Include players",
        category = "Combat",
        subcategory = "Hitbox"
    )
    var hitboxPlayers: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Mobs",
        description = "Include Mobs",
        category = "Combat",
        subcategory = "Hitbox"
    )
    var hitboxMobs: Boolean = true

    // Enhanced ESP Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "ESP",
        description = "Enables entity ESP (see entities through walls)",
        category = "Combat",
        subcategory = "ESP"
    )
    var espEnabled: Boolean = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "ESP Type",
        description = "Choose ESP rendering style",
        category = "Combat",
        subcategory = "ESP",
        options = ["Box", "Outline", "2D", "Health Bar", "Shaded", "Ring"]
    )
    var espType: Int = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Render Players",
        description = "Show ESP for player entities",
        category = "Combat",
        subcategory = "ESP"
    )
    var espRenderPlayers: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Render Mobs",
        description = "Show ESP for mob entities",
        category = "Combat",
        subcategory = "ESP"
    )
    var espRenderMobs: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Render Items",
        description = "Show ESP for item entities",
        category = "Combat",
        subcategory = "ESP"
    )
    var espRenderItems: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Render Invisible",
        description = "Show ESP for invisible entities",
        category = "Combat",
        subcategory = "ESP"
    )
    var espRenderInvisible: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Render Self",
        description = "Show ESP for your own player",
        category = "Combat",
        subcategory = "ESP"
    )
    var espRenderSelf: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Rainbow Color",
        description = "Use rainbow colors for ESP",
        category = "Combat",
        subcategory = "ESP"
    )
    var espRainbowColor: Boolean = false

    @Property(
        type = PropertyType.SLIDER,
        name = "Render Distance",
        description = "Maximum distance to render ESP (in blocks)",
        category = "Combat",
        subcategory = "ESP",
        min = 10,
        max = 128
    )
    var espRenderDistance: Int = 64

    @Property(
        type = PropertyType.SLIDER,
        name = "Line Width",
        description = "Width of ESP outline/lines",
        category = "Combat",
        subcategory = "ESP",
        min = 1,
        max = 5
    )
    var espLineWidth: Int = 2

    @Property(
        type = PropertyType.COLOR,
        name = "Player ESP Color",
        description = "Color for ESP rendering of players",
        category = "Combat",
        subcategory = "ESP Colors"
    )
    var espPlayerColor: Color = Color(0, 255, 0, 100)

    @Property(
        type = PropertyType.COLOR,
        name = "Mob ESP Color",
        description = "Color for ESP rendering of mobs",
        category = "Combat",
        subcategory = "ESP Colors"
    )
    var espMobColor: Color = Color(255, 0, 0, 100)

    @Property(
        type = PropertyType.COLOR,
        name = "Item ESP Color",
        description = "Color for ESP rendering of items",
        category = "Combat",
        subcategory = "ESP Colors"
    )
    var espItemColor: Color = Color(0, 0, 255, 100)




    // Chams Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Chams",
        description = "See players through walls",
        category = "Render"
    )
    var chamsEnabled: Boolean = false

    // Developer Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Developer Mode",
        description = "Enable developer mode for testing features",
        category = "Developer",
        subcategory = "General"
    )
    var developerMode: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Debug Messages",
        description = "Show debug messages in chat",
        category = "Developer",
        subcategory = "General"
    )
    var debugMessages: Boolean = false

    // Sword Swapper
    @Property(
        type = PropertyType.SWITCH,
        name = "Weapon Swapper",
        description = "Swaps weapon for enchantments",
        category = "Combat",
        subcategory = "Weapon Swapper"
    )
    var weaponSwapper :Boolean = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "First Weapon",
        options = ["Sword", "Axe", "Mace"],
        category = "Combat",
        subcategory = "Weapon Swapper"
    )
    var firstWeapon = 0

    @Property(
        type = PropertyType.SELECTOR,
        name = "Second Weapon",
        options = ["Sword", "Axe", "Mace"],
        category = "Combat",
        subcategory = "Weapon Swapper"
    )
    var secondWeapon = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Swap Back",
        description = "Automatically swap back to first weapon after attack",
        category = "Combat",
        subcategory = "Weapon Swapper"
    )
    var weaponSwapBack: Boolean = true

    init {
        initialize()

        // TargetHUD dependencies
        addDependency("animations", "targetHudToggled")
        addDependency("offsetX", "targetHudToggled")
        addDependency("offsetY", "targetHudToggled")
        addDependency("showHead", "targetHudToggled")
        addDependency("background", "targetHudToggled")

        // Mace Dive dependencies
        addDependency("maceDiveKey", "maceDiveEnabled")
        addDependency("groundDetectionHeight", "maceDiveEnabled")
        addDependency("attackMode", "maceDiveEnabled")
        addDependency("autoEquipElytra", "maceDiveEnabled")
        addDependency("autoSwapChestplate", "maceDiveEnabled")
        addDependency("boostStrength", "maceDiveEnabled")
        addDependency("maxHeight", "maceDiveEnabled")

        // Hitbox dependencies
        addDependency("hitboxExpand", "HitboxEnabled")
        addDependency("hitboxPlayers", "HitboxEnabled")
        addDependency("hitboxMobs", "HitboxEnabled")

        // ESP Dependencies
        addDependency("espType", "espEnabled")
        addDependency("espRenderPlayers", "espEnabled")
        addDependency("espRenderMobs", "espEnabled")
        addDependency("espRenderItems", "espEnabled")
        addDependency("espRenderInvisible", "espEnabled")
        addDependency("espRenderSelf", "espEnabled")
        addDependency("espRainbowColor", "espEnabled")
        addDependency("espRenderDistance", "espEnabled")
        addDependency("espLineWidth", "espEnabled")
        addDependency("espPlayerColor", "espEnabled")
        addDependency("espMobColor", "espEnabled")
        addDependency("espItemColor", "espEnabled")


// Add this subcategory description in the init block
        setSubcategoryDescription(
            "Combat",
            "ESP",
            "Settings for the entity ESP module"
        )
        // Debug dependecies
        addDependency("debugMessages", "developerMode")

        // Weapon Swapper dependencies
        addDependency("firstWeapon", "weaponSwapper")
        addDependency("secondWeapon", "weaponSwapper")
        addDependency("weaponSwapBack", "weaponSwapper")

        setSubcategoryDescription(
            "Combat",
            "Weapon Swapper",
            "Automatically swap between weapons for maximum damage"
        )

        setCategoryDescription(
            "Developer",
            "Stuff for debugging the mod"
        )
        setSubcategoryDescription(
            "Combat",
            "ESP",
            "See entities through walls with customizable boxes and tracers"
        )
        setCategoryDescription(
            "Render",
            "Rendering stuff"
        )

        setCategoryDescription(
            "Combat",
            "Combat-related features and settings"
        )

        setSubcategoryDescription(
            "Render",
            "TargetHUD",
            ""
        )

        setSubcategoryDescription(
            "Combat",
            "Mace Dive",
            ""
        )
        setSubcategoryDescription(
            "Combat",
            "Hitbox",
            ""
        )
    }
}