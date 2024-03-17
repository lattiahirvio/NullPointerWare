/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;

public final class SwingSpam extends Module {
    public SwingSpam() {
        super(Categories.Misc, "swing-spam", "Spams swing so no one notices real swings");
    }

    @EventHandler
    private void onTickPre(final TickEvent.Pre event) {
        if (mc.world != null && mc.player != null) sendNoEvent(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }
}
