/*
 * MIT License
 *
 * Copyright (c) 2022-2025
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@file:Suppress("KotlinConstantConditions", "UNCHECKED_CAST")

package com.dov.cm.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.*
import net.minecraft.world.chunk.Chunk
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11

/**
 * A utility class for rendering in Minecraft.
 * @version 09-08-2024
 * @since 07-18-2023
 */
object RenderUtils {
    private val minecraftClient = MinecraftClient.getInstance()

    /**
     * How many chunks away to render things.
     */
    var CHUNK_RADIUS: Int = minecraftClient.options.viewDistance.value

    /**
     * The last player configured gamma.
     */
    private var LAST_GAMMA = 0.0

    /**
     * Resets the render system to the default state.
     */
    fun resetRenderSystem() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableBlend()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
    }

    /**
     * Sets the gamma of the game to the full bright value of 16.0 while storing the last gamma value.
     */
    fun setHighGamma() {
        LAST_GAMMA = gamma
        gamma = 16.0
    }

    /**
     * Resets the gamma to the players last configured value.
     */
    fun setLowGamma() {
        gamma = LAST_GAMMA
    }

    var gamma: Double
        /**
         * Gets the current game gamma.
         *
         * @return The current game gamma.
         */
        get() = minecraftClient.options.gamma.value
        /**
         * Sets the gamma to the given value.
         *
         * @param gamma The value to set the gamma to.
         */
        set(gamma) {
            val newValue = gamma
                .coerceAtLeast(0.0)
                .coerceAtMost(16.0)
            val newGamma = minecraftClient.options.gamma
            if (newGamma.value != newValue) {
                // Note: This would need a proper implementation without the ISimpleOption interface
                // For now, let's just set the value directly if possible
                try {
                    val method = newGamma.javaClass.getDeclaredMethod("setValue", Double::class.java)
                    method.isAccessible = true
                    method.invoke(newGamma, newValue)
                } catch (e: Exception) {
                    // Fallback method if reflection fails
                    println("Could not set gamma: ${e.message}")
                }
            }
        }

    /**
     * Whether the gamma is set to the full bright value, 16.0.
     */
    val isHighGamma: Boolean
        get() = gamma == 16.0

    /**
     * Whether the gamma is set to the last gamma value as defined by the player.
     */
    val isLastGamma: Boolean
        get() = gamma <= LAST_GAMMA

    /**
     * Gets the current render distance based off of the maximum of the client and simulation render distances.
     * @return The current render distance.
     */
    fun getRenderDistance(): Int {
        val client = minecraftClient.options.viewDistance.value + 1
        val networkView = minecraftClient.world?.simulationDistance ?: 0 + 1
        return maxOf(client, networkView)
    }

    /**
     * Gets the visible chunks around the player based off of the render distance.
     * @return The visible chunks around the player.
     *
     * @see getRenderDistance
     */
    fun getVisibleChunks(renderDistance: Int = getRenderDistance()): List<Chunk> {
        val chunks = mutableSetOf<Chunk>()
        val player = minecraftClient.player ?: return emptyList()
        val chunkX = player.chunkPos.x
        val chunkZ = player.chunkPos.z
        val level = minecraftClient.world ?: return emptyList()

        for (x in -(renderDistance + 1) until (renderDistance)) {
            for (z in -(renderDistance + 1) until (renderDistance)) {
                val chunkX1 = chunkX + x
                val chunkZ1 = chunkZ + z
                chunks.add(level.getChunk(chunkX1, chunkZ1))
            }
        }
        return chunks.toList()
    }

    /**
     * Gets the camera position.
     * @return The camera position.
     */
    fun getCameraPos(): Vec3d {
        val camera = MinecraftClient.getInstance().blockEntityRenderDispatcher.camera
        return camera.pos
    }

    /**
     * Gets the camera block position.
     * @return The camera block position.
     */
    private fun getCameraBlockPos(): BlockPos {
        val camera = minecraftClient.entityRenderDispatcher.camera
        return camera.blockPos
    }

    /**
     * Gets the camera region position.
     * @return The camera region position.
     */
    fun getCameraRegionPos(): RegionPos {
        return RegionPos.fromBlockPos(getCameraBlockPos())
    }

    /**
     * Applies the regional render offset to the given matrix stack.
     * @param stack The matrix stack to apply the offset to.
     * @param region The region to apply the offset from.
     */
    fun applyRegionalRenderOffset(stack: MatrixStack, region: RegionPos) {
        val offset = region.toVec3d().subtract(getCameraPos())
        stack.translate(offset.x, offset.y, offset.z)
    }

    /**
     * Draws an outlined box.
     * @param box The box to draw.
     * @param matrixStack The matrix stack to draw with.
     */
    fun drawOutlinedBox(box: Box, matrixStack: MatrixStack) {
        val matrix = matrixStack.peek().positionMatrix
        val tessellator = RenderSystem.renderThreadTesselator()
        RenderSystem.setShader(ShaderProgramKeys.POSITION)
        val bufferBuilder = tessellator
            .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION)

        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        bufferBuilder.vertex(matrix, minX, minY, minZ)
        bufferBuilder.vertex(matrix, maxX, minY, minZ)

        bufferBuilder.vertex(matrix, maxX, minY, minZ)
        bufferBuilder.vertex(matrix, maxX, minY, maxZ)

        bufferBuilder.vertex(matrix, maxX, minY, maxZ)
        bufferBuilder.vertex(matrix, minX, minY, maxZ)

        bufferBuilder.vertex(matrix, minX, minY, maxZ)
        bufferBuilder.vertex(matrix, minX, minY, minZ)

        bufferBuilder.vertex(matrix, minX, minY, minZ)
        bufferBuilder.vertex(matrix, minX, maxY, minZ)

        bufferBuilder.vertex(matrix, maxX, minY, minZ)
        bufferBuilder.vertex(matrix, maxX, maxY, minZ)

        bufferBuilder.vertex(matrix, maxX, minY, maxZ)
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ)

        bufferBuilder.vertex(matrix, minX, minY, maxZ)
        bufferBuilder.vertex(matrix, minX, maxY, maxZ)

        bufferBuilder.vertex(matrix, minX, maxY, minZ)
        bufferBuilder.vertex(matrix, maxX, maxY, minZ)

        bufferBuilder.vertex(matrix, maxX, maxY, minZ)
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ)

        bufferBuilder.vertex(matrix, maxX, maxY, maxZ)
        bufferBuilder.vertex(matrix, minX, maxY, maxZ)

        bufferBuilder.vertex(matrix, minX, maxY, maxZ)
        bufferBuilder.vertex(matrix, minX, maxY, minZ)

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
    }

    /**
     * Uploads an outlined box to a vertex buffer.
     * @param box The box to draw.
     * @param vertexBuffer The vertex buffer to upload to.
     */
    fun drawOutlinedBox(box: Box, vertexBuffer: VertexBuffer) {
        val tessellator = RenderSystem.renderThreadTesselator()
        val bb = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION)
        drawOutlinedBox(box, bb)
        val built = bb.end()
        vertexBuffer.bind()
        vertexBuffer.upload(built)
        VertexBuffer.unbind()
    }

    /**
     * Draws an outlined box. This will lerp the box to the current position of the entity.
     * @param partialTicks The delta time.
     * @param bufferBuilder The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param entity The entity to draw the box around.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the box.
     */
    fun drawOutlinedBox(
        partialTicks: Float,
        bufferBuilder: BufferBuilder,
        matrix4f: Matrix4f,
        entity: Entity,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
        lerp: Boolean = true,
    ) {
        var box = entity.boundingBox
        if (lerp) {
            val lerpedX = MathHelper.lerp(partialTicks.toDouble(), entity.prevX, entity.x) - entity.x
            val lerpedY = MathHelper.lerp(partialTicks.toDouble(), entity.prevY, entity.y) - entity.y
            val lerpedZ = MathHelper.lerp(partialTicks.toDouble(), entity.prevZ, entity.z) - entity.z
            box = Box(
                lerpedX + box.minX,
                lerpedY + box.minY,
                lerpedZ + box.minZ,
                lerpedX + box.maxX,
                lerpedY + box.maxY,
                lerpedZ + box.maxZ
            )
        }
        drawOutlinedBox(
            box, bufferBuilder, matrix4f, red, green, blue, alpha
        )
    }

    /**
     * Draws an outlined box.
     * @param bb The box to draw.
     * @param buffer The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the box.
     */
    fun drawOutlinedBox(
        bb: Box,
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
        withOffset: Boolean = true,
    ) {
        var minX = bb.minX.toFloat()
        var minY = bb.minY.toFloat()
        var minZ = bb.minZ.toFloat()
        var maxX = bb.maxX.toFloat()
        var maxY = bb.maxY.toFloat()
        var maxZ = bb.maxZ.toFloat()

        var max = Vec3d(maxX.toDouble(), maxY.toDouble(), maxZ.toDouble())
        var min = Vec3d(minX.toDouble(), minY.toDouble(), minZ.toDouble())
        if (withOffset) {
            max = max.subtract(getCameraRegionPos().toVec3d())
            min = min.subtract(getCameraRegionPos().toVec3d())
        }
        val newBB = Box(min, max)
        minX = newBB.minX.toFloat()
        minY = newBB.minY.toFloat()
        minZ = newBB.minZ.toFloat()
        maxX = newBB.maxX.toFloat()
        maxY = newBB.maxY.toFloat()
        maxZ = newBB.maxZ.toFloat()

        // bottom face
        buffer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha)
        // top face
        buffer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha)
        // corners
        buffer.vertex(matrix4f, minX, minY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, maxY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, minY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, maxY, minZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, minY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, maxX, maxY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, minY, maxZ).color(red, green, blue, alpha)
        buffer.vertex(matrix4f, minX, maxY, maxZ).color(red, green, blue, alpha)
    }

    /**
     * Draws a line from the center of the clients screen to the given end point.
     * @param buffer The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param partialTicks The delta time.
     * @param end The end point of the line.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the line.
     * @param offsetEnd Whether to offset the end point by the camera region position.
     */
    fun drawSingleLine(
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        partialTicks: Float,
        end: Vec3d,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
        offsetEnd: Boolean = false,
    ) {
        if (offsetEnd) {
            drawSingleLine(
                buffer,
                matrix4f,
                getCenterOfScreen(partialTicks),
                end.subtract(getCameraRegionPos().toVec3d()),
                red, green, blue,
                alpha
            )
        } else {
            drawSingleLine(buffer, matrix4f, getCenterOfScreen(partialTicks), end, red, green, blue, alpha)
        }
    }

    /**
     * Draws a line from the center of the clients screen to the given end point. This will lerp the end point to
     * the current position of the entity.
     * @param buffer The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param partialTicks The delta time.
     * @param entity The entity to draw to.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the line.
     */
    fun drawSingleLine(
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        partialTicks: Float,
        entity: Entity,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
    ) {
        val lerpedX = MathHelper.lerp(partialTicks.toDouble(), entity.prevX, entity.x)
        val lerpedY = MathHelper.lerp(partialTicks.toDouble(), entity.prevY, entity.y)
        val lerpedZ = MathHelper.lerp(partialTicks.toDouble(), entity.prevZ, entity.z)

        val lerpedPos = Vec3d(lerpedX, lerpedY, lerpedZ)
            .subtract(getCameraRegionPos().toVec3d())

        val box = Box(
            lerpedPos.x + entity.boundingBox.minX,
            lerpedPos.y + entity.boundingBox.minY,
            lerpedPos.z + entity.boundingBox.minZ,
            lerpedPos.x + entity.boundingBox.maxX,
            lerpedPos.y + entity.boundingBox.maxY,
            lerpedPos.z + entity.boundingBox.maxZ
        )
        val center = box.center
        drawSingleLine(buffer, matrix4f, getCenterOfScreen(partialTicks), center, red, green, blue, alpha)
    }

    /**
     * Draws a line from the center of the clients screen to the given end point.
     * @param buffer The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param partialTicks The delta time.
     * @param end The end point of the line.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the line.
     */
    fun drawSingleLine(
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        partialTicks: Float,
        end: BlockPos,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
    ) {
        // Convert RegionPos to BlockPos equivalent
        val regionPos = getCameraRegionPos()
        val regionBlockPos = BlockPos(regionPos.x, regionPos.y, regionPos.z)

        drawSingleLine(
            buffer,
            matrix4f,
            getCenterOfScreen(partialTicks),
            end.subtract(regionBlockPos).toCenterPos(),
            red, green, blue,
            alpha
        )
    }


    fun drawOutlinedPlane(
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        start: Vec3d,
        end: Vec3d,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
        withOffset: Boolean = false,
    ) {
        if (!withOffset) {
            val box = Box(
                start.x, start.y, start.z, end.x, end.y, end.z
            )
            drawOutlinedBox(box, buffer, matrix4f, red, green, blue, alpha)
        } else {
            val box = Box(
                start.subtract(getCameraRegionPos().toVec3d()),
                end.subtract(getCameraRegionPos().toVec3d())
            )
            drawOutlinedBox(box, buffer, matrix4f, red, green, blue, alpha)
        }
    }

    /**
     * Draws a line from the given start point to the given end point.
     * @param buffer The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param start The start point of the line.
     * @param end The end point of the line.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the line.
     */
    fun drawSingleLine(
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        start: Vec3d,
        end: Vec3d,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
        withOffset: Boolean = false,
    ) {
        if (withOffset) {
            drawSingleLine(
                buffer,
                matrix4f,
                start.subtract(getCameraRegionPos().toVec3d()).toVector3f(),
                end.subtract(getCameraRegionPos().toVec3d()).toVector3f(),
                red, green, blue,
                alpha
            )
            return
        }
        drawSingleLine(buffer, matrix4f, start.toVector3f(), end.toVector3f(), red, green, blue, alpha)
    }

    /**
     * Draws a line from the given start point in the given direction.
     * @param buffer The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param start The start point of the line.
     * @param direction The direction of the line.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the line.
     */
    fun drawSingleLine(
        buffer: BufferBuilder,
        matrix4f: Matrix4f,
        start: Vec3d,
        direction: Direction,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
        withOffset: Boolean = false,
    ) {
        val end = when (direction) {
            Direction.UP -> start.add(0.0, 1.0, 0.0)
            Direction.DOWN -> start.add(0.0, -1.0, 0.0)
            Direction.NORTH -> start.add(0.0, 0.0, -1.0)
            Direction.SOUTH -> start.add(0.0, 0.0, 1.0)
            Direction.EAST -> start.add(1.0, 0.0, 0.0)
            Direction.WEST -> start.add(-1.0, 0.0, 0.0)
        }
        drawSingleLine(buffer, matrix4f, start, end, red, green, blue, alpha, withOffset)
    }

    /**
     * Draws a line from the given start point to the given end point.
     * @param bufferBuilder The buffer builder to draw with.
     * @param matrix4f The matrix to draw with.
     * @param start The start point of the line.
     * @param end The end point of the line.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the line.
     */
    private fun drawSingleLine(
        bufferBuilder: BufferBuilder,
        matrix4f: Matrix4f,
        start: Vector3f,
        end: Vector3f,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
    ) {
        bufferBuilder.vertex(
            matrix4f, start.x, start.y, start.z
        ).color(red, green, blue, alpha)
        bufferBuilder.vertex(
            matrix4f, end.x, end.y, end.z
        ).color(red, green, blue, alpha)
    }

    /**
     * Draws an outlined box.
     * @param bb The box to draw.
     * @param buffer The buffer builder to draw with.
     */
    private fun drawOutlinedBox(bb: Box, buffer: BufferBuilder) {
        val minX = bb.minX.toFloat()
        val minY = bb.minY.toFloat()
        val minZ = bb.minZ.toFloat()
        val maxX = bb.maxX.toFloat()
        val maxY = bb.maxY.toFloat()
        val maxZ = bb.maxZ.toFloat()
        buffer.vertex(minX, minY, minZ)
        buffer.vertex(maxX, minY, minZ)
        buffer.vertex(maxX, minY, minZ)
        buffer.vertex(maxX, minY, maxZ)
        buffer.vertex(maxX, minY, maxZ)
        buffer.vertex(minX, minY, maxZ)
        buffer.vertex(minX, minY, maxZ)
        buffer.vertex(minX, minY, minZ)
        buffer.vertex(minX, minY, minZ)
        buffer.vertex(minX, maxY, minZ)
        buffer.vertex(maxX, minY, minZ)
        buffer.vertex(maxX, maxY, minZ)
        buffer.vertex(maxX, minY, maxZ)
        buffer.vertex(maxX, maxY, maxZ)
        buffer.vertex(minX, minY, maxZ)
        buffer.vertex(minX, maxY, maxZ)
        buffer.vertex(minX, maxY, minZ)
        buffer.vertex(maxX, maxY, minZ)
        buffer.vertex(maxX, maxY, minZ)
        buffer.vertex(maxX, maxY, maxZ)
        buffer.vertex(maxX, maxY, maxZ)
        buffer.vertex(minX, maxY, maxZ)
        buffer.vertex(minX, maxY, maxZ)
        buffer.vertex(minX, maxY, minZ)
    }

    /**
     * Cleans up the render system by popping the matrix stack, resetting the shader color, and enabling depth testing.
     * @param matrixStack The matrix stack to clean up.
     */
    fun cleanupRender(matrixStack: MatrixStack) {
        matrixStack.pop()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_BLEND)
    }

    /**
     * Gets the look vector of the player.
     * @param delta The delta time.
     * @return The look vector of the player.
     */
    fun getLookVec(delta: Float): Vec3d {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return Vec3d.ZERO
        val pitch = player.getPitch(delta)
        val yaw = player.getYaw(delta)
        return Rotation(pitch, yaw).asLookVec()
    }

    /**
     * Sets up the render by enabling blending, disabling depth testing, pushing the matrix stack and
     * applying the regional render offset.
     */
    fun setupRender(matrixStack: MatrixStack) {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        matrixStack.push()
        val region = getCameraRegionPos()
        applyRegionalRenderOffset(matrixStack, region)
    }

    /**
     * Sets up the render with the position color shader.
     */
    fun setupRenderWithShader(matrixStack: MatrixStack) {
        setupRender(matrixStack)
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    }

    /**
     * Gets a buffer builder for rendering.
     *
     * @return A buffer builder for rendering.
     */
    fun getBufferBuilder(): BufferBuilder {
        val tessellator = Tessellator.getInstance()
        return tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)
    }

    /**
     * Gets the center of the screen based on the players look vector.
     * @param partialTicks The delta time.
     */
    private fun getCenterOfScreen(partialTicks: Float): Vec3d {
        val regionVec = getCameraRegionPos().toVec3d()
        return getLookVec(partialTicks).add(getCameraPos()).subtract(regionVec)
    }

    /**
     * Draws the given buffer builder with the global program.
     */
    fun drawBuffer(bufferBuilder: BufferBuilder) {
        try {
            val end = bufferBuilder.end()
            BufferRenderer.drawWithGlobalProgram(end)
        } catch (_: Exception) {
            // Ignore
        }
        resetRenderSystem()
    }

    /**
     * Draws the given buffer builder with the global program, then cleans up the render.
     */
    fun drawBuffer(buffer: BufferBuilder, matrixStack: MatrixStack) {
        drawBuffer(buffer)
        cleanupRender(matrixStack)
    }

    /**
     * Offsets the given position by the camera region position.
     */
    fun offsetPosWithCamera(pos: Vec3d): Vec3d {
        return pos.subtract(getCameraRegionPos().toVec3d())
    }

    /**
     * Gets the lerped position of an entity between its previous and current position.
     * @param e The entity to get the lerped position of.
     * @param partialTicks The delta time.
     * @return The lerped position of the entity.
     */
    fun getLerpedPos(e: Entity, partialTicks: Float): Vec3d {
        val lerpedX = MathHelper.lerp(partialTicks.toDouble(), e.prevX, e.x) - e.x
        val lerpedY = MathHelper.lerp(partialTicks.toDouble(), e.prevY, e.y) - e.y
        val lerpedZ = MathHelper.lerp(partialTicks.toDouble(), e.prevZ, e.z) - e.z
        return Vec3d(lerpedX, lerpedY, lerpedZ)
    }

    /**
     * Renders an entity's bounding box with the given color.
     * @param matrixStack The matrix stack to draw with.
     * @param partialTicks The delta time.
     * @param e The entity to render.
     * @param red The red component of the color.
     * @param green The green component of the color.
     * @param blue The blue component of the color.
     * @param alpha The alpha of the box.
     * @param region The camera region.
     * @param boxSize The size multiplier for the box.
     */
    fun renderEntityBox(
        matrixStack: MatrixStack,
        partialTicks: Float,
        e: Entity,
        red: Float = 1.0f,
        green: Float = 1.0f,
        blue: Float = 1.0f,
        alpha: Float = 1f,
        region: RegionPos = getCameraRegionPos(),
        boxSize: Float = 1.0f
    ) {
        matrixStack.push()
        RenderSystem.setShaderColor(red, green, blue, alpha)
        matrixStack.push()
        val lerped = getLerpedPos(e, partialTicks).subtract(region.toVec3d())
        matrixStack.translate(
            e.x + lerped.x, e.y + lerped.y, e.z + lerped.z
        )
        matrixStack.scale(boxSize, boxSize, boxSize)
        val bb = e.boundingBox.offset(-e.x, -e.y, -e.z)
        drawOutlinedBox(bb, matrixStack)
        matrixStack.pop()
        matrixStack.pop()
    }
}