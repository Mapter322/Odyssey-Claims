package earth.terrarium.cadmus.api.settings;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import earth.terrarium.cadmus.api.teams.TeamId;

import java.util.Optional;
import java.util.UUID;

public record ClaimSettingsTarget(TeamId team, UUID town) {
    public static final UUID GLOBAL = new UUID(0L, 0L);

    public static final ByteCodec<ClaimSettingsTarget> BYTE_CODEC = ObjectByteCodec.create(
        TeamId.BYTE_CODEC.fieldOf(ClaimSettingsTarget::team),
        ByteCodec.UUID.fieldOf(ClaimSettingsTarget::town),
        ClaimSettingsTarget::new
    );

    public ClaimSettingsTarget(TeamId team) {
        this(team, GLOBAL);
    }

    public boolean isGlobal() {
        return GLOBAL.equals(town);
    }

    public Optional<UUID> townId() {
        return isGlobal() ? Optional.empty() : Optional.of(town);
    }
}
