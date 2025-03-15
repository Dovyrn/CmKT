package com.dov.cm.managers


import com.dov.cm.event.BotEvent
import com.dov.cm.event.EventBus
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import java.util.ArrayList

class AntiBotManager (eventBus : EventBus)  {
    private val bots = ArrayList<PlayerEntity>()

    init {
        eventBus.registerListener(this)
    }

    fun isNotBot(entity: Entity): Boolean {
        if (entity == getMc().player) {
            return false
        }
        if (entity !is PlayerEntity || !BotEvent().invoke()) {
            return true
        }
        return bots.contains(entity) && entity != getMc().player && entity.isAlive
    }

    fun addBot(playerEntity: PlayerEntity) {
        bots.add(playerEntity)
    }

    fun getBots(): ArrayList<PlayerEntity> {
        return bots
    }

    fun getMc(): MinecraftClient {
        return MinecraftClient.getInstance()
    }
}