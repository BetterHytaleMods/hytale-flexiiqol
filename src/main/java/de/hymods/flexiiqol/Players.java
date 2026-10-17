package de.hymods.flexiiqol;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;

/** Components belong to the player's world thread; packets can be written from anywhere. */
public final class Players {
    private Players() {
    }

    /** Runs {@code action} on the player's world thread, if the player is still in a world then. */
    public static void onWorld(@Nonnull PlayerRef playerRef, @Nonnull BiConsumer<Store<EntityStore>, Ref<EntityStore>> action) {
        Ref<EntityStore> ref = playerRef.getReference();
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (ref == null || world == null) {
            return;
        }
        world.execute(() -> {
            if (ref.isValid()) {
                action.accept(ref.getStore(), ref);
            }
        });
    }
}
