package org.madblock.newgamesapi.nukkit.packet;

import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.ProtocolInfo;

public class AnimateEntityPacket extends DataPacket {

    public long eid;
    public String animation;
    public String controller;

    @Override
    public byte pid() {
        return ProtocolInfo.ANIMATE_ENTITY_PACKET;
    }

    @Override
    public void decode() {

    }

    @Override
    public void encode() {
        this.reset();
        this.putString(animation);
        this.putString("default");
        this.putString("\"\"");
        this.putString(controller);
        this.putLFloat(0);
        this.putUnsignedVarInt(1);
        this.putUnsignedVarLong(eid);
    }
}
