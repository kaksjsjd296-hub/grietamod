package com.grietamod.client;

import com.grietamod.GrietaMod;
import com.grietamod.net.NetworkHandler;
import com.grietamod.net.UploadChunkPacket;
import com.grietamod.server.RiftManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Menu de la "Grieta Configuradora": elegir una imagen y subirla para que aparezca dentro de la grieta. */
public class RiftConfigScreen extends Screen {
    private static final ResourceLocation PREVIEW_LOC = new ResourceLocation(GrietaMod.MODID, "dynamic/rift_preview");
    private static final int PREVIEW_SIZE = 100;

    private byte[] pngBytes;
    private DynamicTexture previewTexture;
    private int previewW, previewH;
    private Component status = Component.empty();
    private int statusColor = 0xAAAAAA;

    public RiftConfigScreen() {
        super(Component.translatable("gui.grietamod.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int rowY1 = 34 + PREVIEW_SIZE + 28;
        int rowY2 = rowY1 + 24;

        addRenderableWidget(Button.builder(Component.translatable("gui.grietamod.choose"), b -> chooseFile())
                .bounds(cx - 155, rowY1, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.grietamod.remove"), b -> removeImage())
                .bounds(cx + 5, rowY1, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.grietamod.upload"), b -> upload())
                .bounds(cx - 155, rowY2, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.grietamod.close"), b -> onClose())
                .bounds(cx + 5, rowY2, 150, 20).build());
    }

    // ------------------------------------------------------------------ acciones

    private void chooseFile() {
        String path = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(5);
            filters.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg"))
                    .put(stack.UTF8("*.bmp")).put(stack.UTF8("*.gif")).flip();
            path = TinyFileDialogs.tinyfd_openFileDialog(
                    Component.translatable("gui.grietamod.dialog_title").getString(),
                    "", filters, "Imagenes (png, jpg, gif, bmp)", false);
        } catch (Throwable t) {
            setStatus(Component.translatable("gui.grietamod.dialog_failed"), 0xFF7777);
            return;
        }
        if (path != null && !path.isEmpty()) {
            loadFile(Path.of(path));
        }
    }

    private void loadFile(Path file) {
        try {
            ClientImageUtil.Result r = ClientImageUtil.prepare(file);
            this.pngBytes = r.png;
            setPreview(r.png);
            setStatus(Component.translatable("gui.grietamod.ready", r.width, r.height, r.png.length / 1024), 0x77FF77);
        } catch (Exception e) {
            this.pngBytes = null;
            setStatus(Component.translatable("gui.grietamod.error", String.valueOf(e.getMessage())), 0xFF7777);
        }
    }

    private void upload() {
        if (pngBytes == null) {
            setStatus(Component.translatable("gui.grietamod.pick_first"), 0xFFCC66);
            return;
        }
        if (pngBytes.length > RiftManager.MAX_IMAGE_BYTES) {
            setStatus(Component.translatable("gui.grietamod.too_big"), 0xFF7777);
            return;
        }
        int chunk = RiftManager.CHUNK_SIZE;
        int total = (pngBytes.length + chunk - 1) / chunk;
        for (int i = 0; i < total; i++) {
            int from = i * chunk;
            int to = Math.min(pngBytes.length, from + chunk);
            NetworkHandler.CHANNEL.sendToServer(new UploadChunkPacket(i, total, Arrays.copyOfRange(pngBytes, from, to)));
        }
        onClose();
    }

    private void removeImage() {
        NetworkHandler.CHANNEL.sendToServer(new UploadChunkPacket(0, 0, new byte[0]));
        this.pngBytes = null;
        releasePreview();
        setStatus(Component.translatable("gui.grietamod.removed"), 0xFFCC66);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (!paths.isEmpty()) {
            loadFile(paths.get(0));
        }
    }

    // ------------------------------------------------------------------ vista previa

    private void setPreview(byte[] png) throws java.io.IOException {
        releasePreview();
        NativeImage ni = NativeImage.read(new ByteArrayInputStream(png));
        previewW = ni.getWidth();
        previewH = ni.getHeight();
        previewTexture = new DynamicTexture(ni);
        Minecraft.getInstance().getTextureManager().register(PREVIEW_LOC, previewTexture);
    }

    private void releasePreview() {
        if (previewTexture != null) {
            Minecraft.getInstance().getTextureManager().release(PREVIEW_LOC);
            previewTexture = null;
        }
    }

    private void setStatus(Component c, int color) {
        this.status = c;
        this.statusColor = color;
    }

    @Override
    public void removed() {
        releasePreview();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);

        int x = this.width / 2 - PREVIEW_SIZE / 2;
        int y = 34;
        g.fill(x - 2, y - 2, x + PREVIEW_SIZE + 2, y + PREVIEW_SIZE + 2, 0xFF666666);
        g.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFF101010);

        if (previewTexture != null) {
            float s = Math.min(PREVIEW_SIZE / (float) previewW, PREVIEW_SIZE / (float) previewH);
            int dw = Math.max(1, Math.round(previewW * s));
            int dh = Math.max(1, Math.round(previewH * s));
            g.blit(PREVIEW_LOC, x + (PREVIEW_SIZE - dw) / 2, y + (PREVIEW_SIZE - dh) / 2,
                    dw, dh, 0, 0, previewW, previewH, previewW, previewH);
        } else {
            g.drawCenteredString(this.font, Component.translatable("gui.grietamod.drop_hint"),
                    this.width / 2, y + PREVIEW_SIZE / 2 - 4, 0x888888);
        }

        g.drawCenteredString(this.font, this.status, this.width / 2, y + PREVIEW_SIZE + 10, this.statusColor);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
