package com.dov.cm.util

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

object ModSounds {

val LOVE_IS_WAR: SoundEvent = registerSoundEvent("love_is_war")

private fun registerSoundEvent(name: String): SoundEvent {
    val id = Identifier.of("logical_zoom", name)
    return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id))
}

fun registerSounds() {
    println("Registering Mod Sounds for logical_zoom")
}
}