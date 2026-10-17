package de.hymods.flexiiqol.spike;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.player.MouseInteraction;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;

/**
 * Feasibility test: logs what a player's client sends. Interaction chains and mouse buttons in
 * detail, every other packet type once when it first shows up, all of them counted.
 */
public final class Probe implements PlayerPacketWatcher {
    private final HytaleLogger logger;
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Map<String, LongAdder> counts = new ConcurrentHashMap<>();
    private PacketFilter handle;

    public Probe(HytaleLogger logger) {
        this.logger = logger;
    }

    public void register() {
        this.handle = PacketAdapters.registerInbound((PlayerPacketWatcher) this);
    }

    public void close() {
        if (this.handle != null) {
            PacketAdapters.deregisterInbound(this.handle);
            this.handle = null;
        }
    }

    public void watch(UUID player, boolean on) {
        if (on) {
            this.players.add(player);
        } else {
            this.players.remove(player);
        }
    }

    public boolean watching(UUID player) {
        return this.players.contains(player);
    }

    public String counts() {
        Map<String, Long> sorted = new TreeMap<>();
        this.counts.forEach((name, count) -> sorted.put(name, count.sum()));
        return sorted.toString();
    }

    public void log(String line) {
        this.logger.at(Level.INFO).log("probe %s", line);
    }

    @Override
    public void accept(PlayerRef player, Packet packet) {
        if (!this.players.contains(player.getUuid())) {
            return;
        }
        String name = packet.getClass().getSimpleName();
        LongAdder count = this.counts.computeIfAbsent(name, key -> {
            this.log(player.getUsername() + " first " + key + " (id " + packet.getId() + ")");
            return new LongAdder();
        });
        count.increment();
        if (packet instanceof SyncInteractionChains chains) {
            for (SyncInteractionChain update : chains.updates) {
                if (update.initial || update.state != InteractionState.NotFinished) {
                    this.log("%s chain %d %s initial=%s state=%s item=%s".formatted(player.getUsername(), update.chainId,
                            update.interactionType, update.initial, update.state, update.itemInHandId));
                }
            }
        } else if (packet instanceof MouseInteraction mouse && mouse.mouseButton != null) {
            this.log("%s mouse %s %s item=%s".formatted(player.getUsername(), mouse.mouseButton.mouseButtonType,
                    mouse.mouseButton.state, mouse.itemInHandId));
        }
    }
}
