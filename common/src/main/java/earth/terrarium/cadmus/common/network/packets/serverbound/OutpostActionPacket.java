package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.api.NotificationApi;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.common.outposts.OutpostManager;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public record OutpostActionPacket(ChunkPos start, ChunkPos end) implements Packet<OutpostActionPacket> {

    public static final ServerboundPacketType<OutpostActionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("outpost_action"),
        ObjectByteCodec.create(
            ExtraByteCodecs.CHUNK_POS.fieldOf(OutpostActionPacket::start),
            ExtraByteCodecs.CHUNK_POS.fieldOf(OutpostActionPacket::end),
            OutpostActionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            Component error = OutpostManager.claim(serverPlayer, packet.start(), packet.end());
            if (error == null) return;
            String key = error.getContents() instanceof TranslatableContents contents ?
                contents.getKey() : TownManager.ERR_NO_PERMISSION;
            NotificationApi.notify(serverPlayer, key);
        })
    );

    @Override
    public PacketType<OutpostActionPacket> type() {
        return TYPE;
    }
}
