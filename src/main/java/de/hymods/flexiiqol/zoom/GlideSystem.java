package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Function;

/**
 * Every tick: does the player hold a zoom chain right now? Watching the chains also covers every way a
 * zoom can end (release, click, item change, death), not only the release.
 */
public final class GlideSystem extends EntityTickingSystem<EntityStore> {
    private final Glide glide;
    private final Function<UUID, Float> lerpFor;
    private final ComponentType<EntityStore, InteractionManager> managerType;
    private final Query<EntityStore> query;

    /** @param lerpFor the camera lerp a player's glide asks for ({@link Glide#lerp}), null for none */
    public GlideSystem(Glide glide, Function<UUID, Float> lerpFor) {
        this.glide = glide;
        this.lerpFor = lerpFor;
        this.managerType = InteractionModule.get().getInteractionManagerComponent();
        this.query = Query.and(PlayerRef.getComponentType(), this.managerType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef player = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }
        boolean zooming = zooming(archetypeChunk.getComponent(index, this.managerType));
        if (!zooming && !this.glide.isActive(player.getUuid())) {
            return;
        }
        this.glide.update(player, zooming ? this.lerpFor.apply(player.getUuid()) : null);
    }

    static boolean zooming(InteractionManager manager) {
        if (manager == null) {
            return false;
        }
        for (InteractionChain chain : manager.getChains().values()) {
            RootInteraction root = chain.getRootInteraction();
            if (root != null && root.getId() != null && root.getId().startsWith(ZoomAssets.PREFIX)) {
                return true;
            }
        }
        return false;
    }
}
