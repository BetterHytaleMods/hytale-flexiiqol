package de.hymods.flexiiqol.spike;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.interaction.event.InteractionChainStartEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/** Feasibility test: which root interaction the server starts for a watched player's input. */
public final class ProbeChainSystem extends EntityEventSystem<EntityStore, InteractionChainStartEvent> {
    private final Probe probe;

    public ProbeChainSystem(Probe probe) {
        super(InteractionChainStartEvent.class);
        this.probe = probe;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionChainStartEvent event) {
        PlayerRef player = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (player != null && this.probe.watching(player.getUuid())) {
            this.probe.log("%s starts %s with %s".formatted(player.getUsername(), event.getType(), event.getRootInteractionId()));
        }
    }
}
