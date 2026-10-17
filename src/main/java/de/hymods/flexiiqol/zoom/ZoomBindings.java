package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Puts a zoom root on one of the player's inputs. The player's own {@link Interactions} component
 * wins over whatever item is held; it is saved with the player, and entries whose root no longer
 * exists are dropped when the plugin is gone. Only call on the player's world thread.
 */
public final class ZoomBindings {
    private ZoomBindings() {
    }

    public static void bind(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull InteractionType input, @Nonnull String rootId) {
        Interactions existing = store.getComponent(ref, Interactions.getComponentType());
        Interactions interactions = existing != null ? existing : new Interactions();
        if (rootId.equals(interactions.getInteractionId(input))) {
            return;
        }
        interactions.setInteractionId(input, rootId);
        if (existing == null) {
            store.putComponent(ref, Interactions.getComponentType(), interactions);
        }
    }

    /** Removes a zoom root from {@code input}; anything else bound there stays. */
    public static void unbind(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull InteractionType input) {
        Interactions interactions = store.getComponent(ref, Interactions.getComponentType());
        if (interactions == null) {
            return;
        }
        String id = interactions.getInteractionId(input);
        if (id == null || !id.startsWith(ZoomAssets.PREFIX)) {
            return;
        }
        interactions.removeInteractionId(input);
        if (interactions.isEmpty() && !interactions.isOverrideAll()) {
            store.tryRemoveComponent(ref, Interactions.getComponentType());
        }
    }

    /** The zoom root bound to {@code input}, or null. */
    public static String bound(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull InteractionType input) {
        Interactions interactions = store.getComponent(ref, Interactions.getComponentType());
        String id = interactions == null ? null : interactions.getInteractionId(input);
        return id != null && id.startsWith(ZoomAssets.PREFIX) ? id : null;
    }
}
