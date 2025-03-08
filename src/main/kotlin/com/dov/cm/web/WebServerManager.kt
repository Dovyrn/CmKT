package com.dov.cm.web

import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW
import java.awt.Desktop
import java.net.URI

/**
 * Manages the web server lifecycle, starting it when joining a world
 * and stopping it when leaving or closing the game
 */
object WebServerManager {
    private var isServerStarted = false
    private var openedBrowser = false
    private val WEB_PORT = 8080
    private const val WEB_URL = "http://localhost:8080"

    fun init() {
        // Start the server when a world is loaded and stop when leaving
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val isInGame = client.world != null && client.player != null

            // Start server when player joins a world
            if (isInGame && !isServerStarted) {
                WebServer.start()
                isServerStarted = true
                openedBrowser = false
                UChat.chat("§aWeb configuration launched at §b§nhttp://localhost:$WEB_PORT§r §a(Press §bCTRL+B§a to open)")
            }
            // Stop server when player leaves a world
            else if (!isInGame && isServerStarted) {
                WebServer.stop()
                isServerStarted = false
            }

            // Check for Ctrl+B shortcut to open browser
            if (isServerStarted && !openedBrowser) {
                if (GLFW.glfwGetKey(client.window.handle, GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS &&
                    GLFW.glfwGetKey(client.window.handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {

                    openBrowser()
                    openedBrowser = true

                    // Debounce the key press
                    Thread {
                        Thread.sleep(500)
                        openedBrowser = false
                    }.start()
                }
            }
        }

        // Make sure to stop the server when the game exits
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            if (isServerStarted) {
                WebServer.stop()
                isServerStarted = false
            }
        }
    }

    /**
     * Try to open the web interface in the default browser
     */
    private fun openBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(WEB_URL))
                UChat.chat("§aOpened web configuration in browser")
            } else {
                UChat.chat("§cCouldn't open browser automatically. Please go to §b$WEB_URL§c manually")
            }
        } catch (e: Exception) {
            UChat.chat("§cError opening browser: ${e.message}. Please go to §b$WEB_URL§c manually")
        }
    }
}