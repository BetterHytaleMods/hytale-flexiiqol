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

/**
 * Every tick: does a player with a glide setting hold a zoom chain right now? Watching the chains
 * also covers every way a zoom can end (release, click, item change, death), not only the release.
 */
public final class GlideSystem extends EntityTickingSystem<EntityStore> {
    private final Glide glide;
    private final ComponentType<EntityStore, InteractionManager> managerType;
    private final Query<EntityStore> query;

    public GlideSystem(Glide glide) {
        this.glide = glide;
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
        UUID id = player.getUuid();
        Glide.Setting setting = this.glide.setting(id);
        if (setting == null && !this.glide.isActive(id)) {
            return;
        }
        boolean want = setting != null && (setting.always() || zooming(archetypeChunk.getComponent(index, this.managerType)));
        this.glide.update(player, want, setting);
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
