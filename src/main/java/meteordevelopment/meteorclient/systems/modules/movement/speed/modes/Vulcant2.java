/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.utils.player.MoveUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.sqrt;
import static meteordevelopment.meteorclient.utils.player.PlayerUtils.isMoving;

public final class Vulcant2 extends SpeedMode {
    public Vulcant2() {
        super(SpeedModes.VulcantTest);
    }

    private double speed, level, lastSpeed;

    private boolean touchedGround;

    private int ticks, stage;

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }



    @Override
    public void onUpdate(final SendMovementPacketsEvent.Pre event) {

        double x = mc.player.getVelocity().getX();
        double y = mc.player.getVelocity().getY();
        double z = mc.player.getVelocity().getZ();

        final double xDist = mc.player.getX() - mc.player.prevX;
        final double zDist = mc.player.getZ() - mc.player.prevZ;

        lastSpeed = MathHelper.hypot(xDist, zDist);

        ticks++;
        MoveUtils.resetMotionXYZ();

        if (stage == 0)
        {
            MoveUtils.setTimer(1.0F);
            stage = 1;
        }

        if (!mc.player.isOnGround())
        {
            if (ticks > 3 && mc.player.getVelocity().getY() > 0)
            {
                MoveUtils.motionY(-0.27);
            }
        }

        if (MoveUtils.getSpeed() < 0.22f && !mc.player.isOnGround())
        {
            MoveUtils.setMotion(0.22f);
        }

        if (MoveUtils.isMoving())
        {
            if (mc.player.isOnGround())
            {
                ticks = 0;

                mc.player.jump();
                MoveUtils.setTimer(1.2F);
                stage = 0;

                if (MoveUtils.getSpeed() < 0.48f) {
                    MoveUtils.setMotion(0.48f);
                }
                else
                {
                    MoveUtils.setMotion((MoveUtils.getSpeed() * 0.985f));
                }
            }
        }
        else
        {
            MoveUtils.setTimer(1.0F);
            MoveUtils.resetMotionXZ();
        }

    }

    @Override
    public void onMove(final PlayerMoveEvent event) {

        Vec3d vel = PlayerUtils.getHorizontalVelocity(settings.vanillaSpeed.get());
        double velX = vel.getX();
        double velZ = vel.getZ();

        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            double value = (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1) * 0.205;
            velX += velX * value;
            velZ += velZ * value;
        }

        Anchor anchor = Modules.get().get(Anchor.class);
        if (anchor.isActive() && anchor.controlMovement) {
            velX = anchor.deltaX;
            velZ = anchor.deltaZ;
        }

        ((IVec3d) event.movement).set(velX, event.movement.y, velZ);
    }

    public void strafe(float speed) {
        if (!isMoving()) {
            return;
        }
        double direction = getDirection();
        double x = -Math.sin(direction) * speed;
        double z = Math.cos(direction) * speed;

        Vec3d motion = new Vec3d(x, mc.player.getVelocity().y, z);
        mc.player.setVelocity(motion);
    }

    public void strafe() {
        this.strafe(getSpeed());
    }


    public float getSpeed() {
        return (float) sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
    }

    public double getDirection() {
        double rotationYaw = mc.player.getYaw();
        if (mc.player.input.movementForward < 0) {
            rotationYaw += 180;
        }
        double forward = 1;
        if (mc.player.input.movementForward < 0) {
            forward = -0.5;
        } else if (mc.player.input.movementForward > 0) {
            forward = 0.5;
        }
        if (mc.player.input.movementSideways > 0) {
            rotationYaw -= 90 * forward;
        }
        if (mc.player.input.movementSideways < 0) {
            rotationYaw += 90 * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    private double fallSpeed()
    {
        return mc.player.horizontalCollision ? -0.055 : -(0.1 - 1e-3);
    }

}
