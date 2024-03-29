/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.packets;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.network.packet.Packet;

public class PacketEvent {
    public static class Receive extends Cancellable {
        private static final Receive INSTANCE = new Receive();

        public Packet<?> packet;

        public static Receive get(Packet<?> packet) {
            INSTANCE.setCancelled(false);
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }

    public static class Recieved {
        private static final Recieved INSTANCE = new Recieved();

        public Packet<?> packet;

        public static Recieved get(Packet<?> packet) {
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }

    public static class Send extends Cancellable {
        private static final Send INSTANCE = new Send();

        public Packet<?> packet;

        public static Send get(Packet<?> packet) {
            INSTANCE.setCancelled(false);
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }

    public static class Sent {
        private static final Sent INSTANCE = new Sent();

        public Packet<?> packet;

        public static Sent get(Packet<?> packet) {
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }

    public static class SendBypass extends Cancellable {
        private static final SendBypass INSTANCE = new SendBypass();

        public Packet<?> packet;

        public static SendBypass get(Packet<?> packet) {
            INSTANCE.setCancelled(false);
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }

    public static class SentBypass {
        private static final SentBypass INSTANCE = new SentBypass();

        public Packet<?> packet;

        public static SentBypass get(Packet<?> packet) {
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }
}
