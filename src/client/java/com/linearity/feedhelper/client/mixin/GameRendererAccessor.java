package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("hudProjectionMatrix")
    ProjectionMatrix3 callGetHudProjectionMatrix();

    @Invoker("getProjectionMatrix")
    Matrix4f callGetProjectionMatrix(float fov);

    @Invoker("getBasicProjectionMatrix")
    Matrix4f callGetBasicProjectionMatrix(float fov);

    @Invoker("getFov")
    float callGetFov(Camera camera, float tickProgress, boolean changingFov);
}
