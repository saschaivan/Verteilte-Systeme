package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighborUpdate implements Serializable {
    InetSocketAddress address;
    Direction direction;

    public NeighborUpdate(InetSocketAddress address, Direction direction) {
        this.address = address;
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
