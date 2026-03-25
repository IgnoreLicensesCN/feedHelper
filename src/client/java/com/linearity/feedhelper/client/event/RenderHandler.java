package com.linearity.feedhelper.client.event;

import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

public class RenderHandler implements IRenderer
{
    private static final RenderHandler INSTANCE = new RenderHandler();
    private final Minecraft mc;
    private Pair<Entity, CompoundTag> lastEnderItems;

    public RenderHandler()
    {
        this.mc = Minecraft.getInstance();
        this.lastEnderItems = null;
    }

    public static RenderHandler getInstance()
    {
        return INSTANCE;
    }


}
