package de.hymods.flexiisgoggles.goggles;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every tick: which goggles each player wears, on their own or mounted on a helm. Goggles share the
 * head slot with helmets; there is no face slot. The other systems read the result.
 */
public final class WornGoggles extends EntityTickingSystem<EntityStore> {
    private static final short HEAD = (short) ItemArmorSlot.Head.getValue();

    private final Map<UUID, Goggles> worn = new ConcurrentHashMap<>();
    private final Query<EntityStore> query = Query.and(PlayerRef.getComponentType(), InventoryComponent.Armor.getComponentType());

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
        InventoryComponent.Armor armor = archetypeChunk.getComponent(index, InventoryComponent.Armor.getComponentType());
        ItemContainer inventory = armor == null ? null : armor.getInventory();
        ItemStack head = inventory == null || inventory.getCapacity() <= HEAD ? null : inventory.getItemStack(HEAD);
        Goggles goggles = ItemStack.isEmpty(head) ? null : Goggles.byItemId(head.getItemId());
        if (goggles == null) {
            this.worn.remove(player.getUuid());
        } else {
            this.worn.put(player.getUuid(), goggles);
        }
    }

    /** The goggles the player has on, or null. */
    @Nullable
    public Goggles worn(@Nonnull UUID player) {
        return this.worn.get(player);
    }

    /** Players online with goggles on. */
    public int count() {
        return this.worn.size();
    }

    public void forget(@Nonnull UUID player) {
        this.worn.remove(player);
    }
}
