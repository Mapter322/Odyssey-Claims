package earth.terrarium.cadmus.api.settings;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record ChunkRef(ResourceLocation dimension, ChunkPos pos) {

    public static final ByteCodec<ChunkRef> BYTE_CODEC = ObjectByteCodec.create(
        ByteCodec.STRING.fieldOf(ref -> ref.dimension().toString()),
        ByteCodec.INT.fieldOf(ref -> ref.pos().x),
        ByteCodec.INT.fieldOf(ref -> ref.pos().z),
        (dimension, x, z) -> new ChunkRef(ResourceLocation.parse(dimension), new ChunkPos(x, z))
    );

    public static ChunkRef of(Level level, ChunkPos pos) {
        return new ChunkRef(level.dimension().location(), pos);
    }

    public String key() {
        return pos.x + "," + pos.z;
    }
}
