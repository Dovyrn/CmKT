package com.dov.cm.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color

class Render3DEngine {
    companion object {
        private val mc = MinecraftClient.getInstance()

        fun drawFilledBox(matrices: MatrixStack, box: Box, color: Color) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

            val matrix = matrices.peek().positionMatrix

            // Draw box vertices using the pre-adjusted box
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            // ... (rest of the box drawing logic)

            BufferRenderer.drawWithGlobalProgram(buffer.end())

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        fun drawBoxOutline(box: Box, color: Color, lineWidth: Float) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.lineWidth(lineWidth)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

            val matrix = MatrixStack().peek().positionMatrix

            // Bottom rectangle
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)

            // Top rectangle
            buffer.vertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).color(color.rgb)

            // Vertical lines
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).color(color.rgb)

            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(color.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat()).color(color.rgb)

            BufferRenderer.drawWithGlobalProgram(buffer.end())

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        fun drawFilledFadeBox(
            matrices: MatrixStack,
            box: Box,
            startColor: Color,
            endColor: Color
        ) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

            val matrix = matrices.peek().positionMatrix

            // Bottom face
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(startColor.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat()).color(startColor.rgb)
            buffer.vertex(matrix, box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(endColor.rgb)
            buffer.vertex(matrix, box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat()).color(endColor.rgb)

            // Similar logic for other faces would be implemented here...

            BufferRenderer.drawWithGlobalProgram(buffer.end())

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        fun drawCircle3D(
            matrices: MatrixStack,
            entity: Entity,
            radius: Float,
            color: Int,
            points: Int
        ) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val tessellator = Tessellator.getInstance()
            val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)

            val matrix = matrices.peek().positionMatrix

            val x = entity.x
            val y = entity.y
            val z = entity.z

            for (i in 0..points) {
                val angle = i * (2 * Math.PI / points)
                val pointX = x + radius * Math.cos(angle)
                val pointZ = z + radius * Math.sin(angle)

                buffer.vertex(matrix, pointX.toFloat(), y.toFloat(), pointZ.toFloat()).color(color)
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end())

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        fun worldSpaceToScreenSpace(pos: Vec3d): Vec3d? {
            val mc = MinecraftClient.getInstance()
            val camera = mc.gameRenderer.camera

            // Get viewport dimensions
            val window = mc.window
            val screenWidth = window.scaledWidth
            val screenHeight = window.scaledHeight

            // Create a copy of the position to avoid modifying the original
            val positionVec = Vec3d(pos.x, pos.y, pos.z)

            // Step 1: Calculate position relative to camera
            val relativePosVec = positionVec.subtract(camera.pos)

            // Step 2: Apply camera rotation
            // Get camera angles in radians
            val cameraYaw = Math.toRadians(camera.yaw + 180.0)
            val cameraPitch = Math.toRadians(camera.pitch.toDouble())

            // Rotate the vector based on camera orientation
            val rotatedX = relativePosVec.x * Math.cos(cameraYaw) + relativePosVec.z * Math.sin(cameraYaw)
            val rotatedZ = -relativePosVec.x * Math.sin(cameraYaw) + relativePosVec.z * Math.cos(cameraYaw)
            val rotatedY = relativePosVec.y * Math.cos(cameraPitch) - rotatedZ * Math.sin(cameraPitch)
            val finalZ = relativePosVec.y * Math.sin(cameraPitch) + rotatedZ * Math.cos(cameraPitch)

            // Check if the position is behind the camera
            if (finalZ <= 0.05) {
                return null
            }

            // Step 3: Project to screen
            val fov = Math.toRadians(mc.options.fov.value.toDouble())
            val aspectRatio = screenWidth.toDouble() / screenHeight.toDouble()

            val scale = 1.0 / Math.tan(fov / 2.0) / finalZ

            val screenX = screenWidth / 2.0 + rotatedX * scale * screenHeight
            val screenY = screenHeight / 2.0 - rotatedY * scale * screenHeight / aspectRatio

            return Vec3d(screenX, screenY, finalZ)
        }
    }
}