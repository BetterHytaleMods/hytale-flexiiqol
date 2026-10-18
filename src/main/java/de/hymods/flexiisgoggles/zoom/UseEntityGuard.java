package de.hymods.flexiisgoggles.zoom;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseEntityEvent;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * "Use entity" runs what the target has bound to the pressed input, in the presser's name. Without
 * this guard, R with an empty hand at a player whose R zooms would start that player's zoom for the
 * presser.
 */
public final class UseEntityGuard extends EntityEventSystem<EntityStore, UseEntityEvent.Pre> {

    public UseEntityGuard() {
        super(UseEntityEvent.Pre.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UseEntityEvent.Pre event) {
        if (!event.getTargetEntity().isValid()) {
            return;
        }
        Interactions target = commandBuffer.getComponent(event.getTargetEntity(), Interactions.getComponentType());
        if (target != null && ZoomBindings.isZoom(target.getInteractionId(event.getInteractionType()))) {
            event.setCancelled(true);
        }
    }
}
