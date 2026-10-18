package de.hymods.flexiiqol.zoom;

import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A gliding camera like OptiFine's cinematic camera, while zoomed: a server camera in first person
 * that follows the player's look with a rotation lerp below 1.
 */
public final class Glide {
    /** Players whose camera is ours right now, with the lerp they got. */
    private final Map<UUID, Float> active = new ConcurrentHashMap<>();

    /**
     * The camera's rotation lerp for a glide amount: the share of the remaining turn covered per
     * step. Null for 0, which is no glide at all. Up to 0.9 the lerp is {@code 1 - glide}; above,
     * the lag (1 / lerp) grows in proportion, so 1.8 lags twice as long as 0.9.
     */
    @Nullable
    public static Float lerp(float glide) {
        if (!(glide > 0f)) {
            return null;
        }
        return glide <= 0.9f ? 1f - glide : 0.09f / Math.min(glide, 1.8f);
    }

    public boolean isActive(@Nonnull UUID player) {
        return this.active.containsKey(player);
    }

    /** Sends the gliding camera, or the normal one for null, when that changes. */
    void update(@Nonnull PlayerRef player, @Nullable Float lerp) {
        UUID id = player.getUuid();
        if (lerp != null) {
            Float sent = this.active.get(id);
            if (sent == null || !sent.equals(lerp)) {
                this.active.put(id, lerp);
                player.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, camera(lerp)));
            }
        } else if (this.active.remove(id) != null) {
            reset(player);
        }
    }

    /** Back to the normal camera, for everyone this camera belongs to. */
    public void resetAll(@Nonnull Iterable<PlayerRef> players) {
        for (PlayerRef player : players) {
            if (this.active.remove(player.getUuid()) != null) {
                reset(player);
            }
        }
    }

    public void forget(@Nonnull UUID player) {
        this.active.remove(player);
    }

    private static void reset(PlayerRef player) {
        player.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, false, null));
    }

    private static ServerCameraSettings camera(float lerp) {
        ServerCameraSettings settings = new ServerCameraSettings();
        settings.positionLerpSpeed = 1f;
        settings.rotationLerpSpeed = lerp;
        settings.isFirstPerson = true;
        settings.eyeOffset = true;
        settings.displayReticle = true;
        settings.allowPitchControls = true;
        return settings;
    }
}
