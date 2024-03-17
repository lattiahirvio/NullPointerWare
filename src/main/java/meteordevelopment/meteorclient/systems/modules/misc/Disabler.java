package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CopyOnWriteArrayList;

public final class Disabler extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
   // private final SettingGroup sgModes = settings.createGroup("Modes"); //Pog

    public Disabler() {
        super(Categories.Movement, "NoAntiCheat", "Removes the Anticheat...");
    }

    public final Setting<DisablerMode> mode = sgGeneral.add(new EnumSetting.Builder<DisablerMode>()
        .name("mode")
        .description("The mode for the Disabler.")
        .defaultValue(DisablerMode.Grim)
        .build()
    );

    public final Setting<Boolean> scaffoldDisabler = sgGeneral.add(new BoolSetting.Builder()
        .name("Sprint disabler")
        .description("Sprint disabler.")
        .defaultValue(true)
        .visible(() -> mode.get().equals(DisablerMode.VulcanSprint))
        .build()
    );

    public final Setting<Integer> vulcanTickTimer = sgGeneral.add(new IntSetting.Builder()
        .name("Ticks")
        .description("How many seconds to wait between sending funny packet")
        .defaultValue(19)
        .sliderMin(0)
        .sliderMax(50)
        .visible(() -> (mode.get().equals(DisablerMode.VulcanScaffold)) || scaffoldDisabler.get())
        .build()
    );

    public final Setting<Boolean> sprintDisabler = sgGeneral.add(new BoolSetting.Builder()
        .name("Sprint disabler")
        .description("Sprint disabler.")
        .defaultValue(true)
        .visible(() -> mode.get().equals(DisablerMode.VulcanScaffold) || scaffoldDisabler.get())
        .build()
    );

    public final Setting<Boolean> autodisable = sgGeneral.add(new BoolSetting.Builder()
        .name("autodisable")
        .description("Autodisable.")
        .defaultValue(true)
        .visible(() -> mode.get().equals(DisablerMode.VulcanFullPing) || mode.get().equals(DisablerMode.VulcanFullBoth) || mode.get().equals(DisablerMode.VulcanFullScreenHandler))
        .build()
    );

    public final Setting<Boolean> spoofOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-onground")
        .description("Spoofs onground.")
        .defaultValue(true)
        .build()
    );

    public final Setting<onGroundEnum> onGroundMode = sgGeneral.add(new EnumSetting.Builder<onGroundEnum>()
        .name("mode")
        .description("The mode for the Disabler.")
        .defaultValue(onGroundEnum.ON_GROUND)
        .visible(spoofOnGround::get)
        .build()
    );

    public final Setting<Boolean> redo = sgGeneral.add(new BoolSetting.Builder()
        .name("minemalia-redo")
        .description("Redoes the disabler on minemalia.")
        .defaultValue(true)
        .visible(() -> (mode.get().equals(DisablerMode.VulcanFullPing) || mode.get().equals(DisablerMode.VulcanFullScreenHandler) || mode.get().equals(DisablerMode.VulcanFullBoth)) && autodisable.get())
        .build()
    );

    public final Setting<Integer> seconds = sgGeneral.add(new IntSetting.Builder()
        .name("seconds")
        .description("How many seconds to wait before disable.")
        .defaultValue(10)
        .sliderMin(0)
        .sliderMax(50)
        .visible(() -> (mode.get().equals(DisablerMode.VulcanFullPing) || mode.get().equals(DisablerMode.VulcanFullScreenHandler) || mode.get().equals(DisablerMode.VulcanFullBoth)) && autodisable.get())
        .build()
    );

    CopyOnWriteArrayList<ScreenHandlerSlotUpdateS2CPacket> packetListTransac = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<CommonPingS2CPacket> packetListPing = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<CommonPingS2CPacket> grimPacketQueue = new CopyOnWriteArrayList<>();

    private boolean delay = false;

    //vulcan
    public int vulcanfullticks = 0;
    public int vulcanScaffoldTicks = 0;


    @Override
    public void onDeactivate() {
        for (ScreenHandlerSlotUpdateS2CPacket packet : packetListTransac) {
            packet.apply(mc.getNetworkHandler());
        }
        for (CommonPingS2CPacket packet : packetListPing) {
            packet.apply(mc.getNetworkHandler());
        }

        for (CommonPingS2CPacket packet : grimPacketQueue) packet.apply(mc.getNetworkHandler());

        grimPacketQueue.clear();
        delay = false;

        packetListTransac.clear();
        packetListPing.clear();
        vulcanfullticks = 0;
    }

    @Override
    public String getInfoString() {
        return mode.get().toString();
    }

    @Override
    public void onActivate() {
    }

    @EventHandler
    private void onPacket(final PacketEvent.Receive e) {
        Packet<?> p = e.packet;

        if ((mode.get().equals(DisablerMode.VulcanFullScreenHandler) || mode.get().equals(DisablerMode.VulcanFullBoth)) && e.packet instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            packetListTransac.add(packet);
            e.setCancelled(true);
        }
        if ((mode.get().equals(DisablerMode.VulcanFullPing) || mode.get().equals(DisablerMode.VulcanFullBoth)) && e.packet instanceof CommonPingS2CPacket pingPacket) {
            packetListPing.add(pingPacket);
            e.setCancelled(true);
        }

        if (mode.get().equals(DisablerMode.CancelPayload) && e.packet instanceof CustomPayloadS2CPacket) {
            e.cancel();
        }

    }


    @EventHandler
    private void onPacket(final PacketEvent.Send event) {
        if (isNull()) return;
        Packet<?> packet = event.packet;
        if (spoofOnGround.get()) {
            if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(onGroundMode.get().getValue());
            }

            if (packet instanceof PlayerMoveC2SPacket.Full) {
                ((PlayerMoveC2SPacketAccessor) packet).setOnGround(onGroundMode.get().getValue());
            }
        }

        if (mode.get().equals(DisablerMode.GrimPlace)) {
            if (event.packet instanceof PlayerInteractBlockC2SPacket p &&  (p).getBlockHitResult().getBlockPos().getSquaredDistance(mc.player.getBlockPos()) >= 0 && p.getBlockHitResult().getBlockPos().getSquaredDistance(mc.player.getBlockPos()) <= 5) {
                event.cancel();
                BlockHitResult hitresult = new BlockHitResult(p.getBlockHitResult().getPos(), Direction.byId( 6 + p.getBlockHitResult().getSide().getId() * 7), p.getBlockHitResult().getBlockPos(), p.getBlockHitResult().isInsideBlock());
                sendNoEvent(new PlayerInteractBlockC2SPacket(p.getHand(), hitresult, p.getSequence()));
            }
        }

            /* Really old disabler method... but still works
               On Grim you need a lot of extra logic for it to works.
               I will try my best to explain everything.
            */
        if (mode.get().equals(DisablerMode.Grim)) {
            /*
             * This method serves as a disabler for GrimAC.
             * It exploits a delay tactic to trick the server into thinking the abilities packet hasn't arrived yet.
             * By delaying the transaction upon receiving abilities packets,
             * we can deactivate the AntiCheat system temporarily.
             * The Anticheat exempts us, as the same situation could happen to laggy players
             * The disabler works on old Intave and Grim.
             * On Grim, it flags 'reach', because it assumes the player hasn't received any packets, and thus is not moving(?),
             * thus indicating a possible cheat attempt.
             *
             * Source: https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/events/packets/PacketPlayerAbilities.java
             */
            if (mc.player.age < 20) {
                grimPacketQueue.clear();
                return;
            }




            /* Specific to Grim:
             * Explanation:
             * After leaving spectator mode, players teleport back (affected by BadPacketsN).
             * BadPacketsN checks teleport queues with every PlayerMoveC2SPacket.Full.
             * By delaying transaction packets, our lastTransaction data on Grim (or any) becomes the latest transac.
             * BadPacketsN checks if the teleport transaction is greater than the latest sent transaction.
             * Since we send all at once, it won't have time to verify until the next tick (C06).
             * Even if we bypass BadPacketsN, it still sets us back.
             * TODO: FIND A WAY AROUND THIS (works on all servers).
             * On some servers, we receive teleport packets before abilities packets after leaving spectator mode.
             * Delaying right after teleport could mitigate this issue.
             * Note: Activate only after leaving spectator mode.
             * Reference: https://github.com/GrimAnticheat/Grim/blob/2.0/src/main/java/ac/grim/grimac/manager/SetbackTeleportUtil.java
- peenis esp
- grim disavler???
- import optimize
             */
            if (packet instanceof PlayerPositionLookS2CPacket) {
                // should we delay packet?
                if (mc.player.getAbilities().flying) {
                    delay = true;
                }
            }

            if (delay) {
                //it not really good idea to delay our packets for way too long, let's just release it after a while
                // idk this is fine for minemalia ig.
//                if (timer.hasElapsed(seconds.toLong() * 1000L)) {
//                    packetQueue.forEach() { handlePacket(it.packet) }
//                    packetQueue.clear()
//
//                    delay = false
//                    timer.reset()
//                }

                if (packet instanceof CommonPingS2CPacket p ) {
                    grimPacketQueue.add(p);
                    event.cancel();

                    sendNoEvent(new CommonPongC2SPacket(0));
                }
            }
        }

        if (mode.get().equals(DisablerMode.CancelKeepalive)) {
            if (event.packet instanceof KeepAliveC2SPacket p) {
                event.cancel();
            }
        }
    }

    @EventHandler
    private void onUpdate(final SendMovementPacketsEvent.Pre e) {
        vulcanfullticks++;
        vulcanScaffoldTicks++;

        if (spoofOnGround.get())
            mc.player.setOnGround(onGroundMode.get().getValue());

        if (autodisable.get() && vulcanfullticks >= seconds.get()*20) {
            if (!redo.get())
                this.toggle();
            else {
                mc.player.updatePosition(mc.player.getX(), mc.player.getY() - 128, mc.player.getZ());
                onDeactivate();
            }
            vulcanfullticks = 0;
        }

        if (mode.get().equals(DisablerMode.CustomPayload)) {
            // really retarded, will prob crash game
            byte[] array = new byte[] {8, 52, 48, 52, 49, 51, 101, 98, 49};
            PacketByteBuf a = new PacketByteBuf(null);
            for (int i = 0; i < array.length; i++) {
                a.setBytes(i, array);
            }
            sendNoEvent(new CustomPayloadC2SPacket(a));
        }

        if (mode.get().equals(DisablerMode.NullPlace)) {
            sendNoEvent(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(0/0, 0/0, 0/0), Direction.DOWN, 0));
        }


        if (mode.get().equals(DisablerMode.VulcanSprint) || sprintDisabler.get()) {
            sendNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            sendNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }

        if ((mode.get().equals(DisablerMode.VulcanScaffold) || scaffoldDisabler.get()) && vulcanScaffoldTicks >= vulcanTickTimer.get()) {
            sendNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
            sendNoEvent(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            vulcanScaffoldTicks = 0;
        }
    }

    public boolean isVulcanMode() {
        return mode.get().equals(DisablerMode.VulcanFullBoth) || mode.get().equals(DisablerMode.VulcanFullPing) || mode.get().equals(DisablerMode.VulcanFullScreenHandler);
    }

    public enum DisablerMode {
        Grim,
        VulcanSprint,
        VulcanFullPing,
        VulcanFullScreenHandler,
        VulcanFullBoth,
        VulcanScaffold,
        CustomPayload,
        NullPlace,
        CancelKeepalive,
        CancelPayload,
        GrimPlace
    }

    public enum onGroundEnum {
        ON_GROUND(true),
        OFF_GROUND(false);

        private final boolean value;

        onGroundEnum(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }
    }
}
