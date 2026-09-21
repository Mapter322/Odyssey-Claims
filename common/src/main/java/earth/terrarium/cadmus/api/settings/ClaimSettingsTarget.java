package earth.terrarium.cadmus.api.settings;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import earth.terrarium.cadmus.api.teams.TeamId;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record ClaimSettingsTarget(TeamId team, UUID town, @Nullable ChunkRef chunk) {
    public static final UUID GLOBAL = new UUID(0L, 0L);

    public static final ByteCodec<ClaimSettingsTarget> BYTE_CODEC = ObjectByteCodec.create(
        TeamId.BYTE_CODEC.fieldOf(ClaimSettingsTarget::team),
        ByteCodec.UUID.fieldOf(ClaimSettingsTarget::town),
        ChunkRef.BYTE_CODEC.optionalFieldOf(target -> Optional.ofNullable(target.chunk())),
        (team, town, chunk) -> new ClaimSettingsTarget(team, town, chunk.orElse(null))
    );

    public ClaimSettingsTarget(TeamId team, UUID town) {
        this(team, town, null);
    }

    public ClaimSettingsTarget(TeamId team) {
        this(team, GLOBAL, null);
    }

    public boolean isGlobal() {
        return GLOBAL.equals(town) && chunk == null;
    }

    public boolean isChunk() {
        return chunk != null;
    }

    public Optional<UUID> townId() {
        return GLOBAL.equals(town) ? Optional.empty() : Optional.of(town);
    }
}
