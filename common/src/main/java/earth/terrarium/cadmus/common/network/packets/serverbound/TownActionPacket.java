package earth.terrarium.cadmus.common.network.packets.serverbound;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.cadmus.Cadmus;
import earth.terrarium.cadmus.common.commands.claims.ClaimCommandType;
import earth.terrarium.cadmus.common.network.NetworkHandler;
import earth.terrarium.cadmus.common.network.packets.clientbound.ClaimMapNotificationPacket;
import earth.terrarium.cadmus.common.towns.TownManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public record TownActionPacket(
    ClaimCommandType action,
    String town,
    ChunkPos start,
    ChunkPos end
) implements Packet<TownActionPacket> {

    public static final ServerboundPacketType<TownActionPacket> TYPE = CodecPacketType.Server.create(
        Cadmus.id("town_action"),
        ObjectByteCodec.create(
            ByteCodec.ofEnum(ClaimCommandType.class).fieldOf(TownActionPacket::action),
            ByteCodec.STRING.fieldOf(TownActionPacket::town),
            ExtraByteCodecs.CHUNK_POS.fieldOf(TownActionPacket::start),
            ExtraByteCodecs.CHUNK_POS.fieldOf(TownActionPacket::end),
            TownActionPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (packet.action() != ClaimCommandType.TOWN_CREATE && packet.action() != ClaimCommandType.TOWN_ADD) return;
            Component error;
            if (packet.action() == ClaimCommandType.TOWN_CREATE) {
                error = TownManager.create(serverPlayer, packet.start(), packet.end(), packet.town());
            } else {
                UUID townId;
                try {
                    townId = UUID.fromString(packet.town());
                } catch (IllegalArgumentException e) {
                    return;
                }
                error = TownManager.add(serverPlayer, townId, packet.start(), packet.end());
            }
            if (error == null) return;
            String key = error.getContents() instanceof TranslatableContents contents ?
                contents.getKey() : TownManager.ERR_NO_PERMISSION;
            if (NetworkHandler.CHANNEL.canSendToPlayer(serverPlayer, ClaimMapNotificationPacket.TYPE)) {
                NetworkHandler.CHANNEL.sendToPlayer(new ClaimMapNotificationPacket(key), serverPlayer);
            }
        })
    );

    @Override
    public PacketType<TownActionPacket> type() {
        return TYPE;
    }
}
