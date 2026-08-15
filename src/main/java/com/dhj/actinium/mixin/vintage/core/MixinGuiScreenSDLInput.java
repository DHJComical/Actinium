package com.dhj.actinium.mixin.vintage.core;

import com.gtnewhorizons.angelica.sdlgpu.SDLGPUGate;
import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SDL GPU input handling for GUI screens.
 *
 * <p>The SDL GPU path replaces {@code Display.update()} with its own event pump, so lwjglxx's
 * input queues behave differently than on the GL path (coalesced moves, no Display.update
 * polling). The vanilla {@code handleInput()} loop still works, but the Forge mouse-click event
 * gate (GuiScreenEvent.MouseClickedEvent) can swallow clicks; handle the queues directly and
 * invoke {@code mouseClicked} bypassing the Forge event so clicks reach the screen reliably.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenSDLInput {
    @Invoker("mouseClicked")
    abstract void invokeMouseClicked(int mouseX, int mouseY, int mouseButton);

    @Inject(method = "handleInput", at = @At("HEAD"), cancellable = true)
    private void sdlHandleInput(CallbackInfo ci) {
        if (SDLGPUGate.isActive()) {
            final GuiScreen self = (GuiScreen) (Object) this;
            while (Mouse.next()) {
                if (Mouse.getEventButton() != -1 && Mouse.getEventButtonState()) {
                    final int i = Mouse.getEventX() * self.width / self.mc.displayWidth;
                    final int j = self.height - Mouse.getEventY() * self.height / self.mc.displayHeight - 1;
                    invokeMouseClicked(i, j, Mouse.getEventButton());
                } else {
                    try {
                        self.handleMouseInput();
                    } catch (java.io.IOException ignored) {
                        // vanilla handleInput declares IOException; a throw would kill the tick
                    }
                }
            }
            while (Keyboard.next()) {
                try {
                    self.handleKeyboardInput();
                } catch (java.io.IOException ignored) {
                    // see above
                }
            }
            ci.cancel();
        }
    }
}
