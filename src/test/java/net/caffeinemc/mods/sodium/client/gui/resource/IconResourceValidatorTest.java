package net.caffeinemc.mods.sodium.client.gui.resource;

import net.caffeinemc.mods.sodium.client.config.structure.ColorTheme;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconResourceValidatorTest {
    @Test
    void validatesEveryDeclaredIconBeforeRendering() {
        ResourceLocation first = new ResourceLocation("first", "textures/icon.png");
        ResourceLocation second = new ResourceLocation("second", "textures/icon.png");
        List<ResourceLocation> validated = new ArrayList<>();

        IconResourceValidator.validate(List.of(owner("first", first), owner("plain", null), owner("second", second)),
                validated::add);

        assertEquals(List.of(first, second), validated);
    }

    @Test
    void reportsConfigAndResourcePathOnMissingIcon() {
        ResourceLocation icon = new ResourceLocation("broken", "textures/missing.png");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> IconResourceValidator.validate(List.of(owner("broken", icon)), resource -> {
                    throw new IllegalArgumentException("not found");
                }));

        assertTrue(exception.getMessage().contains("broken:textures/missing.png"));
        assertTrue(exception.getMessage().contains("config 'broken'"));
    }

    private static ModOptions owner(String id, ResourceLocation icon) {
        ExternalPage page = new ExternalPage(new TextComponentString("External"), screen -> { });
        return new ModOptions(id, id, "1.0", ColorTheme.defaultFor(id), icon, true,
                List.of(page), List.of(), List.of(), List.of());
    }
}
