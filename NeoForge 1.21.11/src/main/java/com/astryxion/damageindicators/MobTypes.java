package com.astryxion.damageindicators;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;

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

    MobTypes() {
        texture = Identifier.fromNamespaceAndPath("damageindicators", "textures/gui/mob_types/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public static MobTypes getTypeFor(Entity entity) {
        if (entity instanceof Player) {
            return PLAYER;
        }
        if (entity instanceof LivingEntity living) {
            if (living.getType().is(Tags.EntityTypes.BOSSES)) {
                return BOSS;
            }
            if (living.getType().is(EntityTypeTags.AQUATIC) || living instanceof WaterAnimal || living.canBreatheUnderwater()) {
                if (living.getType().is(EntityTypeTags.ARTHROPOD)) {
                    return WATER_ARTHROPOD;
                }
                return living instanceof Enemy ? WATER_MONSTER : WATER_ANIMAL;
            }
            if (living.getType().is(EntityTypeTags.UNDEAD)) {
                return living instanceof Enemy ? UNDEAD : UNDEAD_ANIMAL;
            }
            if (living.getType().is(EntityTypeTags.ARTHROPOD)) {
                return living instanceof Enemy ? ARTHROPOD_MONSTER : ARTHROPOD;
            }
            if (living.getType().is(EntityTypeTags.ILLAGER) || living instanceof Witch) {
                return ILLAGER;
            }
            if (living instanceof AbstractGolem) {
                return GOLEM;
            }
            if (living instanceof Npc) {
                return VILLAGER;
            }
            if (living instanceof Enemy) {
                return MONSTER;
            }
            if (living instanceof AmbientCreature) {
                return AMBIENT;
            }
            if (living instanceof Mob) {
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
        if (entity.getType().is(Tags.EntityTypes.BOSSES)) {
            return 4;
        }
        if (entity.getType().is(EntityTypeTags.UNDEAD) || entity.fireImmune()) {
            return 0;
        }
        if (entity.getType().is(EntityTypeTags.ARTHROPOD)) {
            return 3;
        }
        if (entity instanceof Player || entity instanceof Witch || entity instanceof Villager || entity instanceof AbstractGolem || entity instanceof Npc) {
            return 2;
        }
        return 1;
    }

    public static boolean isHostileForCleanSkin(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    public Identifier getTexture() {
        return texture;
    }
}
