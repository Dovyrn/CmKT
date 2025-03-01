package com.dov.cm.config

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import java.io.File

/**
 * Configuration for the mod features
 */
object Config : Vigilant(File("./config/config.toml")) {

    // TargetHUD Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "TargetHUD",
        description = "Displays information about your current target",
        category = "Combat",
        subcategory = "TargetHUD"
    )
    var targetHudToggled: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Animations",
        description = "Enable smooth animations for the TargetHUD",
        category = "Combat",
        subcategory = "TargetHUD"
    )
    var animations: Boolean = true

    @Property(
        type = PropertyType.SLIDER,
        name = "X Offset",
        description = "Horizontal position adjustment for the TargetHUD",
        category = "Combat",
        subcategory = "TargetHUD",
        min = -500,
        max = 500
    )
    var offsetX: Int = 0

    @Property(
        type = PropertyType.SLIDER,
        name = "Y Offset",
        description = "Vertical position adjustment for the TargetHUD",
        category = "Combat",
        subcategory = "TargetHUD",
        min = -500,
        max = 500
    )
    var offsetY: Int = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Player Head",
        description = "Display the target's head in the HUD",
        category = "Combat",
        subcategory = "TargetHUD"
    )
    var showHead: Boolean = true

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Background Opacity",
        description = "Adjust the transparency of the TargetHUD background",
        category = "Combat",
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

        setCategoryDescription(
            "Combat",
            "Combat-related features and settings"
        )

        setSubcategoryDescription(
            "Combat",
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