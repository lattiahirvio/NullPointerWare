/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.MoveUtils;
import net.minecraft.text.Text;

public final class MineMenClub extends SpeedMode {

    public MineMenClub() {
        super(SpeedModes.MineMenClub);
    }


    @Override
    public void onActivate() {
        ChatUtils.addMessage(Text.literal("MMC speed isn't here :)\n go look somewhere else..."));
    }
}
