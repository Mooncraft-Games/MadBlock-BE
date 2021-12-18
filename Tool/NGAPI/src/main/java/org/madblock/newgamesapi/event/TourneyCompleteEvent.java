package org.madblock.newgamesapi.event;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TourneyCompleteEvent extends Event implements Cancellable {

    public final String sourceGameTypeID;
    public final String sourceSessionID;

}
