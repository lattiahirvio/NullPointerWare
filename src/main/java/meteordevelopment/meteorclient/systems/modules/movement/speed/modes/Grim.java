/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Aura;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.MoveUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;

public final class Grim extends SpeedMode {

    public Grim() {
        super(SpeedModes.Grim);
    }

    @Override
    public void onTick() {
        if (settings.isNull()) return;
        double boost = boost();
        if (boost == 0 || !MoveUtils.hasMovement()) return;
        double yaw = MoveUtils.getDirection(Rotations.serverYaw);
        if (!mc.player.isOnGround() || settings.grimGround.get()) {
            mc.player.addVelocity(-Math.sin(yaw) * boost, 0, Math.cos(yaw) * boost);
        }
    }

    @Override
    public void onMove(final PlayerMoveEvent event) {
        if (boost() != 0) {
            if (settings.grimStrafe.get() && (!mc.player.isOnGround() || settings.grimGround.get())) {
                ((IVec3d) event.movement).set(MoveUtils.strafe(MoveUtils.getSpeed(), settings.grimStrafeAmount.get(), Rotations.serverYaw, event.movement));
            }
        }
    }

    double boost() {
        if (settings.isNull()) return 0;
        double boost = 0;
        Box expand = mc.player.getBoundingBox().expand(1);
        for (Entity entity : mc.world.getEntities()) {
            if (!isSpeedable(entity)) continue;
            Box box = entity.getBoundingBox();
            if (expand.intersects(box)) boost += settings.grimBoost.get();
        }
        return boost;
    }



    private boolean isSpeedable(final Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof MinecartEntity || entity instanceof BoatEntity) return true;
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof FakePlayerEntity) return false;
        return true;
    }
}
