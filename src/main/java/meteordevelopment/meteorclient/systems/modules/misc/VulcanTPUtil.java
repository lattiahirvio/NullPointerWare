/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.VulcanTP;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public final class VulcanTPUtil extends Module {
    public VulcanTPUtil() {
        super(Categories.Misc, "vulcan-tp-util", "Needs funny usage.");
    }


    int ticks = -1;

    Vec3d tpPos;

    public void setPos(Vec3d vec) {
        tpPos = vec;
        ticks = (int) (Math.ceil(Math.sqrt(mc.player.squaredDistanceTo(tpPos)) / 10) * 5);
    }


    @EventHandler
    private void onMove(final PlayerMoveEvent event) {
        if (ticks > 0)
            event.cancel();
    }

    @EventHandler
    private void onPacketSend(final PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround && ticks > 0) {
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(final TickEvent.Pre e){
        if (ticks > 0) ticks--;
        else if (ticks != -1){
            for (int i = 0; i < Math.ceil(Math.sqrt(mc.player.squaredDistanceTo(tpPos)) / 10); i++) {
                emptyFull();
            }

            sendNoEvent(new PlayerMoveC2SPacket.Full(tpPos.x, tpPos.y, tpPos.z, mc.player.getYaw(), mc.player.getPitch(), true));
            mc.player.setPosition(tpPos);
            ticks = -1;
        }
    }

}
