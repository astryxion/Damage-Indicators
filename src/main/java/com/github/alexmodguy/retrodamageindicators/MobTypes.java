package com.github.alexmodguy.retrodamageindicators;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;

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
    MobTypes(){
        texture = ResourceLocation.fromNamespaceAndPath("retrodamageindicators", "textures/gui/mob_types/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static MobTypes getTypeFor(Entity entity){
        if(entity instanceof Player){
            return PLAYER;
        }
        if(entity instanceof LivingEntity living){
            if (living.getType() == EntityType.ENDER_DRAGON || living.getType() == EntityType.WITHER || living instanceof Warden) {
                return BOSS;
            }
            if (living instanceof WaterAnimal) {
                return living instanceof Enemy ? WATER_MONSTER : WATER_ANIMAL;
            }
            if (isInTag(living, EntityTypeTags.UNDEAD)) {
                return living instanceof Enemy ? UNDEAD : UNDEAD_ANIMAL;
            }
            if (isInTag(living, EntityTypeTags.ARTHROPOD)) {
                return living instanceof WaterAnimal || living.canBreatheUnderwater() ? WATER_ARTHROPOD : living instanceof Enemy ? ARTHROPOD_MONSTER : ARTHROPOD;
            }
            if (isInTag(living, EntityTypeTags.RAIDERS)) {
                return ILLAGER;
            }
            if(living instanceof AbstractGolem){
                return GOLEM;
            }
            if(living instanceof Npc){
                return VILLAGER;
            }
            if(living instanceof Enemy){
                return MONSTER;
            }
            if(living instanceof AmbientCreature){
                return AMBIENT;
            }
            if(living instanceof Mob){
                return ANIMAL;
            }
        }
        return UNKNOWN;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    /** 1.21.1: MobType was removed; equivalent check using entity type tags. */
    private static boolean isInTag(LivingEntity living, net.minecraft.tags.TagKey<EntityType<?>> tagKey) {
        return BuiltInRegistries.ENTITY_TYPE.getTag(tagKey)
                .stream()
                .flatMap(h -> h.stream())
                .anyMatch(holder -> holder.value() == living.getType());
    }
}
