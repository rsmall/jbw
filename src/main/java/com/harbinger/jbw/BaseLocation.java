package com.harbinger.jbw;

import com.harbinger.jbw.Position.Resolution;

import java.util.Map;

/**
 * Represents a location on the map where it makes sense to place a base (e.g. near minerals).
 */
public class BaseLocation {

    static final int NUM_ATTRIBUTES = 10;

    private final Position center;
    private final Position position;
    private final Region region;
    private final int minerals;
    private final int gas;
    private final boolean island;
    private final boolean mineralOnly;
    private final boolean startLocation;

    BaseLocation(final int[] data, int index, final Map<Integer, Region> idToRegion) {
        final int x = data[index++];
        final int y = data[index++];
        center = new Position(x, y, Resolution.PIXEL);
        final int tx = data[index++];
        final int ty = data[index++];
        position = new Position(tx, ty, Resolution.BUILD);
        final int regionID = data[index++];
        region = idToRegion.get(regionID);
        minerals = data[index++];
        gas = data[index++];
        island = (data[index++] == 1);
        mineralOnly = (data[index++] == 1);
        startLocation = (data[index++] == 1);
    }

    /**
     * @return the top-left (build) Position where a Hatchery, Nexus, or Command Center should be
     *         built
     */
    public Position getPosition() {
        return position;
    }

    /**
     * @return the center Position of where a Hatchery, Nexus, or Command Center should be built
     */
    public Position getCenter() {
        return center;
    }

    /**
     * @return the Region this base is located in
     */
    public Region getRegion() {
        return region;
    }

    /**
     * @return the amount of minerals originally available
     */
    public int getMinerals() {
        return minerals;
    }

    /**
     * @return the amount of gas originally available
     */
    public int getGas() {
        return gas;
    }

    /**
     * @return true if this base is not reachable by ground from any other base; false otherwise
     */
    public boolean isIsland() {
        return island;
    }

    /**
     * @return true if this base only has mineral resources; false otherwise
     */
    public boolean isMineralOnly() {
        return mineralOnly;
    }

    /**
     * @return true if this base is a potential start location for a player; false otherwise
     */
    public boolean isStartLocation() {
        return startLocation;
    }
}
