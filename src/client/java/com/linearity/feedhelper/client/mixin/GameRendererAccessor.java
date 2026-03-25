package com.linearity.feedhelper.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.CachedPerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("hud3dProjectionMatrixBuffer")
    CachedPerspectiveProjectionMatrixBuffer callGetHudProjectionMatrix();

    @Invoker("getProjectionMatrixForCulling")
    Matrix4f callGetProjectionMatrix(float fov);

    @Invoker("getProjectionMatrix")
    Matrix4f callGetBasicProjectionMatrix(float fov);

    @Invoker("getFov")
    float callGetFov(Camera camera, float tickProgress, boolean changingFov);
}
