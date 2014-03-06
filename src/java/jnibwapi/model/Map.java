package jnibwapi.model;

import java.awt.Point;
import java.util.*;

import jnibwapi.Broodwar;

/**
 * Stores information about a StarCraft map.
 */
public class Map {
    public static final int TILE_SIZE = 32;

    private final int width;
    private final int height;
    private final int walkWidth;
    private final int walkHeight;
    private final String name;
    private final String fileName;
    private final String hash;
    private final int[] heightMap;
    private final boolean[] buildable;
    private final boolean[] walkable;
    /** Walkability of build tiles */
    private final boolean[] lowResWalkable;

    // The following are set in initialize() method
    private boolean mapDetailsEnabled = false;
    /** Region ID for each build tile */
    private int[] regionMap = null;
    private List<Region> regions = null;
    private List<ChokePoint> chokePoints = null;
    private List<BaseLocation> baseLocations = null;
    private HashMap<Integer, Region> idToRegion = null;

    public Map(final String name, final String fileName, final String hash,
            final int width, final int height, final int[] heightMap,
            final int[] buildable, final int[] walkable) {
        this.width = width;
        this.height = height;
        walkWidth = 4 * width;
        walkHeight = 4 * height;
        this.name = name;
        this.fileName = fileName;
        this.hash = hash;
        this.heightMap = heightMap;
        this.buildable = new boolean[buildable.length];
        this.walkable = new boolean[walkable.length];

        for (int i = 0; i < buildable.length; i++) {
            this.buildable[i] = (buildable[i] == 1);
        }

        for (int i = 0; i < walkable.length; i++) {
            this.walkable[i] = (walkable[i] == 1);
        }

        // Fill lowResWalkable for A* search
        lowResWalkable = new boolean[width * height];
        Arrays.fill(lowResWalkable, true);
        for (int wx = 0; wx < walkWidth; wx++) {
            for (int wy = 0; wy < walkHeight; wy++) {
                lowResWalkable[(wx / 4) + (width * (wy / 4))] &=
                        isWalkable(wx, wy);
            }
        }
    }

    /** Initialize the map with regions and base locations */
    public void initialize(final int[] regionMapData, final int[] regionData,
            final HashMap<Integer, int[]> regionPolygons,
            final int[] chokePointData, final int[] baseLocationData) {
        // regionMap
        regionMap = regionMapData;

        // regions
        regions = new ArrayList<>();
        if (regionData != null) {
            for (int index = 0; index < regionData.length; index +=
                    Region.NUM_ATTRIBUTES) {
                final int[] coordinates = regionPolygons.get(regionData[index]);
                final Region region =
                        new Region(regionData, index, coordinates);
                regions.add(region);
            }
        }
        idToRegion = new HashMap<>();
        for (final Region region : regions) {
            idToRegion.put(region.getID(), region);
        }

        // choke points
        chokePoints = new ArrayList<>();
        if (chokePointData != null) {
            for (int index = 0; index < chokePointData.length; index +=
                    ChokePoint.NUM_ATTRIBUTES) {
                final ChokePoint chokePoint =
                        new ChokePoint(chokePointData, index, idToRegion);
                chokePoints.add(chokePoint);
            }
        }

        // base locations
        baseLocations = new ArrayList<>();
        if (baseLocationData != null) {
            for (int index = 0; index < baseLocationData.length; index +=
                    BaseLocation.NUM_ATTRIBUTES) {
                final BaseLocation baseLocation =
                        new BaseLocation(baseLocationData, index);
                baseLocations.add(baseLocation);
            }
        }

        // connect the region graph
        for (final ChokePoint chokePoint : chokePoints) {
            chokePoint.getFirstRegion().addChokePoint(chokePoint);
            chokePoint.getFirstRegion().addConnectedRegion(
                    chokePoint.getSecondRegion());
            chokePoint.getSecondRegion().addChokePoint(chokePoint);
            chokePoint.getSecondRegion().addConnectedRegion(
                    chokePoint.getFirstRegion());
        }

        mapDetailsEnabled = true;
    }

    /**
     * If {@code mapDetailsEenabled} is {@code true}, the map will be analyzed
     * in detail allowing the {@link Broodwar#getMap() map} to provide region,
     * choke point, and base location information. Broodwar may hang for a short
     * duration the first time a map is analyzed. Once the map analysis has
     * completed, the information is cached, reducing the analysis time for that
     * map.
     * 
     * @return
     */
    public boolean isMapDetailsEnabled() {
        return mapDetailsEnabled;
    }

    /** In build tiles (32px) */
    public int getWidth() {
        return width;
    }

    /** In build tiles (32px) */
    public int getHeight() {
        return height;
    }

    public int getWalkWidth() {
        return walkWidth;
    }

    public int getWalkHeight() {
        return walkHeight;
    }

    /** The name of the current map */
    public String getName() {
        return name;
    }

    /** The file name of the current map / replay file */
    public String getFileName() {
        return fileName;
    }

    public String getHash() {
        return hash;
    }

    public int getHeight(final int tx, final int ty) {
        if ((tx < width) && (ty < height) && (tx >= 0) && (ty >= 0)) {
            return heightMap[tx + (width * ty)];
        } else {
            return 0;
        }
    }

    /**
     * Works only after initialize(). Returns null if the specified position is
     * out of bounds.
     */
    public Region getRegion(final int tx, final int ty) {
        if ((tx < width) && (ty < height) && (tx >= 0) && (ty >= 0)) {
            return idToRegion.get(regionMap[tx + (width * ty)]);
        } else {
            return null;
        }
    }

    public boolean isBuildable(final int tx, final int ty) {
        if ((tx < width) && (ty < height) && (tx >= 0) && (ty >= 0)) {
            return buildable[tx + (width * ty)];
        } else {
            return false;
        }
    }

    public boolean isWalkable(final int wx, final int wy) {
        if ((wx < walkWidth) && (wy < walkHeight) && (wx >= 0) && (wy >= 0)) {
            return walkable[wx + (walkWidth * wy)];
        } else {
            return false;
        }
    }

    /** Checks whether all 16 walk tiles in a build tile are walkable */
    public boolean isLowResWalkable(final int tx, final int ty) {
        if ((tx < width) && (ty < height) && (tx >= 0) && (ty >= 0)) {
            return lowResWalkable[tx + (width * ty)];
        } else {
            return false;
        }
    }

    /** Works only after initialize() */
    public List<Region> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    /** Works only after initialize() */
    public Region getRegion(final int regionID) {
        return idToRegion.get(regionID);
    }

    /** Works only after initialize() */
    public List<ChokePoint> getChokePoints() {
        return Collections.unmodifiableList(chokePoints);
    }

    /** Works only after initialize() */
    public List<BaseLocation> getBaseLocations() {
        return Collections.unmodifiableList(baseLocations);
    }

    /** Works only after initialize() */
    public List<BaseLocation> getStartLocations() {
        final List<BaseLocation> startLocations = new ArrayList<>();
        for (final BaseLocation bl : baseLocations) {
            if (bl.isStartLocation()) {
                startLocations.add(bl);
            }
        }
        return startLocations;
    }

    /**
     * Find the shortest walkable distance, in pixels, between two tile
     * positions or -1 if not reachable. Works only after initialize(). Ported
     * from BWTA.
     */
    public double getGroundDistance(final int startTx, final int startTy,
            final int endTx, final int endTy) {
        if (!isConnected(startTx, startTy, endTx, endTy)) {
            return -1;
        }
        return aStarSearchDistance(startTx, startTy, endTx, endTy);
    }

    /**
     * Based on map connectedness only. Ignores buildings. Works only after
     * initialize(). Ported from BWTA.
     */
    public boolean isConnected(final int startTx, final int startTy,
            final int endTx, final int endTy) {
        if (getRegion(startTx, startTy) == null) {
            return false;
        }
        if (getRegion(endTx, endTy) == null) {
            return false;
        }
        return getRegion(startTx, startTy).getAllConnectedRegions().contains(
                getRegion(endTx, endTy));
    }

    /**
     * Performs an A* search. Intended to be called from
     * {@link #getGroundDistance(int, int, int, int)}. Ported from BWTA.
     */
    private double aStarSearchDistance(final int startTx, final int startTy,
            final int endTx, final int endTy) {
        // Distance of 10 per build tile, or sqrt(10^2 + 10^2) ~= 14 diagonally
        final int mvmtCost = 10;
        final int mvmtCostDiag = 14;
        final PriorityQueue<AStarTile> openTiles =
                new PriorityQueue<AStarTile>(); // min heap
        // Map from tile to distance
        final HashMap<Point, Integer> gmap = new HashMap<Point, Integer>();
        final HashSet<Point> closedTiles = new HashSet<Point>();
        final Point start = new Point(startTx, startTy);
        final Point end = new Point(endTx, endTy);
        openTiles.add(new AStarTile(start, 0));
        gmap.put(start, 0);
        while (!openTiles.isEmpty()) {
            final Point p = openTiles.poll().tilePos;
            if (p.equals(end)) {
                return (gmap.get(p) * TILE_SIZE) / (double) mvmtCost;
            }
            final int gvalue = gmap.get(p);
            closedTiles.add(p);
            // Explore the neighbours of p
            final int minx = Math.max(p.x - 1, 0);
            final int maxx = Math.min(p.x + 1, width - 1);
            final int miny = Math.max(p.y - 1, 0);
            final int maxy = Math.min(p.y + 1, height - 1);
            for (int x = minx; x <= maxx; x++) {
                for (int y = miny; y <= maxy; y++) {
                    if (!isLowResWalkable(x, y)) {
                        continue;
                    }
                    if ((p.x != x) && (p.y != y) && !isLowResWalkable(p.x, y)
                            && !isLowResWalkable(x, p.y)) {
                        continue; // Not diagonally accessible
                    }
                    final Point t = new Point(x, y);
                    if (closedTiles.contains(t)) {
                        continue;
                    }

                    int g = gvalue + mvmtCost;
                    if ((x != p.x) && (y != p.y)) {
                        g = gvalue + mvmtCostDiag;
                    }
                    final int dx = Math.abs(x - end.x);
                    final int dy = Math.abs(y - end.y);
                    // Heuristic for remaining distance:
                    // min(dx, dy) is the minimum diagonal distance, so costs
                    // mvmtCostDiag
                    // abs(dx - dy) is the rest of the distance, so costs
                    // mvmtCost
                    final int h =
                            (Math.abs(dx - dy) * mvmtCost)
                                    + (Math.min(dx, dy) * mvmtCostDiag);
                    final int f = g + h;
                    if (!gmap.containsKey(t) || (gmap.get(t) > g)) {
                        gmap.put(t, g);
                        for (final Iterator<AStarTile> it =
                                openTiles.iterator(); it.hasNext();) {
                            if (it.next().tilePos.equals(t)) {
                                it.remove();
                            }
                        }
                        openTiles.add(new AStarTile(t, f));
                    }
                }
            }
        }
        // Not found
        return -1;
    }

    private static class AStarTile implements Comparable<AStarTile> {
        Point tilePos;
        int distPlusCost;

        public AStarTile(final Point tile, final int distance) {
            tilePos = tile;
            distPlusCost = distance;
        }

        @Override
        public int compareTo(final AStarTile o) {
            return Integer.compare(distPlusCost, o.distPlusCost);
        }
    }

}
