package com.linearity.feedhelper.client.utils;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class MatrixUtils {

    /**
     * 将世界坐标转换为屏幕坐标
     *
     * @param worldPos 世界坐标
     * @param matrices 当前MatrixStack
     * @param screenWidth 屏幕宽
     * @param screenHeight 屏幕高
     * @return 屏幕坐标（0,0左上角），超出屏幕返回null
     */
    public static Vec3d project(Vec3d worldPos, MatrixStack matrices, int screenWidth, int screenHeight) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 构造齐次坐标
        Vector4f pos = new Vector4f((float) worldPos.x, (float) worldPos.y, (float) worldPos.z, 1.0f);
        pos.mul(matrix);

        // 透视除法
        if (pos.w() == 0) return null;
        float ndcX = pos.x() / pos.w();
        float ndcY = pos.y() / pos.w();
        float ndcZ = pos.z() / pos.w();

        // NDC -> 屏幕坐标
        float screenX = (ndcX * 0.5f + 0.5f) * screenWidth;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * screenHeight; // y翻转
        float screenZ = ndcZ; // 深度（0~1）

        // 超出屏幕则返回null
        if (screenX < 0 || screenX > screenWidth || screenY < 0 || screenY > screenHeight) return null;

        return new Vec3d(screenX, screenY, screenZ);
    }
}
