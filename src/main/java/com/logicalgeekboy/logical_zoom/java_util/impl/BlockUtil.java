package com.logicalgeekboy.logical_zoom.java_util.impl;

import com.logicalgeekboy.logical_zoom.java_util.MC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

public class BlockUtil implements MC {

    public static BlockUtil INSTANCE = new BlockUtil();

    public ArrayList method1010(int n, Predicate predicate) {
        ArrayList<BlockPos> arrayList = new ArrayList<>();
        for (int i = -n; i <= n; ++i) {
            for (int j = -n; j <= n; ++j) {
                for (int k = -n; k <= n; ++k) {
                    BlockPos blockPos;
                    if (predicate.test(blockPos = Objects.requireNonNull(getMc().player).getBlockPos().add(i, j, k))) {
                        arrayList.add(blockPos);
                    }
                }
            }
        }
        return arrayList;
    }

    public boolean isCollidesEntity(BlockPos blockPos) {
        for (Entity entity : Objects.requireNonNull(getMc().world).getEntities()) {
            if (!(entity instanceof ItemEntity) && new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1).intersects(entity.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }
}