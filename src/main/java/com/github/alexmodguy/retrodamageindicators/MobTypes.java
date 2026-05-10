package com.github.alexmodguy.retrodamageindicators;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
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
        texture = new ResourceLocation("retrodamageindicators:textures/gui/mob_types/" + name().toLowerCase(Locale.ROOT) + ".png");
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
            if (living instanceof MonsterEntity) {
                return MONSTER;
            }
            if (living instanceof AmbientEntity) {
                return AMBIENT;
            }
            if (living instanceof CreatureEntity && !(living instanceof MonsterEntity)) {
                return ANIMAL;
            }
        }
        return UNKNOWN;
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}
