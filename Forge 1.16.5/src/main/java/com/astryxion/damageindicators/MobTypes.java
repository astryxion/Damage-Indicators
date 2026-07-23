package com.astryxion.damageindicators;

import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.WitchEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

public enum MobTypes {
    PLAYER,
    UNDEAD,
    UNDEAD_ANIMAL,
    ANIMAL,
    WATER_ANIMAL,
    AMBIENT,
    MONSTER,
    WATER_MONSTER,
    ARTHROPOD,
    ARTHROPOD_MONSTER,
    WATER_ARTHROPOD,
    GOLEM,
    VILLAGER,
    ILLAGER,
    UNKNOWN,
    BOSS;

    private final ResourceLocation texture;

    MobTypes() {
        texture = new ResourceLocation("damageindicators:textures/gui/mob_types/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static MobTypes getTypeFor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return PLAYER;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;

            ITag<EntityType<?>> bossTag = EntityTypeTags.getAllTags().getTag(new ResourceLocation("forge", "bosses"));
            if (bossTag != null && bossTag.contains(living.getType())) {
                return BOSS;
            }
            CreatureAttribute creatureAttribute = living.getMobType();
            if (creatureAttribute == CreatureAttribute.WATER) {
                return living instanceof MonsterEntity ? WATER_MONSTER : WATER_ANIMAL;
            } else if (creatureAttribute == CreatureAttribute.UNDEAD) {
                return living instanceof MonsterEntity ? UNDEAD : UNDEAD_ANIMAL;
            } else if (creatureAttribute == CreatureAttribute.ARTHROPOD) {
                return living instanceof WaterMobEntity || living.canBreatheUnderwater() ? WATER_ARTHROPOD : living instanceof MonsterEntity ? ARTHROPOD_MONSTER : ARTHROPOD;
            } else if (creatureAttribute == CreatureAttribute.ILLAGER) {
                return ILLAGER;
            }
            if (living instanceof GolemEntity) {
                return GOLEM;
            }
            if (living instanceof AbstractVillagerEntity) {
                return VILLAGER;
            }
            // Slimes/magma cubes implement IMob but are not MonsterEntity in 1.16.5.
            if (living instanceof MonsterEntity || living instanceof IMob) {
                return MONSTER;
            }
            if (living instanceof AmbientEntity) {
                return AMBIENT;
            }
            if (living instanceof CreatureEntity || living instanceof MobEntity) {
                return ANIMAL;
            }
        }
        return UNKNOWN;
    }

    /**
     * Icon column index into the original DITypeIcons.png atlas (5 columns), matching Damage Indicators 1.12.2.
     * 0 = undead/fire-immune, 1 = default living, 2 = humanoid, 3 = arthropod, 4 = boss.
     */
    public static int getCleanSkinIconIndex(LivingEntity entity) {
        ITag<EntityType<?>> bossTag = EntityTypeTags.getAllTags().getTag(new ResourceLocation("forge", "bosses"));
        if (bossTag != null && bossTag.contains(entity.getType())) {
            return 4;
        }
        if (entity.getMobType() == CreatureAttribute.UNDEAD || entity.fireImmune()) {
            return 0;
        }
        if (entity.getMobType() == CreatureAttribute.ARTHROPOD) {
            return 3;
        }
        if (entity instanceof PlayerEntity || entity instanceof WitchEntity || entity instanceof AbstractVillagerEntity || entity instanceof GolemEntity) {
            return 2;
        }
        return 1;
    }

    public static boolean isHostileForCleanSkin(LivingEntity entity) {
        ITag<EntityType<?>> bossTag = EntityTypeTags.getAllTags().getTag(new ResourceLocation("forge", "bosses"));
        return entity instanceof MonsterEntity || entity instanceof IMob
                || (bossTag != null && bossTag.contains(entity.getType()));
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}
