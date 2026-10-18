package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.CoreItemAbility;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;

/**
 * Keeps each player's zoom step on R, except while the game needs R ({@link ZoomRules}). Checking
 * every tick covers joining, respawning, switching items, changing game modes and picking another
 * step on {@code /qol} alike.
 */
public final class ZoomBindingSystem extends EntityTickingSystem<EntityStore> {
    /** R in the default controls ("Use Ability 3"). */
    public static final InteractionType INPUT = InteractionType.Ability3;
    /** The ability slot R casts from. */
    private static final short RUNE_SLOT = 3;

    private final Function<UUID, String> rootFor;

    /** @param rootFor the zoom root a player gets, or null while the zoom is off or not loaded */
    public ZoomBindingSystem(Function<UUID, String> rootFor) {
        this.rootFor = rootFor;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef player = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        if (player == null) {
            return;
        }
        String root = this.rootFor.apply(player.getUuid());
        Interactions interactions = archetypeChunk.getComponent(index, Interactions.getComponentType());
        String current = interactions == null ? null : interactions.getInteractionId(INPUT);
        boolean ours = ZoomBindings.isZoom(current);
        boolean taken = current != null && !ours || interactions != null && interactions.isOverrideAll();

        InventoryComponent.Tool tool = archetypeChunk.getComponent(index, InventoryComponent.Tool.getComponentType());
        InventoryComponent.Hotbar hotbar = archetypeChunk.getComponent(index, InventoryComponent.Hotbar.getComponentType());
        boolean usingTools = tool != null && tool.isUsingToolsItem();
        ItemStack weapon = hotbar == null ? null : hotbar.getActiveItem();
        ItemStack inHand = usingTools ? tool.getActiveItem() : weapon;
        boolean block = !ItemStack.isEmpty(inHand) && inHand.getItem().hasBlockType();
        boolean rune = !usingTools && runeReady(archetypeChunk.getComponent(index, InventoryComponent.AbilitySlots.getComponentType()), weapon);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ZoomRules.decide(root != null, taken, block, rune) == ZoomRules.Hold.ZOOM) {
            if (!root.equals(current)) {
                ZoomBindings.bind(commandBuffer, ref, INPUT, root);
            }
        } else if (ours) {
            ZoomBindings.unbind(commandBuffer, ref, INPUT);
        }
    }

    /** Like the game: R casts the rune in slot 3 if it fits the weapon in the hotbar. */
    private static boolean runeReady(@Nullable InventoryComponent.AbilitySlots slots, @Nullable ItemStack weapon) {
        if (slots == null || ItemStack.isEmpty(weapon)) {
            return false;
        }
        ItemContainer inventory = slots.getInventory();
        if (inventory == null || inventory.getCapacity() <= RUNE_SLOT) {
            return false;
        }
        ItemStack rune = inventory.getItemStack(RUNE_SLOT);
        return !ItemStack.isEmpty(rune) && rune.getItem().getAbility() instanceof CoreItemAbility ability && ability.canUseWith(weapon);
    }
}
