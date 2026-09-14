package dev.apogee.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Multiplies every vertex colour by a tint, used to draw afterimages with the wearer's equipment. */
final class TintedBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private int red;
    private int green;
    private int blue;
    private int alpha;

    TintedBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    void setTint(int argb) {
        alpha = argb >>> 24 & 0xFF;
        red = argb >>> 16 & 0xFF;
        green = argb >>> 8 & 0xFF;
        blue = argb & 0xFF;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new Tinted(delegate.getBuffer(renderType));
    }

    private final class Tinted implements VertexConsumer {
        private final VertexConsumer inner;

        private Tinted(VertexConsumer inner) {
            this.inner = inner;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            inner.setColor(r * red / 255, g * green / 255, b * blue / 255, a * alpha / 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }
}
