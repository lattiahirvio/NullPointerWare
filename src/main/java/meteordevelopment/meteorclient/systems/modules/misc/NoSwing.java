/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public final class NoSwing extends Module {
    public NoSwing() {
        super(Categories.Misc, "no-swing", "Cancels swinging.");
    }

    @EventHandler
    private void onPacketSend(final PacketEvent.Send event) { if (event.packet instanceof HandSwingC2SPacket) event.cancel(); }
}
