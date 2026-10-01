package dev.apogee.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;

public interface ImplantHandRenderer {
    void apogee$renderImplants(float partialTick, PoseStack poses, MultiBufferSource.BufferSource buffers,
                               LocalPlayer player, int light);
}
