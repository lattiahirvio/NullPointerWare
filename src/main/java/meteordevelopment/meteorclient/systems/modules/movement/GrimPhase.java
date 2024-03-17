/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Objects;

public final class GrimPhase extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> spider = sgGeneral.add(new BoolSetting.Builder()
        .name("spider")
        .description("Helps you with spider, adds extra block check.")
        .defaultValue(true)
        .build()
    );
    public GrimPhase() {
        super(Categories.Movement, "grim-phase", "phase on top!!");
    }

    @EventHandler
    private void onCollision(final CollisionShapeEvent e) {
        if (isPhasePosition(e.pos, e.state.getBlock())) {
            sendNoEvent(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, e.pos, Direction.DOWN));
            e.shape = VoxelShapes.empty();
        }
    }

    // Code is a mess, so we don't send 1000 packets per second
    private boolean isPhasePosition(BlockPos pos, Block block) {
        BlockPos player = mc.player.getBlockPos();
        BlockPos forward = player.offset(mc.player.getHorizontalFacing());
        if (block == Blocks.AIR) return false;
        if (Objects.equals(pos, forward) || Objects.equals(pos, forward.up())) return true;
        if (spider.get() && Objects.equals(pos, forward.up().up())) return true;
        if (Objects.equals(pos, player) || Objects.equals(pos, player.up()) || Objects.equals(pos, player.up().up())) return true;
        if ((mc.player.isSneaking() && Objects.equals(pos, player.down())) || (mc.player.isSneaking() && Objects.equals(pos, forward.down()))) return true;
        return false;
    }
}
