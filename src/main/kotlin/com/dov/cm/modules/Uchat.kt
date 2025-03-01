package com.dov.cm.modules

import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object UChat {
    fun say(message: String) {
        MinecraftClient.getInstance().player?.networkHandler?.sendChatMessage(message)
    }

    fun chat(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(Text.of(message), false)
    }
    fun mChat(message: String) {
        MinecraftClient.getInstance().player?.sendMessage(Text.of("§7[§cM§6y§ea§au§7]§f $message"), false)
    }
}
