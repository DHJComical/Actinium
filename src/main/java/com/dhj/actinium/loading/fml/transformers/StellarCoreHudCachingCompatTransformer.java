package com.dhj.actinium.loading.fml.transformers;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Bridges StellarCore's HudCaching with the GLSM state cache.
 *
 * <p>StellarCore renders the HUD into a cached framebuffer while
 * {@code HUDCaching.renderingCacheOverride} is true, and its own mixins
 * intercept the vanilla {@code GlStateManager} blend/color calls during that
 * window to force alpha factors {@code (ONE, ONE_MINUS_SRC_ALPHA)} and opaque
 * colors. Under Actinium those callers are redirected to the GLSM cache
 * instead, so StellarCore's interceptors never run; without the same overrides
 * the cache buffer ends up with per-element alpha (e.g. 0.3 for the JourneyMap
 * grid, 0 for the reticle) and the cached-HUD blit — whose RGB blend depends
 * on alpha — shows those areas as fully transparent, revealing the scene
 * behind the map GUI.</p>
 *
 * <p>{@code HUDCaching} is loaded by StellarCore's own early mixin loader
 * during tweak bootstrap, before any mixin prepare phase can run; a mixin
 * targeting it fails with {@code MixinTargetAlreadyLoadedException} no matter
 * how early the config is registered. A launchwrapper transformer is the only
 * hook that runs before the class is defined regardless of when it is loaded,
 * so this transformer rewrites {@code renderCachedHud} directly:</p>
 *
 * <ul>
 *   <li>after each {@code renderingCacheOverride} store, mirror the value onto
 *       {@code GLSMConfig.hudCacheOverride} so the GLSM blend/color paths apply
 *       the same overrides StellarCore would have applied;</li>
 *   <li>right before the {@code GuiIngame.renderGameOverlay} call inside the
 *       override window, restore the HUD baseline (alpha test {@code GREATER/0.1}
 *       etc.), since the call site that normally carries the baseline
 *       restoration in {@code EntityRenderer.updateCameraAndRender} is replaced
 *       by StellarCore's {@code @Redirect}.</li>
 * </ul>
 */
public final class StellarCoreHudCachingCompatTransformer implements IClassTransformer {
    private static final Logger LOGGER = LogManager.getLogger("Actinium");

    private static final String TARGET_CLASS =
        "github.kasuminova.stellarcore.client.hudcaching.HUDCaching";
    private static final String TARGET_METHOD = "renderCachedHud";
    private static final String TARGET_METHOD_DESC =
        "(Lnet/minecraft/client/renderer/EntityRenderer;Lnet/minecraft/client/gui/GuiIngame;F)V";

    private static final String OVERRIDE_OWNER = TARGET_CLASS.replace('.', '/');
    private static final String OVERRIDE_FIELD = "renderingCacheOverride";
    private static final String OVERRIDE_DESC = "Z";

    private static final String GLSM_CONFIG_OWNER =
        "com/gtnewhorizons/angelica/glsm/hooks/GLSMConfig";
    private static final String GLSM_CONFIG_FIELD = "hudCacheOverride";
    private static final String GLSM_CONFIG_DESC = "Z";

    private static final String BOUNDARY_OWNER = "com/dhj/actinium/render/GuiGlStateBoundary";
    private static final String BOUNDARY_METHOD = "restoreHudBaseline";
    private static final String BOUNDARY_DESC = "()V";

    private static final String GAME_OVERLAY_OWNER = "net/minecraft/client/gui/GuiIngame";
    private static final String GAME_OVERLAY_METHOD = "renderGameOverlay";
    private static final String GAME_OVERLAY_DESC = "(F)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        boolean transformed = false;

        for (MethodNode method : classNode.methods) {
            if (!TARGET_METHOD.equals(method.name) || !TARGET_METHOD_DESC.equals(method.desc)) {
                continue;
            }
            transformed = rewriteRenderCachedHud(method);
            break;
        }

        if (!transformed) {
            LOGGER.warn(
                "Could not find the expected HudCaching instructions in {}; "
                    + "StellarCore's cached HUD may render incorrectly",
                TARGET_CLASS
            );
            return basicClass;
        }

        // No branches are added and the stack balance is unchanged, so the
        // original stack map frames stay valid; copying them avoids resolving
        // classes while this class is still being defined.
        ClassWriter writer = new ClassWriter(classReader, 0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean rewriteRenderCachedHud(MethodNode method) {
        boolean transformed = false;
        boolean insideOverrideWindow = false;

        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {

            if (instruction.getOpcode() == Opcodes.PUTSTATIC
                && isOverrideStore((FieldInsnNode) instruction)) {
                AbstractInsnNode pushed = instruction.getPrevious();
                int value = pushed != null ? constantValue(pushed) : -1;
                if (value < 0) {
                    LOGGER.warn("renderingCacheOverride store without a constant push in {}; aborting", TARGET_CLASS);
                    return false;
                }
                // The pushed constant was consumed by the original store, so
                // re-push it for the mirrored store (net stack change: zero).
                // InsnList has no "insert after", so anchor on the next node.
                AbstractInsnNode anchor = instruction.getNext();
                InsnList mirror = new InsnList();
                mirror.add(new InsnNode(value == 1 ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                mirror.add(new FieldInsnNode(
                    Opcodes.PUTSTATIC, GLSM_CONFIG_OWNER, GLSM_CONFIG_FIELD, GLSM_CONFIG_DESC));
                if (anchor == null) {
                    method.instructions.add(mirror);
                } else {
                    method.instructions.insertBefore(anchor, mirror);
                }
                insideOverrideWindow = value == 1;
                transformed = true;
                continue;
            }

            if (insideOverrideWindow
                && instruction.getOpcode() == Opcodes.INVOKEVIRTUAL
                && isGameOverlayCall((MethodInsnNode) instruction)) {
                method.instructions.insertBefore(
                    instruction,
                    new MethodInsnNode(Opcodes.INVOKESTATIC, BOUNDARY_OWNER, BOUNDARY_METHOD, BOUNDARY_DESC, false)
                );
                transformed = true;
            }
        }

        return transformed;
    }

    private static boolean isOverrideStore(FieldInsnNode field) {
        return OVERRIDE_OWNER.equals(field.owner)
            && OVERRIDE_FIELD.equals(field.name)
            && OVERRIDE_DESC.equals(field.desc);
    }

    private static boolean isGameOverlayCall(MethodInsnNode call) {
        return GAME_OVERLAY_OWNER.equals(call.owner)
            && GAME_OVERLAY_METHOD.equals(call.name)
            && GAME_OVERLAY_DESC.equals(call.desc);
    }

    private static int constantValue(AbstractInsnNode instruction) {
        if (instruction.getOpcode() == Opcodes.ICONST_0) {
            return 0;
        }
        if (instruction.getOpcode() == Opcodes.ICONST_1) {
            return 1;
        }
        return -1;
    }
}
