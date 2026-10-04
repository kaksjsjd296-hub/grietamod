package com.grietamod.client;

import com.mojang.blaze3d.platform.NativeImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Lee PNG/JPG/GIF/BMP, la reduce (max 512 px) y la convierte a PNG para enviarla al servidor. */
public class ClientImageUtil {
    private static final int[] MAX_SIZES = {512, 384, 256, 192, 128};
    private static final int MAX_BYTES = 700_000;

    public static class Result {
        public final byte[] png;
        public final int width;
        public final int height;

        Result(byte[] png, int width, int height) {
            this.png = png;
            this.width = width;
            this.height = height;
        }
    }

    public static Result prepare(Path path) throws IOException {
        BufferedImage src = ImageIO.read(path.toFile());
        if (src == null) {
            throw new IOException("Formato de imagen no soportado");
        }
        int sw = src.getWidth();
        int sh = src.getHeight();
        int[] pixels = src.getRGB(0, 0, sw, sh, null, 0, sw);

        for (int max : MAX_SIZES) {
            float scale = Math.min(1f, max / (float) Math.max(sw, sh));
            int tw = Math.max(1, Math.round(sw * scale));
            int th = Math.max(1, Math.round(sh * scale));
            byte[] png = encode(pixels, sw, sh, tw, th);
            if (png.length <= MAX_BYTES) {
                return new Result(png, tw, th);
            }
        }
        throw new IOException("La imagen es demasiado grande");
    }

    /** Reduce con promedio por areas (ponderado por alpha) y codifica como PNG. */
    private static byte[] encode(int[] src, int sw, int sh, int tw, int th) throws IOException {
        try (NativeImage out = new NativeImage(NativeImage.Format.RGBA, tw, th, false)) {
            for (int y = 0; y < th; y++) {
                int y0 = (int) ((long) y * sh / th);
                int y1 = Math.max(y0 + 1, (int) ((long) (y + 1) * sh / th));
                for (int x = 0; x < tw; x++) {
                    int x0 = (int) ((long) x * sw / tw);
                    int x1 = Math.max(x0 + 1, (int) ((long) (x + 1) * sw / tw));
                    long sa = 0, sr = 0, sg = 0, sb = 0;
                    int count = 0;
                    for (int yy = y0; yy < y1 && yy < sh; yy++) {
                        for (int xx = x0; xx < x1 && xx < sw; xx++) {
                            int argb = src[yy * sw + xx];
                            int a = (argb >>> 24) & 0xFF;
                            sa += a;
                            sr += (long) ((argb >> 16) & 0xFF) * a;
                            sg += (long) ((argb >> 8) & 0xFF) * a;
                            sb += (long) (argb & 0xFF) * a;
                            count++;
                        }
                    }
                    int a, r, g, b;
                    if (sa == 0 || count == 0) {
                        a = 0; r = 0; g = 0; b = 0;
                    } else {
                        a = (int) (sa / count);
                        r = (int) (sr / sa);
                        g = (int) (sg / sa);
                        b = (int) (sb / sa);
                    }
                    // NativeImage usa el orden ABGR
                    out.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            return out.asByteArray();
        }
    }
}
