package org.madblock.newgamesapi.nukkit.packet;

import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.LevelChunkPacket;
import cn.nukkit.utils.BinaryStream;
import lombok.ToString;

@ToString
public class EmptyLevelChunkPacket extends DataPacket {

    private static byte[] EMPTY_CHUNK_DATA;

    {
        BinaryStream payload = new BinaryStream();
        payload.put(new byte[] { 8, 0 });

        // 3D biome data
        payload.putByte((byte) ((1 << 1) | 1));
        payload.put(new byte[512]);
        payload.putVarInt(1);
        payload.putVarInt(0);

        // border blocks (useless)
        payload.putByte((byte) 0);

        EMPTY_CHUNK_DATA = payload.getBuffer();
    }

    public int chunkX;
    public int chunkZ;

    @Override
    public byte pid() {
        return LevelChunkPacket.NETWORK_ID;
    }

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putVarInt(this.chunkX);
        this.putVarInt(this.chunkZ);
        this.putUnsignedVarInt(1);
        this.putBoolean(false);
        this.putByteArray(EMPTY_CHUNK_DATA);
    }

}
