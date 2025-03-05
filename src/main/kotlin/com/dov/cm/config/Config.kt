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
        type = PropertyType.SWITCH,
        name = "Swap Packet",
        description = "Sends a packet to swap slots easier",
        category = "Combat",
        subcategory = "Mace Dive"
    )
    var SwapPacket: Boolean = false

    @Property(
        type = PropertyType.TEXT,
        name = "Mace Keybind",
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

    // MACE D-TAP

    @Property(
        PropertyType.SWITCH,
        name = "Mace D-Tap",
        description = "Uses 2 sets of weapons to D-Tap",
        category = "Combat",
        subcategory = "Mace D-Tap"
    )
    var maceDTap: Boolean = false

    @Property(
        PropertyType.SWITCH,
        name = "Axe Priority",
        description = "If the target is holding a shield, Ignores the first weapon and instead uses an Axe",
        category = "Combat",
        subcategory = "Mace D-Tap"
    )
    var axePriority: Boolean = true

    @Property(
        PropertyType.SELECTOR,
        name = "First weapon",
        description = "The first weapon to attack with",
        options = ["Sword", "Axe", "Mace"],
        category = "Combat",
        subcategory = "Mace D-Tap"

    )
    var maceFirstWeapon: Int = 2

    @Property(
        PropertyType.SELECTOR,
        name = "Second weapon",
        description = "The first weapon to attack with",
        options = ["Sword", "Axe", "Mace"],
        category = "Combat",
        subcategory = "Mace D-Tap"

    )
    var maceSecondWeapon: Int = 2

    @Property(
        PropertyType.SWITCH,
        name = "Switch Only",
        description = "Instead of trying to perform a D-Tap. Only switches for the property bug",
        category = "Combat",
        subcategory = "Mace D-Tap"
    )
    var switchOnly: Boolean = false

    // Hitbox settings
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
    @Property(
        type = PropertyType.SELECTOR,
        name = "Targets",
        description = "Players/All entites",
        category = "Combat",
        subcategory = "Hitbox",
        options = ["Players", "All Entites"]
    )
    var hitboxTargets: Int = 1



    @Property(
        type = PropertyType.SWITCH,
        name = "Esp",
        description = "Draws a box around players",
        category = "Render",
        subcategory = "Esp"
    )
    var espEnabled: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Render Players",
        description = "Show ESP for players",
        category = "Render",
        subcategory = "Esp"
    )
    var espRenderPlayers: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Player ESP Color",
        description = "Color for ESP rendering of players",
        category = "Render",
        subcategory = "Esp"
    )
    var espPlayerColor: Color = Color(255, 255, 255, 255)

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

    // Storage ESP Settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Storage ESP",
        description = "Enables ESP highlighting for storage blocks",
        category = "Render",
        subcategory = "Storage ESP"
    )
    var storageEspEnabled: Boolean = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Opacity",
        description = "Adjust the transparency of storage ESP highlights",
        category = "Render",
        subcategory = "Storage ESP"
    )
    var storageEspOpacity: Float = 0.4F

    @Property(
        type = PropertyType.SWITCH,
        name = "Tracers",
        description = "Draw lines from crosshair to storage blocks",
        category = "Render",
        subcategory = "Storage ESP"
    )
    var storageEspTracers: Boolean = false

    // Chest ESP settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Chests/Barrels",
        description = "Show ESP for regular chests and barrels",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var chestEspEnabled: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Chest Color",
        description = "Color for chest/barrel ESP highlights",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var chestEspColor: Color = Color(184, 134, 11, 160) // Yellowish Brown

    // Ender Chest ESP settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Ender Chests",
        description = "Show ESP for ender chests",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var enderChestEspEnabled: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Ender Chest Color",
        description = "Color for ender chest ESP highlights",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var enderChestEspColor: Color = Color(128, 0, 128, 160) // Purple

    // Trapped Chest ESP settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Trapped Chests",
        description = "Show ESP for trapped chests",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var trappedChestEspEnabled: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Trapped Chest Color",
        description = "Color for trapped chest ESP highlights",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var trappedChestEspColor: Color = Color(255, 44, 44, 163) // Red

    // Hopper ESP settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Hoppers",
        description = "Show ESP for hoppers",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var hopperEspEnabled: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Hopper Color",
        description = "Color for hopper ESP highlights",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var hopperEspColor: Color = Color(128, 128, 128, 160) // Grey

    // Furnace ESP settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Furnaces",
        description = "Show ESP for furnaces",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var furnaceEspEnabled: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Furnace Color",
        description = "Color for furnace ESP highlights",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var furnaceEspColor: Color = Color(128, 128, 128, 160) // Grey

    // Shulker ESP settings
    @Property(
        type = PropertyType.SWITCH,
        name = "Shulker Boxes",
        description = "Show ESP for shulker boxes",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var shulkerEspEnabled: Boolean = true

    @Property(
        type = PropertyType.COLOR,
        name = "Shulker Color",
        description = "Color for shulker box ESP highlights",
        category = "Render",
        subcategory = "Storage ESP Types"
    )
    var shulkerEspColor: Color = Color(245, 13, 222, 153) // Pink

    // UTILS AAAAAAAAAAAAAAA

    @Property(
        type = PropertyType.SWITCH,
        name = "Sprint",
        description = "Toggles sprint",
        category = "Utilities"
    )
    var sprint : Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "No jump delay",
        description = "Removes the delay when holding space",
        category = "Utilities"
    )
    var noJumpDelay = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Full Bright",
        description = "Just like how Sylphie brightens up my day.",
        category = "Utilities"
    )
    var fullBright : Boolean = true

    // AIM ASSIST

    @Property(
        type = PropertyType.SWITCH,
        name = "Aim Assist",
        description = "Helps aim at targets automatically",
        category = "Combat",
        subcategory = "Aim Assist"
    )
    var aimAssistEnabled: Boolean = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Mode",
        description = "Which axis to assist aiming on",
        category = "Combat",
        subcategory = "Aim Assist",
        options = ["Both", "Horizontal", "Vertical"]
    )
    var aimAssistMode: Int = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Stop on Edge",
        description = "Stops rotating as soon as it reaches the hitbox",
        category = "Combat",
        subcategory = "Aim Assist"
    )
    var stopOnEdge: Boolean = false

    @Property(
        type = PropertyType.SLIDER,
        name = "Visible Time",
        description = "How long a target must be visible before aim assist activates (ms)",
        category = "Combat",
        subcategory = "Aim Assist",
        min = 0,
        max = 500
    )
    var aimAssistVisibleTime: Int = 100

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Smoothing",
        description = "How smoothly to adjust aim (lower = faster)",
        category = "Combat",
        subcategory = "Aim Assist",
        minF = 0.1F,
        maxF = 1.0F,
        decimalPlaces = 2
    )
    var aimAssistSmoothing: Float = 0.5F

    @Property(
        type = PropertyType.SLIDER,
        name = "FOV",
        description = "Field of view in which aim assist activates (degrees)",
        category = "Combat",
        subcategory = "Aim Assist",
        min = 5,
        max = 180
    )
    var aimAssistFOV: Int = 60

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Range",
        description = "Maximum distance to targets",
        category = "Combat",
        subcategory = "Aim Assist",
        minF = 1.0F,
        maxF = 10.0F,
        decimalPlaces = 1
    )
    var aimAssistRange: Float = 5.0F

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Random",
        description = "Randomness of aim movements",
        category = "Combat",
        subcategory = "Aim Assist",
        minF = 0.0F,
        maxF = 5.0F,
        decimalPlaces = 1
    )
    var aimAssistRandom: Float = 0.5F

    @Property(
        type = PropertyType.SELECTOR,
        name = "Hitbox",
        description = "Which part of the target to aim at",
        category = "Combat",
        subcategory = "Aim Assist",
        options = ["Eye", "Center", "Bottom"]
    )
    var aimAssistHitbox: Int = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "Weapon Only",
        description = "Only activate when holding a weapon",
        category = "Combat",
        subcategory = "Aim Assist"
    )
    var aimAssistWeaponOnly: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Sticky Target",
        description = "Stick to one target until it's invalid",
        category = "Combat",
        subcategory = "Aim Assist"
    )
    var aimAssistStickyTarget: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Target Players",
        description = "Target player entities",
        category = "Combat",
        subcategory = "Aim Assist Targets"
    )
    var aimAssistTargetPlayers: Boolean = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Target Crystals",
        description = "Target end crystal entities",
        category = "Combat",
        subcategory = "Aim Assist Targets"
    )
    var aimAssistTargetCrystals: Boolean = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Target Entities",
        description = "Target all living entities",
        category = "Combat",
        subcategory = "Aim Assist Targets"
    )
    var aimAssistTargetEntities: Boolean = false


    init {
        initialize()

        // Storage ESP dependencies
        addDependency("storageEspTracers", "storageEspEnabled")

        // Storage ESP type dependencies - these should only be active if the master switch is on
        addDependency("chestEspEnabled", "storageEspEnabled")
        addDependency("enderChestEspEnabled", "storageEspEnabled")
        addDependency("trappedChestEspEnabled", "storageEspEnabled")
        addDependency("hopperEspEnabled", "storageEspEnabled")
        addDependency("furnaceEspEnabled", "storageEspEnabled")
        addDependency("shulkerEspEnabled", "storageEspEnabled")

        // Color options should only be active if both the master switch and individual type are enabled
        addDependency("chestEspColor", "chestEspEnabled")
        addDependency("enderChestEspColor", "enderChestEspEnabled")
        addDependency("trappedChestEspColor", "trappedChestEspEnabled")
        addDependency("hopperEspColor", "hopperEspEnabled")
        addDependency("furnaceEspColor", "furnaceEspEnabled")
        addDependency("shulkerEspColor", "shulkerEspEnabled")


        addDependency("aimAssistMode", "aimAssistEnabled")
        addDependency("aimAssistVisibleTime", "aimAssistEnabled")
        addDependency("aimAssistSmoothing", "aimAssistEnabled")
        addDependency("aimAssistFOV", "aimAssistEnabled")
        addDependency("aimAssistRange", "aimAssistEnabled")
        addDependency("aimAssistRandom", "aimAssistEnabled")
        addDependency("aimAssistHitbox", "aimAssistEnabled")
        addDependency("aimAssistWeaponOnly", "aimAssistEnabled")
        addDependency("aimAssistStickyTarget", "aimAssistEnabled")
        addDependency("aimAssistTargetPlayers", "aimAssistEnabled")
        addDependency("aimAssistTargetCrystals", "aimAssistEnabled")
        addDependency("aimAssistTargetEntities", "aimAssistEnabled")
        addDependency("stopOnEdge","aimAssistEnabled")


        setSubcategoryDescription(
            "Combat",
            "Aim Assist",
            "Settings for automatic aiming assistance"
        )

        setSubcategoryDescription(
            "Combat",
            "Aim Assist Targets",
            "Configure which types of entities to target"
        )

        // Set subcategory descriptions
        setSubcategoryDescription(
            "Render",
            "Storage ESP",
            "Highlight storage blocks like chests and containers"
        )

        setCategoryDescription(
            "Utilities",
            "Helpful small things"
        )


        setSubcategoryDescription(
            "Render",
            "Storage ESP Types",
            "Configure which storage blocks to highlight"
        )

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
        addDependency("hitboxTargets", "HitboxEnabled")

        // ESP Dependencies
        addDependency("espRenderPlayers", "espEnabled")
        addDependency("espPlayerColor", "espEnabled")

        // Add this subcategory description in the init block
        setSubcategoryDescription(
            "Render",
            "Esp",
            "Settings for the entity ESP module"
        )

        // Debug dependencies
        addDependency("debugMessages", "developerMode")

        // Weapon Swapper dependencies
        addDependency("firstWeapon", "weaponSwapper")
        addDependency("secondWeapon", "weaponSwapper")
        addDependency("weaponSwapBack", "weaponSwapper")

        setSubcategoryDescription(
            "Combat",
            "Mace D-Tap",
            "Hits twice with 2 weapons to D-Tap"
        )

        // Mace D-Tap dependencies
        addDependency("axePriority","maceDTap")
        addDependency("maceFirstWeapon","maceDTap")
        addDependency("maceSecondWeapon","maceDTap")
        addDependency("switchOnly","maceDTap")

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
            "Render",
            "Esp",
            "Settings for the entity ESP module"
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
            "Display information about your current target"
        )

        setSubcategoryDescription(
            "Combat",
            "Mace Dive",
            "Quick aerial attacks using mace and elytra"
        )

        setSubcategoryDescription(
            "Combat",
            "Hitbox",
            "Expand entity hitboxes for easier targeting"
        )
    }
}