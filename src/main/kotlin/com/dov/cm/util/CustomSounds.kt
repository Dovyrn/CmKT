package com.logicalgeekboy.logical_zoom

import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.registry.Registry
import net.minecraft.registry.Registries

object CustomSounds {
    // Define mod ID
    private const val MOD_ID = "logical_zoom"

    // Custom sound events
    val LOVE_IS_WAR: SoundEvent = registerSound("love_is_war")

    // Helper method to register sounds
    private fun registerSound(id: String): SoundEvent {
        val identifier = Identifier.of(MOD_ID, id)
        println("Registering sound event: $identifier")
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier))
    }

    // Initialization method to ensure sounds are registered
    fun initialize() {
        println("Registering $MOD_ID Sounds")
        // Trigger static initialization of LOVE_IS_WAR
        val soundEvent = LOVE_IS_WAR
        println("Love is War Sound Event: $soundEvent")
    }
}