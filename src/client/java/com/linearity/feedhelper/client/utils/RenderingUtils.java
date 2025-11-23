package com.linearity.feedhelper.client.utils;

import com.linearity.feedhelper.client.utils.linedshapes.IcosaSphere;
import com.linearity.feedhelper.client.utils.linedshapes.OutlineBalls;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBufferManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.linearity.feedhelper.client.utils.linedshapes.OutlineBalls.getSphereLinesCached;


public class RenderingUtils {

    public static final float[] ENDER_DRAGON_PURPLE = new float[]{
            0.7176471F + (ThreadLocalRandom.current().nextFloat() * ( 0.8745098F-0.7176471F)),
            0.F,
            0.8235294F + (ThreadLocalRandom.current().nextFloat() * ( 0.9764706F-0.8235294F)),
            0.5f};;

    public static int rectSize = 10;

    public static void renderBoxLines(Box box, MatrixStack matrices, VertexConsumer consumer, float[] color, Vec3d cameraPos) {
        Vector3f[] vertices = new Vector3f[] {
                new Vector3f((float)box.minX, (float)box.minY, (float)box.minZ),
                new Vector3f((float)box.maxX, (float)box.minY, (float)box.minZ),
                new Vector3f((float)box.maxX, (float)box.minY, (float)box.maxZ),
                new Vector3f((float)box.minX, (float)box.minY, (float)box.maxZ),
                new Vector3f((float)box.minX, (float)box.maxY, (float)box.minZ),
                new Vector3f((float)box.maxX, (float)box.maxY, (float)box.minZ),
                new Vector3f((float)box.maxX, (float)box.maxY, (float)box.maxZ),
                new Vector3f((float)box.minX, (float)box.maxY, (float)box.maxZ)
        };

        int[][] edges = new int[][]{
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        Matrix4f mat = matrices.peek().getPositionMatrix();

        for (int[] edge : edges) {
            Vector3f from = vertices[edge[0]];
            Vector3f to = vertices[edge[1]];

            // 世界坐标减去摄像机位置
            Vec3d offsetFrom = new Vec3d(from.x(), from.y(), from.z()).subtract(cameraPos);
            Vec3d offsetTo   = new Vec3d(to.x(), to.y(), to.z()).subtract(cameraPos);

            putLine(consumer, mat, 0,0,0,
                    new Vector3f((float)offsetFrom.x, (float)offsetFrom.y, (float)offsetFrom.z),
                    new Vector3f((float)offsetTo.x,   (float)offsetTo.y,   (float)offsetTo.z),
                    color);
        }
    }


    public static Vec3d worldToScreen(MinecraftClient client, Vec3d worldPos) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos(); // 摄像机位置
        Quaternionf rotation = camera.getRotation(); // 摄像机旋转

        // 世界坐标相对摄像机
        Vec3d relative = worldPos.subtract(camPos);

        // 构造 view 矩阵：旋转 + 平移
        Vector4f vec = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1f);
        // 用四元数旋转（旋转相反方向）
        Quaternionf inverseRot = new Quaternionf(rotation).conjugate();
        Vector3f tmp = new Vector3f(vec.x(), vec.y(), vec.z());
        inverseRot.transform(tmp);
        vec.set(tmp.x(), tmp.y(), tmp.z(), 1f);

        // 构造投影矩阵
        float fov = client.options.getFov().getValue().floatValue();
        float aspect = (float) client.getWindow().getScaledWidth() / client.getWindow().getScaledHeight();
        float near = 0.05f;
        float far = 1000f;
        float f = (float) (1.0 / Math.tan(Math.toRadians(fov / 2.0)));
        float[] proj = new float[16];
        proj[0] = f / aspect; proj[5] = f; proj[10] = (far + near) / (near - far); proj[11] = -1f;
        proj[14] = (2f * far * near) / (near - far);

        // Clip space
        float clipX = proj[0] * vec.x() + proj[2] * vec.z();
        float clipY = proj[5] * vec.y() + proj[6] * vec.z();
        float clipZ = proj[10] * vec.z() + proj[14];
        float clipW = -vec.z(); // 注意 Minecraft 使用右手系

        if (clipW == 0f) return null;

        float ndcX = clipX / clipW;
        float ndcY = clipY / clipW;
        float ndcZ = clipZ / clipW;

        // 裁剪
        if (ndcX < -1f || ndcX > 1f || ndcY < -1f || ndcY > 1f || ndcZ < -1f || ndcZ > 1f) {
            return null;
        }

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        double screenX = (ndcX * 0.5 + 0.5) * sw;
        double screenY = (1.0 - (ndcY * 0.5 + 0.5)) * sh;

        return new Vec3d(screenX, screenY, ndcZ);
    }

    public static void drawRect(DrawContext context, int x1, int y1, int x2, int y2, int argb) {
        context.drawVerticalLine(x1,y1,y2,argb);
        context.drawVerticalLine(x2,y1,y2,argb);
        context.drawHorizontalLine(x1,x2,y1,argb);
        context.drawHorizontalLine(x1,x2,y2,argb);

    }

    public static void renderTransparentSphere(MatrixStack matrices,
                                               double x, double y, double z,
                                               float radius, float[] color,
                                               int latDivisions, int lonDivisions) {
// 获取线框层

        VertexConsumerProvider.Immediate provider = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        VertexConsumer consumer = provider.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 设置线宽
        GL11.glLineWidth(2.f);


        for (OutlineBalls.Line line : getSphereLinesCached(radius,latDivisions,lonDivisions)) {

            Vector3f a = line.a();
            Vector3f b = line.b();

            putLine(consumer,matrix,x,y,z,a,b,color);
        }

        provider.draw();
    }

    // 画线段
    public static void putLine(VertexConsumer consumer, Matrix4f matrix,
                                double cx, double cy, double cz,
                                Vector3f from, Vector3f to, float[] color) {
        Vector3f normal = new Vector3f(to).sub(from).normalize(); // 简单法线

        // 起点
        consumer.vertex(matrix, (float)(cx + from.x()), (float)(cy + from.y()), (float)(cz + from.z()))
                .color(color[0], color[1], color[2], color[3])
                .texture(0f, 0f)      // UV0
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(0xF000F0)
                .normal(normal.x(), normal.y(), normal.z());

        // 终点
        consumer.vertex(matrix, (float)(cx + to.x()), (float)(cy + to.y()), (float)(cz + to.z()))
                .color(color[0], color[1], color[2], color[3])
                .texture(0f, 0f)      // UV0
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(0xF000F0)
                .normal(normal.x(), normal.y(), normal.z());
    }

    private static Vector3f sphericalToCartesian(float radius, float lat, float lon) {
        float x = (float)(radius * Math.sin(lat) * Math.cos(lon));
        float y = (float)(radius * Math.cos(lat));
        float z = (float)(radius * Math.sin(lat) * Math.sin(lon));
        return new Vector3f(x, y, z);
    }

    public static void renderBeam(MatrixStack matrices, Vec3d start, Vec3d end, float[] color, float width) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate provider = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = provider.getBuffer(RenderLayer.getEntityTranslucent(
                Identifier.of("minecraft", "textures/entity/beacon_beam.png")));

        Vec3d dir = end.subtract(start).normalize();
        float yaw = client.getEntityRenderDispatcher().camera.getYaw();
        float pitch = client.getEntityRenderDispatcher().camera.getPitch();

// 转成弧度
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);

// 摄像机朝向向量
        Vec3d cameraDir = new Vec3d(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        // 摄像机右向量
        Vec3d right = dir.crossProduct(cameraDir).normalize().multiply(width / 2.0);
        if (right.lengthSquared() < 1e-6) right = new Vec3d(width / 2.0, 0, 0);

        Vec3d up = right.crossProduct(dir).normalize().multiply(width / 2.0);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 八个角点组成柱体
        Vec3d[] corners = new Vec3d[]{
                start.add(right).add(up),
                start.add(right).subtract(up),
                start.subtract(right).subtract(up),
                start.subtract(right).add(up),
                end.add(right).add(up),
                end.add(right).subtract(up),
                end.subtract(right).subtract(up),
                end.subtract(right).add(up)
        };

        // 六个面，每面四个顶点
        int[][] faces = new int[][]{
                {0,1,5,4}, // 右面
                {1,2,6,5}, // 后面
                {2,3,7,6}, // 左面
                {3,0,4,7}, // 前面
                {0,3,2,1}, // 底面
                {4,5,6,7}  // 顶面
        };

        for (int[] face : faces) {
            consumer.vertex(matrix, (float) corners[face[0]].x, (float) corners[face[0]].y, (float) corners[face[0]].z)
                    .color(color[0], color[1], color[2], color[3])
                    .texture(0f, 0f)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0)
                    .normal(0f,1f,0f);

            consumer.vertex(matrix, (float) corners[face[1]].x, (float) corners[face[1]].y, (float) corners[face[1]].z)
                    .color(color[0], color[1], color[2], color[3])
                    .texture(1f, 0f)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0)
                    .normal(0f,1f,0f);

            consumer.vertex(matrix, (float) corners[face[2]].x, (float) corners[face[2]].y, (float) corners[face[2]].z)
                    .color(color[0], color[1], color[2], color[3])
                    .texture(1f, 1f)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0)
                    .normal(0f,1f,0f);

            consumer.vertex(matrix, (float) corners[face[3]].x, (float) corners[face[3]].y, (float) corners[face[3]].z)
                    .color(color[0], color[1], color[2], color[3])
                    .texture(0f, 1f)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0)
                    .normal(0f,1f,0f);
        }

        provider.draw();
    }

}
