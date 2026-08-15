package com.dhj.actinium.mixin.vintage.core;

import com.gtnewhorizons.angelica.sdlgpu.SDLGPUGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Defers {@link NetHandlerPlayClient#handleJoinGame} until the FML handshake has registered the
 * {@link NetworkDispatcher} for the connection.
 *
 * <p>On the integrated server (local Netty channel) the join packets are delivered to the client
 * thread through the scheduled-task queue before the client-side handshake completion (which
 * attaches the dispatcher to the channel attribute) has run on the Netty IO thread. The vanilla
 * timing usually lets the handshake win, but the SDL GPU path makes the client thread fast enough
 * that {@code SPacketJoinGame} is processed first; Forge's {@code handleJoinGame} then NPEs on
 * {@code NetworkDispatcher.get(...)} and the world never loads. Re-queue the packet for the next
 * tick until the dispatcher is present.</p>
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClientJoinGame {

    @Shadow @Final
    private NetworkManager netManager;

    @Inject(method = "handleJoinGame", at = @At("HEAD"), cancellable = true)
    private void sdlDeferJoinGameUntilHandshake(SPacketJoinGame packetIn, CallbackInfo ci) {
        if (!SDLGPUGate.isActive()) return;
        final NetHandlerPlayClient self = (NetHandlerPlayClient) (Object) this;
        final NetworkManager nm = netManager;
        if (nm == null || nm.channel() == null) return;
        if (nm.channel().attr(NetworkDispatcher.FML_DISPATCHER).get() == null) {
            // Handshake not finished yet; try again on the next scheduled-task drain.
            Minecraft.getMinecraft().addScheduledTask(() -> self.handleJoinGame(packetIn));
            ci.cancel();
        }
    }
}
