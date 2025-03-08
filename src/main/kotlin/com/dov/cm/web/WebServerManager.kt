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
    private var lastStartAttempt = 0L
    private val WEB_PORT = 8080
    private const val START_RETRY_DELAY = 10000L // 10 seconds between retry attempts
    private const val WEB_URL = "http://localhost:8080"

    fun init() {
        try {
            // Check if NanoHTTPD is available
            val nanoClass = Class.forName("fi.iki.elonen.NanoHTTPD")
            UChat.chat("§aNanoHTTPD class loaded successfully: ${nanoClass.name}")
        } catch (e: ClassNotFoundException) {
            UChat.chat("§cError loading NanoHTTPD class: ${e.message}")
            UChat.chat("§cWeb configuration will not be available")
            return
        }

        // Start the server when a world is loaded and stop when leaving
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            try {
                val isInGame = client.world != null && client.player != null
                val currentTime = System.currentTimeMillis()

                // Start server when player joins a world
                if (isInGame && !isServerStarted && currentTime - lastStartAttempt > START_RETRY_DELAY) {
                    lastStartAttempt = currentTime

                    try {
                        WebServer.start()
                        isServerStarted = true
                        openedBrowser = false
                        UChat.chat("§aWeb configuration launched at §b§nhttp://localhost:$WEB_PORT§r §a(Press §bCTRL+B§a to open)")
                    } catch (e: Exception) {
                        UChat.chat("§cFailed to start web server: ${e.message}")
                        isServerStarted = false
                    }
                }
                // Stop server when player leaves a world
                else if (!isInGame && isServerStarted) {
                    WebServer.stop()
                    isServerStarted = false
                }

                // Check for Ctrl+B shortcut to open browser
                if (isServerStarted && !openedBrowser) {
                    val controlPressed = GLFW.glfwGetKey(client.window.handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                            GLFW.glfwGetKey(client.window.handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS

                    if (controlPressed &&
                        GLFW.glfwGetKey(client.window.handle, GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS) {

                        openBrowser()
                        openedBrowser = true

                        // Debounce the key press
                        Thread {
                            try {
                                Thread.sleep(500)
                                openedBrowser = false
                            } catch (e: Exception) {
                                // Ignore interruption
                            }
                        }.start()
                    }
                }
            } catch (e: Exception) {
                UChat.chat("§cError in WebServerManager tick: ${e.message}")
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
                Desktop.getDesktop().browse(URI(WEB_URL + "/modules"))
                UChat.chat("§aOpened web configuration in browser")
            } else {
                UChat.chat("§cCouldn't open browser automatically. Please go to §b$WEB_URL/modules§c manually")
            }
        } catch (e: Exception) {
            UChat.chat("§cError opening browser: ${e.message}. Please go to §b$WEB_URL/modules§c manually")
        }
    }
}