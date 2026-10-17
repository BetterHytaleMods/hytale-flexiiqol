package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Puts a zoom root on one of the player's inputs. The player's own {@link Interactions} component
 * wins over whatever item is held; it is saved with the player, and entries whose root no longer
 * exists are dropped when the plugin is gone. Entries of other plugins or the game mode stay
 * untouched. Only call on the player's world thread.
 */
public final class ZoomBindings {
    private ZoomBindings() {
    }

    public static boolean isZoom(@Nullable String rootId) {
        return rootId != null && rootId.startsWith(ZoomAssets.PREFIX);
    }

    /**
     * Binds {@code rootId} to {@code input}, unless something else than a zoom is bound there.
     *
     * @return whether the zoom is bound now
     */
    public static boolean bind(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref,
                               @Nonnull InteractionType input, @Nonnull String rootId) {
        Interactions existing = accessor.getComponent(ref, Interactions.getComponentType());
        Interactions interactions = existing != null ? existing : new Interactions();
        String current = interactions.getInteractionId(input);
        if (rootId.equals(current)) {
            return true;
        }
        if (current != null && !isZoom(current)) {
            return false;
        }
        interactions.setInteractionId(input, rootId);
        if (existing == null) {
            accessor.putComponent(ref, Interactions.getComponentType(), interactions);
        }
        return true;
    }

    /** Removes a zoom root from {@code input}; anything else bound there stays. */
    public static void unbind(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull InteractionType input) {
        Interactions interactions = accessor.getComponent(ref, Interactions.getComponentType());
        if (interactions == null || !isZoom(interactions.getInteractionId(input))) {
            return;
        }
        interactions.removeInteractionId(input);
        if (interactions.isEmpty() && !interactions.isOverrideAll()) {
            accessor.tryRemoveComponent(ref, Interactions.getComponentType());
        }
    }

    /** The zoom root bound to {@code input}, or null. */
    @Nullable
    public static String bound(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull InteractionType input) {
        Interactions interactions = accessor.getComponent(ref, Interactions.getComponentType());
        String id = interactions == null ? null : interactions.getInteractionId(input);
        return isZoom(id) ? id : null;
    }
}
