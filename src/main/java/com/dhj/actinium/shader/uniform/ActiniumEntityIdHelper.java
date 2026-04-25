package com.dhj.actinium.shader.uniform;

import com.dhj.actinium.shader.pack.ActiniumNamespacedId;
import com.dhj.actinium.shader.pack.ActiniumShaderPackManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.player.EntityPlayer;

public final class ActiniumEntityIdHelper {
    private static final ActiniumNamespacedId CONVERTING_VILLAGER = new ActiniumNamespacedId("minecraft", "zombie_villager_converting");
    private static final ActiniumNamespacedId CURRENT_PLAYER = new ActiniumNamespacedId("minecraft", "current_player");
    private static final ActiniumNamespacedId LIGHTNING_BOLT = new ActiniumNamespacedId("minecraft", "lightning_bolt");

    private static final Object2IntMap<Class<?>> ENTITY_ID_CACHE = new Object2IntOpenHashMap<>();
    private static Object2IntMap<ActiniumNamespacedId> cachedEntityIdMap;

    static {
        ENTITY_ID_CACHE.defaultReturnValue(Integer.MIN_VALUE);
    }

    private ActiniumEntityIdHelper() {
    }

    public static int getEntityId(Entity entity) {
        Object2IntMap<ActiniumNamespacedId> entityIds = ActiniumShaderPackManager.getActiveIdMap().getEntityIds();
        if (entityIds == null || entityIds.isEmpty()) {
            return -1;
        }

        if (entityIds != cachedEntityIdMap) {
            ENTITY_ID_CACHE.clear();
            cachedEntityIdMap = entityIds;
        }

        int specialId = getSpecialEntityId(entity, entityIds);
        if (specialId != -1) {
            return specialId;
        }

        return getNormalEntityId(entity, entityIds);
    }

    private static int getSpecialEntityId(Entity entity, Object2IntMap<ActiniumNamespacedId> entityIds) {
        Entity cameraEntity = Minecraft.getMinecraft().getRenderViewEntity();
        if (entity == cameraEntity && entity instanceof EntityPlayer) {
            int currentPlayerId = entityIds.getOrDefault(CURRENT_PLAYER, -1);
            if (currentPlayerId != -1) {
                return currentPlayerId;
            }
        }

        if (entity instanceof EntityZombieVillager zombieVillager && zombieVillager.isConverting()) {
            return entityIds.getOrDefault(CONVERTING_VILLAGER, -1);
        }

        return -1;
    }

    private static int getNormalEntityId(Entity entity, Object2IntMap<ActiniumNamespacedId> entityIds) {
        Class<?> entityClass = entity.getClass();
        int cached = ENTITY_ID_CACHE.getInt(entityClass);
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int resolved = resolveEntityId(entity, entityClass, entityIds);
        ENTITY_ID_CACHE.put(entityClass, resolved);
        return resolved;
    }

    private static int resolveEntityId(Entity entity, Class<?> entityClass, Object2IntMap<ActiniumNamespacedId> entityIds) {
        String entityType = EntityList.getEntityString(entity);
        if (entityType != null) {
            int mapped = entityIds.getOrDefault(new ActiniumNamespacedId(entityType), -1);
            if (mapped != -1) {
                return mapped;
            }
        }

        int mapped = entityIds.getOrDefault(new ActiniumNamespacedId(entityClass.getSimpleName()), -1);
        if (mapped != -1) {
            return mapped;
        }

        mapped = entityIds.getOrDefault(new ActiniumNamespacedId(entityClass.getName()), -1);
        if (mapped != -1) {
            return mapped;
        }

        if (entity instanceof EntityLightningBolt) {
            return entityIds.getOrDefault(LIGHTNING_BOLT, -1);
        }

        return -1;
    }
}
