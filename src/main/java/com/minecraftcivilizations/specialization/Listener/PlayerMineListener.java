package com.minecraftcivilizations.specialization.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.minecraftcivilizations.specialization.Mining.BlockDamage;
import com.minecraftcivilizations.specialization.Specialization;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

import java.util.HashMap;
import java.util.Set;

public class PlayerMineListener implements Listener {

    public static HashMap<String, Long> armSwinging = new HashMap<>();

    private final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

    private final BlockDamage damage = new BlockDamage();

    public PlayerMineListener() {
        checkArmAnimation();
        receivedArmAnimation();
    }

    private void receivedArmAnimation() {
        manager.addPacketListener(new PacketAdapter(Specialization.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Client.ARM_ANIMATION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                armSwinging.put(event.getPlayer().getName(), System.currentTimeMillis());
            }
        });
    }

    @EventHandler
    private void startBreakingBlock(BlockDamageEvent event) {

        PacketContainer packetContainer = damage.configureBreakingPacket(event.getPlayer(), event.getBlock());

        double breakingTime = damage.getBreakingTime(event.getPlayer(), event.getBlock());

        if (breakingTime == 0) {
            damage.playerBreakBlock(event.getPlayer(), event.getBlock());
            return;
        }

        damage.startBreaking(event.getPlayer(), packetContainer, breakingTime, event.getBlock());

    }

    private void checkArmAnimation() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Specialization.getInstance(), new Runnable() {
            @Override
            public void run() {
                Set<String> keySet = armSwinging.keySet();
                long currentTime = System.currentTimeMillis();
                for (String string : keySet) {
                    if (armSwinging.get(string) + 150 < currentTime) {
                        armSwinging.remove(string);
                    }
                }
            }

        }, 1L, 1L);
    }
}
