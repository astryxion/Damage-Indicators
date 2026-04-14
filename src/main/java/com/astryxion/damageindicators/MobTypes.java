package com.astryxion.damageindicators;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;

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

    private final Identifier texture;
    MobTypes(){
        texture = Identifier.fromNamespaceAndPath("damageindicators", "textures/gui/mob_types/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static MobTypes getTypeFor(Entity entity){
        if(entity instanceof Player){
            return PLAYER;
        }
        if(entity instanceof LivingEntity living){
            if(living.getType().builtInRegistryHolder().is(ConventionalEntityTypeTags.BOSSES)){
                return BOSS;
            }
            if(living.getType().builtInRegistryHolder().is(EntityTypeTags.AQUATIC)){
                return living instanceof Enemy ? WATER_MONSTER : WATER_ANIMAL;
            }else if(living.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)){
                return living instanceof Enemy ? UNDEAD : UNDEAD_ANIMAL;
            }else if(living.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD)){
                return living instanceof WaterAnimal || living.canBreatheUnderwater() ? WATER_ARTHROPOD : living instanceof Enemy ? ARTHROPOD_MONSTER : ARTHROPOD;
            }else if(living.getType().builtInRegistryHolder().is(EntityTypeTags.ILLAGER)){
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

    public Identifier getTexture() {
        return texture;
    }
}
