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
 * A gliding camera like OptiFine's cinematic camera: a server camera in first person that follows
 * the player's look with a rotation lerp below 1. Whether it gets along with the client-side zoom is
 * what the feasibility test finds out.
 */
public final class Glide {
    /** @param always also without zooming, to try the camera on its own */
    public record Setting(float lerp, boolean always) {
        public Setting {
            if (!(lerp > 0f && lerp < 1f)) {
                throw new IllegalArgumentException("glide lerp must be above 0 and below 1");
            }
        }
    }

    private final Map<UUID, Setting> settings = new ConcurrentHashMap<>();
    /** Players whose camera is ours right now, with the lerp they got. */
    private final Map<UUID, Float> active = new ConcurrentHashMap<>();

    public void set(@Nonnull UUID player, @Nullable Setting setting) {
        if (setting == null) {
            this.settings.remove(player);
        } else {
            this.settings.put(player, setting);
        }
    }

    @Nullable
    public Setting setting(@Nonnull UUID player) {
        return this.settings.get(player);
    }

    public boolean isActive(@Nonnull UUID player) {
        return this.active.containsKey(player);
    }

    public int players() {
        return this.settings.size();
    }

    /** Sends the gliding camera or the normal one when {@code want} changes. */
    void update(@Nonnull PlayerRef player, boolean want, @Nullable Setting setting) {
        UUID id = player.getUuid();
        if (want && setting != null) {
            Float sent = this.active.get(id);
            if (sent == null || sent != setting.lerp()) {
                this.active.put(id, setting.lerp());
                player.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, camera(setting.lerp())));
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
        this.settings.remove(player);
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
