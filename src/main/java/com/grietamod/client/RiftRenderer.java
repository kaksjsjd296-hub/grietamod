package com.grietamod.client;

import com.grietamod.GrietaMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Dibuja (1) el cielo rojo remolineante y (2) la grieta con borde rosa brillante.
 * Todo se dibuja justo despues del cielo, antes del terreno, asi las montanas y arboles quedan por delante.
 */
@Mod.EventBusSubscriber(modid = GrietaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RiftRenderer {
    private static final ResourceLocation CLOUDS = new ResourceLocation(GrietaMod.MODID, "textures/environment/red_clouds.png");
    private static final ResourceLocation GALAXY = new ResourceLocation(GrietaMod.MODID, "textures/environment/rift_galaxy.png");
    private static final ResourceLocation WHITE = new ResourceLocation(GrietaMod.MODID, "textures/environment/white.png");

    // ---- tamano de la grieta (bloques, a DIST bloques de distancia en el cielo) ----
    private static final float DIST = 90f;
    private static final float HALF_W = 30f;   // medio ancho maximo
    private static final float HALF_H = 50f;   // media altura (la grieta mide ~100 de alto)
    private static final float TILT = (float) Math.toRadians(-18.0);
    private static final int SLICES = 48;

    // base de coordenadas de la grieta (se calcula en cada frame)
    private static float dx, dy, dz, rx, ry, rz, ux, uy, uz, cosT, sinT;

    public static float smooth(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float easeOut(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float i = 1f - t;
        return 1f - i * i * i;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimensionType().hasSkyLight() || !ClientRiftState.isVisible()) {
            return;
        }

        float pt = event.getPartialTick();
        float sky = smooth(Mth.lerp(pt, ClientRiftState.prevSky, ClientRiftState.sky));
        float rift = Mth.lerp(pt, ClientRiftState.prevRift, ClientRiftState.rift);
        float time = mc.level.getGameTime() + pt;
        Matrix4f m = event.getPoseStack().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (sky > 0.001f) {
            drawDome(m, time, sky);
        }
        if (rift > 0.005f) {
            drawRift(m, time, rift);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // ============================================================ CIELO ROJO

    private static void drawDome(Matrix4f m, float time, float s) {
        // capa 1: nubes grandes girando lento + remolino
        domeLayer(m, CLOUDS, time, s * 0.92f, 1.30f, 0.0035f, 2.6f, 0.00f, 0.00f, 0.85f, 0.75f, 0.75f);
        // capa 2: nubes mas finas girando al reves
        domeLayer(m, CLOUDS, time, s * 0.55f, 0.90f, -0.0060f, -3.6f, 0.37f, 0.21f, 1.00f, 0.55f, 0.55f);
    }

    private static void domeLayer(Matrix4f m, ResourceLocation tex, float time, float alpha, float scale,
                                  float spin, float twist, float uOff, float vOff, float cr, float cg, float cb) {
        RenderSystem.setShaderTexture(0, tex);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        final int rings = 22;
        final int segs = 44;
        final float minE = -0.35f;
        final float maxE = (float) (Math.PI / 2.0);
        for (int r = 0; r < rings; r++) {
            float e0 = Mth.lerp(r / (float) rings, minE, maxE);
            float e1 = Mth.lerp((r + 1) / (float) rings, minE, maxE);
            for (int s = 0; s < segs; s++) {
                float a0 = (float) (2.0 * Math.PI * s / segs);
                float a1 = (float) (2.0 * Math.PI * (s + 1) / segs);
                domeVertex(bb, m, e0, a0, time, alpha, scale, spin, twist, uOff, vOff, cr, cg, cb);
                domeVertex(bb, m, e0, a1, time, alpha, scale, spin, twist, uOff, vOff, cr, cg, cb);
                domeVertex(bb, m, e1, a1, time, alpha, scale, spin, twist, uOff, vOff, cr, cg, cb);
                domeVertex(bb, m, e1, a0, time, alpha, scale, spin, twist, uOff, vOff, cr, cg, cb);
            }
        }
        tess.end();
    }

    private static void domeVertex(BufferBuilder bb, Matrix4f m, float e, float a, float time, float alpha,
                                   float scale, float spin, float twist, float uOff, float vOff,
                                   float cr, float cg, float cb) {
        float ce = Mth.cos(e);
        float x = ce * Mth.cos(a);
        float y = Mth.sin(e);
        float z = ce * Mth.sin(a);

        float px = x * scale;
        float pz = z * scale;
        float rad = Mth.sqrt(px * px + pz * pz);
        float ang = time * spin + twist * (1f - rad);
        float c = Mth.cos(ang);
        float sn = Mth.sin(ang);
        float qx = px * c - pz * sn;
        float qz = px * sn + pz * c;

        float radius = 100f;
        bb.vertex(m, x * radius, y * radius, z * radius)
                .uv(qx * 0.5f + 0.5f + uOff, qz * 0.5f + 0.5f + vOff)
                .color(cr, cg, cb, alpha)
                .endVertex();
    }

    // ============================================================ GRIETA

    private static void drawRift(Matrix4f m, float time, float p) {
        // --- direccion de la grieta en el cielo (igual para todos los jugadores) ---
        float yawR = (float) Math.toRadians(ClientRiftState.yaw);
        float el = (float) Math.toRadians(ClientRiftState.elevation);
        Vec3 d = new Vec3(-Math.sin(yawR) * Math.cos(el), Math.sin(el), Math.cos(yawR) * Math.cos(el));
        Vec3 right = d.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(d);
        dx = (float) d.x; dy = (float) d.y; dz = (float) d.z;
        rx = (float) right.x; ry = (float) right.y; rz = (float) right.z;
        ux = (float) up.x; uy = (float) up.y; uz = (float) up.z;
        cosT = Mth.cos(TILT);
        sinT = Mth.sin(TILT);

        // --- animacion: primero se estira hacia arriba/abajo, luego se ensancha ---
        float hp = easeOut(p / 0.55f);
        float wp = easeOut((p - 0.20f) / 0.80f);
        float curH = HALF_H * hp * (1f + 0.012f * Mth.sin(time * 0.15f));
        float curW = HALF_W * wp;

        // --- bordes izquierdo y derecho de la grieta (forma de lente irregular) ---
        float[] la = new float[SLICES + 1];
        float[] ra = new float[SLICES + 1];
        float[] bs = new float[SLICES + 1];
        float[] wh = new float[SLICES + 1];
        for (int i = 0; i <= SLICES; i++) {
            float t = -1f + 2f * i / SLICES;
            float profile = (float) Math.pow(Math.max(0f, 1f - t * t), 0.8);
            float jag = 1f + 0.10f * Mth.sin(i * 2.3f + 1.1f) * Mth.sin(i * 0.7f)
                    + 0.04f * Mth.sin(time * 0.08f + i * 1.9f);
            float half = curW * profile * jag;
            float bend = Mth.sin(t * 2.2f) * curH * 0.08f;
            la[i] = bend - half;
            ra[i] = bend + half;
            bs[i] = t * curH;
            wh[i] = half;
        }

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();

        // ---- 1) interior: galaxia ----
        RenderSystem.setShaderTexture(0, GALAXY);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float gu = time * 0.0015f;
        float gv = time * 0.0008f;
        for (int i = 0; i < SLICES; i++) {
            galaxyV(bb, m, la[i], bs[i], p, gu, gv);
            galaxyV(bb, m, ra[i], bs[i], p, gu, gv);
            galaxyV(bb, m, ra[i + 1], bs[i + 1], p, gu, gv);
            galaxyV(bb, m, la[i + 1], bs[i + 1], p, gu, gv);
        }
        tess.end();

        // ---- 2) interior: imagen personalizada (si hay) ----
        if (ClientRiftState.imageLoaded) {
            RenderSystem.setShaderTexture(0, ClientRiftState.IMAGE_LOC);
            float boxAsp = HALF_W / HALF_H;
            float imgAsp = (float) ClientRiftState.imageW / (float) ClientRiftState.imageH;
            float uSpan = 1f;
            float vSpan = 1f;
            if (imgAsp > boxAsp) {
                uSpan = boxAsp / imgAsp;
            } else {
                vSpan = imgAsp / boxAsp;
            }
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < SLICES; i++) {
                imageV(bb, m, la[i], bs[i], p, uSpan, vSpan);
                imageV(bb, m, ra[i], bs[i], p, uSpan, vSpan);
                imageV(bb, m, ra[i + 1], bs[i + 1], p, uSpan, vSpan);
                imageV(bb, m, la[i + 1], bs[i + 1], p, uSpan, vSpan);
            }
            tess.end();
        }

        // ---- 3) borde brillante rosa (mezcla aditiva) ----
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderTexture(0, WHITE);
        float pulse = 0.85f + 0.15f * Mth.sin(time * 0.2f);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SLICES; i++) {
            float k0 = 0.4f + 0.6f * Math.min(1f, wh[i] / (HALF_W * 0.3f));
            float k1 = 0.4f + 0.6f * Math.min(1f, wh[i + 1] / (HALF_W * 0.3f));
            // brillo fino y fuerte
            glowQuad(bb, m, la[i], bs[i], la[i + 1], bs[i + 1], -1f, 1.3f * k0, 1.3f * k1, 0.95f * p * pulse);
            glowQuad(bb, m, ra[i], bs[i], ra[i + 1], bs[i + 1], 1f, 1.3f * k0, 1.3f * k1, 0.95f * p * pulse);
            // aureola ancha y suave
            glowQuad(bb, m, la[i], bs[i], la[i + 1], bs[i + 1], -1f, 6f * k0, 6f * k1, 0.40f * p * pulse);
            glowQuad(bb, m, ra[i], bs[i], ra[i + 1], bs[i + 1], 1f, 6f * k0, 6f * k1, 0.40f * p * pulse);
        }
        tess.end();
        RenderSystem.defaultBlendFunc();
    }

    private static void galaxyV(BufferBuilder bb, Matrix4f m, float a, float b, float alpha, float gu, float gv) {
        float u = a / (2f * HALF_W) * 1.4f + 0.5f + gu;
        float v = -b / (2f * HALF_H) * 1.4f + 0.5f + gv;
        put(bb, m, a, b, u, v, 1f, 1f, 1f, alpha);
    }

    private static void imageV(BufferBuilder bb, Matrix4f m, float a, float b, float alpha, float uSpan, float vSpan) {
        float u = 0.5f + (a / (2f * HALF_W)) * uSpan;
        float v = 0.5f - (b / (2f * HALF_H)) * vSpan;
        put(bb, m, a, b, u, v, 1f, 1f, 1f, alpha);
    }

    /** Franja de brillo a un lado del borde: opaca en el borde, transparente hacia afuera (y un poco hacia adentro). */
    private static void glowQuad(BufferBuilder bb, Matrix4f m, float a0, float b0, float a1, float b1,
                                 float side, float w0, float w1, float alpha) {
        // hacia afuera
        put(bb, m, a0, b0, 0.5f, 0.5f, 1.0f, 0.55f, 0.82f, alpha);
        put(bb, m, a0 + side * w0, b0, 0.5f, 0.5f, 0.9f, 0.15f, 0.55f, 0f);
        put(bb, m, a1 + side * w1, b1, 0.5f, 0.5f, 0.9f, 0.15f, 0.55f, 0f);
        put(bb, m, a1, b1, 0.5f, 0.5f, 1.0f, 0.55f, 0.82f, alpha);
        // hacia adentro (mas corto)
        put(bb, m, a0, b0, 0.5f, 0.5f, 1.0f, 0.55f, 0.82f, alpha * 0.6f);
        put(bb, m, a0 - side * w0 * 0.5f, b0, 0.5f, 0.5f, 0.9f, 0.15f, 0.55f, 0f);
        put(bb, m, a1 - side * w1 * 0.5f, b1, 0.5f, 0.5f, 0.9f, 0.15f, 0.55f, 0f);
        put(bb, m, a1, b1, 0.5f, 0.5f, 1.0f, 0.55f, 0.82f, alpha * 0.6f);
    }

    /** Convierte coordenadas locales de la grieta (a,b) a coordenadas del cielo y agrega el vertice. */
    private static void put(BufferBuilder bb, Matrix4f m, float a, float b, float u, float v,
                            float r, float g, float bl, float alpha) {
        float aa = a * cosT - b * sinT;
        float bb2 = a * sinT + b * cosT;
        float x = dx * DIST + rx * aa + ux * bb2;
        float y = dy * DIST + ry * aa + uy * bb2;
        float z = dz * DIST + rz * aa + uz * bb2;
        bb.vertex(m, x, y, z).uv(u, v).color(r, g, bl, alpha).endVertex();
    }
}
