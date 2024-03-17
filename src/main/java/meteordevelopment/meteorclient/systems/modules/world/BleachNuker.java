/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.events.entity.player.MovementInputToVelocityEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ImmutablePairList;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import net.minecraft.world.RaycastContext;


public final class BleachNuker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgRender = settings.createGroup("Render");

    public BlockPos bpos = null;
    private Set<BlockPos> renderBlocks = new HashSet<>();
    ;

    // General

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
        .name("shape")
        .description("The shape of nuking algorithm.")
        .defaultValue(Shape.Sphere)
        .build()
    );

    private final Setting<BleachNuker.Mode> mode = sgGeneral.add(new EnumSetting.Builder<BleachNuker.Mode>()
        .name("mode")
        .description("The way the blocks are broken.")
        .defaultValue(Mode.SurvMulti)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The break range.")
        .defaultValue(4)
        .min(0)
        .visible(() -> shape.get() != Shape.Cube)
        .build()
    );

    private final Setting<Boolean> filter = sgGeneral.add(new BoolSetting.Builder()
        .name("Filter")
        .description("Makes the nuker not break shit.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> flatten = sgGeneral.add(new BoolSetting.Builder()
        .name("Flatten")
        .description("Makes the nuker not break shit.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between breaking blocks.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to break per tick. Useful when insta mining.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<BleachNuker.SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<BleachNuker.SortMode>()
        .name("sort-mode")
        .description("The blocks you want to mine first.")
        .defaultValue(BleachNuker.SortMode.Closest)
        .build()
    );

    private final Setting<Boolean> idkanymore = sgGeneral.add(new BoolSetting.Builder()
        .name("idk anymore man")
        .description("")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> raycast = sgGeneral.add(new BoolSetting.Builder()
        .name("raycast")
        .description("Attempt to instamine everything at once.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Attempt to instamine everything at once.")
        .defaultValue(false)
        .build()
    );

    private final Setting<RotationMode> rotate = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotate")
        .description("Rotates server-side to the block being mined.")
        .defaultValue(RotationMode.Vanilla)
        .build()
    );

    private final Setting<Nuker.ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<Nuker.ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(Nuker.ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("The blocks you don't want to mine.")
        .visible(() -> listMode.get() == Nuker.ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The blocks you want to mine.")
        .visible(() -> listMode.get() == Nuker.ListMode.Whitelist)
        .build()
    );


    // Rendering
    private final Setting<Boolean> enableRenderBounding = sgRender.add(new BoolSetting.Builder()
        .name("bounding-box")
        .description("Enable rendering bounding box for Cube and Uniform Cube.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> circle = sgRender.add(new BoolSetting.Builder()
        .name("circle")
        .description("Render a circle over the player :) ")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> circleColor = sgRender.add(new ColorSetting.Builder()
        .name("circle-color")
        .description("Circle color!")
        .defaultValue(new SettingColor(16, 106, 144, 100))
        .visible(circle::get)
        .build()
    );

    private final Setting<ShapeMode> shapeModeBox = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("nuke-box-mode")
        .description("How the shape for the bounding box is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColorBox = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the bounding box.")
        .defaultValue(new SettingColor(16, 106, 144, 100))
        .build()
    );

    private final Setting<SettingColor> lineColorBox = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the bounding box.")
        .defaultValue(new SettingColor(16, 106, 144, 255))
        .build()
    );

    private final Setting<Boolean> enableRenderBreaking = sgRender.add(new BoolSetting.Builder()
        .name("broken-blocks")
        .description("Enable rendering bounding box for Cube and Uniform Cube.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeModeBreak = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("nuke-block-mode")
        .description("How the shapes for broken blocks are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(enableRenderBreaking::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 80))
        .visible(enableRenderBreaking::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(enableRenderBreaking::get)
        .build()
    );

    private final Pool<BlockPos.Mutable> blockPosPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private boolean firstBlock;
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();

    private int timer;
    private int noBlockTimer;

    private BlockPos.Mutable pos1 = new BlockPos.Mutable(); // Rendering for cubes
    private BlockPos.Mutable pos2 = new BlockPos.Mutable();
    private Box box;
    int maxh = 0;
    int maxv = 0;
    boolean shouldBreak;

    public BleachNuker() {
        super(Categories.World, "Bleach Nuker", "Breaks blocks around you.");
    }

    @Override
    public void onActivate() {
        firstBlock = true;
        timer = 0;
        noBlockTimer = 0;
        shouldBreak = false;
    }

    @EventHandler
    private void onTickPre(final TickEvent.Pre event) {
        renderBlocks.clear();

        double rangeS = range.get();

        ImmutablePairList<BlockPos, Pair<Vec3d, Direction>> blocks = new ImmutablePairList<>();

        // Add blocks around player
        boolean filterToggler = filter.get();

        for (int x = MathHelper.ceil(rangeS); x >= MathHelper.floor(-rangeS); x--) {
            for (int y = MathHelper.ceil(rangeS); y >= (flatten.get() ? -mc.player.getEyeHeight(mc.player.getPose()) + 0.2 : MathHelper.floor(-rangeS)); y--) {
                for (int z = MathHelper.ceil(rangeS); z >= MathHelper.floor(-rangeS); z--) {
                    BlockPos pos = BlockPos.ofFloored(mc.player.getEyePos().add(x, y, z));

                    double distTo = shape.get().equals(Shape.Cube)
                        ? MathHelper.absMax(MathHelper.absMax(mc.player.getX() - (pos.getX() + 0.5), mc.player.getEyeY() - (pos.getY() + 0.5)), mc.player.getZ() - (pos.getZ() + 0.5))
                        : mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));

                    BlockState state = mc.world.getBlockState(pos);
                    if (distTo - 0.5 > rangeS || state.isAir() || state.getBlock() instanceof FluidBlock)
                        continue;

                    if (filterToggler) {
                        boolean contains = whitelist.get().contains(state.getBlock());

                        if ((listMode.equals(Nuker.ListMode.Whitelist) && contains) || (listMode.equals(Nuker.ListMode.Blacklist) && !contains)) {
                            continue;
                        }
                    }

                    Pair<Vec3d, Direction> vec = getBlockAngle(pos);

                    if (vec != null) {
                        blocks.add(pos, vec);
                    } else if (!raycast.get()) {
                        blocks.add(pos, Pair.of(Vec3d.ofCenter(pos), Direction.UP));
                    }
                }
            }
        }

        if (blocks.isEmpty())
            return;

        blocks.sortByKey(getBlockOrderComparator());

        int broken = 0;
        for (ImmutablePair<BlockPos, Pair<Vec3d, Direction>> pos : blocks) {
            float breakingDelta = mc.world.getBlockState(pos.getKey()).calcBlockBreakingDelta(mc.player, mc.world, pos.getKey());

            // Unbreakable block
            if (mc.interactionManager.getCurrentGameMode().isSurvivalLike() && breakingDelta == 0) {
                continue;
            }

            if (mode.get().equals(Mode.SurvMulti) && breakingDelta <= 1f && broken > 0) {
                return;
            }

            if (rotate.get().equals(RotationMode.None)) {
                Vec3d v = pos.getValue().getLeft();
                facePosAuto(v.x, v.y, v.z, (idkanymore.get() ? 1 : 0));
            }

            mc.interactionManager.updateBlockBreakingProgress(pos.getKey(), pos.getValue().getRight());
            renderBlocks.add(pos.getKey());

            mc.player.swingHand(Hand.MAIN_HAND);

            broken++;
            if (mode.get().equals(Mode.Normal)
                || (mode.get().equals(Mode.SurvMulti) && breakingDelta <= 1f)
                || (mode.get().equals(Mode.SurvMulti) && broken >= maxBlocksPerTick.get() )
                || (mode.get().equals(Mode.Multi) && broken >= maxBlocksPerTick.get())) {
                return;
            }
        }

    }

    @EventHandler
    private void onRender(final Render3DEvent e) {
        if (mc.player == null || mc.world == null)
            return;

        if (enableRenderBounding.get()) {
            // Render bounding box if cube and should break stuff
            if (shape.get() != Shape.Sphere && mode.get() != Mode.Instant) {
                box = new Box(pos1, pos2);
                e.renderer.box(box, sideColorBox.get(), lineColorBox.get(), shapeModeBox.get(), 0);
            }
        }

        Vec3d pos = mc.player.getPos().subtract(RenderUtils.getInterpolationOffset(mc.player));

        double lastX = 0;
        double lastZ = range.get();
        for (int angle = 0; angle <= 360; angle += 6) {
            float cos = MathHelper.cos((float) Math.toRadians(angle));
            float sin = MathHelper.sin((float) Math.toRadians(angle));

            double x = range.get() * sin;
            double z = range.get() * cos;

            e.renderer.line(
                pos.x + lastX, pos.y + 1, pos.z + lastZ,
                pos.x + x, pos.y + 1, pos.z + z, circleColor.get()
            );
            lastX = x;
            lastZ = z;
        }
    }

    private Pair<Vec3d, Direction> getBlockAngle(BlockPos pos) {
        for (Direction d: Direction.values()) {
            if (!mc.world.getBlockState(pos.offset(d)).isFullCube(mc.world, pos.offset(d))) {
                Vec3d vec = getLegitLookPos(pos, d, true, 5);

                if (vec != null) {
                    return Pair.of(vec, d);
                }
            }
        }

        return null;
    }

    public Vec3d getLegitLookPos(BlockPos pos, Direction dir, boolean raycast, int res) {
        return getLegitLookPos(new Box(pos), dir, raycast, res, 0.01);
    }

    public void facePosAuto(double x, double y, double z, int a) {
        if (a == 0) {
            facePosPacket(x, y, z);
        } else {
            facePos(x, y, z);
        }
    }

    public void facePos(double x, double y, double z) {
        float[] rot = getViewingRotation(mc.player, x, y, z);

        mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(rot[0] - mc.player.getYaw()));
        mc.player.setPitch(mc.player.getPitch() + MathHelper.wrapDegrees(rot[1] - mc.player.getPitch()));
    }

    public void facePosPacket(double x, double y, double z) {
        float[] rot = getViewingRotation(mc.player, x, y, z);

        if (!mc.player.hasVehicle()) {
            mc.player.headYaw = mc.player.getYaw() + MathHelper.wrapDegrees(rot[0] - mc.player.getYaw());
            mc.player.bodyYaw = mc.player.headYaw;
            mc.player.renderPitch = mc.player.getPitch() + MathHelper.wrapDegrees(rot[1] - mc.player.getPitch());
        }

        mc.player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.LookAndOnGround(
                mc.player.getYaw() + MathHelper.wrapDegrees(rot[0] - mc.player.getYaw()),
                mc.player.getPitch() + MathHelper.wrapDegrees(rot[1] - mc.player.getPitch()), mc.player.isOnGround()));
    }

    public static float[] getViewingRotation(Entity entity, double x, double y, double z) {
        double diffX = x - entity.getX();
        double diffY = y - entity.getEyeY();
        double diffZ = z - entity.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return new float[] {
            (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f,
            (float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) };
    }

    public Vec3d getLegitLookPos(Box box, Direction dir, boolean raycast, int res, double extrude) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d blockPos = new Vec3d(box.minX, box.minY, box.minZ).add(
            (dir == Direction.WEST ? -extrude : dir.getOffsetX() * box.getLengthX() + extrude),
            (dir == Direction.DOWN ? -extrude : dir.getOffsetY() * box.getLengthY() + extrude),
            (dir == Direction.NORTH ? -extrude : dir.getOffsetZ() * box.getLengthZ() + extrude));

        for (double i = 0; i <= 1; i += 1d / (double) res) {
            for (double j = 0; j <= 1; j += 1d / (double) res) {
                Vec3d lookPos = blockPos.add(
                    (dir.getAxis() == Direction.Axis.X ? 0 : i * box.getLengthX()),
                    (dir.getAxis() == Direction.Axis.Y ? 0 : dir.getAxis() == Direction.Axis.Z ? j * box.getLengthY() : i * box.getLengthY()),
                    (dir.getAxis() == Direction.Axis.Z ? 0 : j * box.getLengthZ()));

                if (eyePos.distanceTo(lookPos) > 4.55)
                    continue;

                if (raycast) {
                    if (mc.world.raycast(new RaycastContext(eyePos, lookPos,
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                        return lookPos;
                    }
                } else {
                    return lookPos;
                }
            }
        }

        return null;
    }

    @EventHandler
    private void onMoveCorrection(final MovementInputToVelocityEvent e) {
        if (bpos != null && rotate.get().equals(RotationMode.Grim)) {
            Rotations.rotate(Rotations.getYaw(bpos), Rotations.getPitch(bpos));
            e.yaw = (float) Rotations.getYaw(bpos);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        event.cooldown = 0;
    }

    private void breakBlock(final BlockPos blockPos) {



        if (!BlockUtils.isBed(blockPos))
            return;

        if (packetMine.get()) {
            if (!BlockUtils.canBreak(blockPos.north()) ||
                !BlockUtils.canBreak(blockPos.east()) ||
                !BlockUtils.canBreak(blockPos.south()) ||
                !BlockUtils.canBreak(blockPos.west()) ||
                !BlockUtils.canBreak(blockPos.up()) ||
                !BlockUtils.canBreak(blockPos.down())) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
            } else {

                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos.add(0, 1, 0), BlockUtils.getDirection(blockPos)));
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos.add(0, 1, 0), BlockUtils.getDirection(blockPos)));
            }
        } else {
            if (!BlockUtils.canBreak(blockPos.north()) ||
                !BlockUtils.canBreak(blockPos.east()) ||
                !BlockUtils.canBreak(blockPos.south()) ||
                !BlockUtils.canBreak(blockPos.west()) ||
                !BlockUtils.canBreak(blockPos.up()) ||
                !BlockUtils.canBreak(blockPos.down())) {
                BlockUtils.breakBlock(blockPos, true);
            } else {
                BlockUtils.breakBlock(blockPos.add(0, 1, 0), true);
            }
        }
    }

    public static double maxDist(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Gets the largest X, Y or Z difference, manhattan style
        double dX = Math.ceil(Math.abs(x2 - x1));
        double dY = Math.ceil(Math.abs(y2 - y1));
        double dZ = Math.ceil(Math.abs(z2 - z1));
        return Math.max(Math.max(dX, dY), dZ);
    }

    private Comparator<BlockPos> getBlockOrderComparator() {
        // Comparator that moves the block under the player to last
        // so it doesn't mine itself down without clearing everything above first
        Comparator<BlockPos> keepBlockUnderComparator = Comparator.comparing(BlockPos.ofFloored(mc.player.getPos().add(0, -0.8, 0))::equals);

        Comparator<BlockPos> distComparator = Comparator.comparingDouble(b -> mc.player.getEyePos().distanceTo(Vec3d.ofCenter(b)));
        Comparator<BlockPos> hardnessComparator = Comparator.comparing(b -> mc.world.getBlockState(b).getHardness(mc.world, b));

        return switch (sortMode.get()) {
            case Closest -> keepBlockUnderComparator.thenComparing(distComparator);
            case Furthest -> keepBlockUnderComparator.thenComparing(distComparator.reversed());
            case Softest -> keepBlockUnderComparator.thenComparing(hardnessComparator);
            case Hardest -> keepBlockUnderComparator.thenComparing(hardnessComparator.reversed());
            default -> keepBlockUnderComparator;
        };
    }

    public enum Mode {
        Normal,
        SurvMulti,
        Multi,
        Instant
    }

    public enum SortMode {
        None,
        Closest,
        Furthest,
        Softest,
        Hardest,
    }

    public enum Shape {
        Cube,
        UniformCube,
        Sphere
    }

    public enum RotationMode {
        None,
        Vanilla,
        Grim
    }
}
