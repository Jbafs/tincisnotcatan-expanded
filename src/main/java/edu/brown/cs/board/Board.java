package edu.brown.cs.board;

import static edu.brown.cs.board.TileType.BRICK;
import static edu.brown.cs.board.TileType.DESERT;
import static edu.brown.cs.board.TileType.ORE;
import static edu.brown.cs.board.TileType.SEA;
import static edu.brown.cs.board.TileType.SHEEP;
import static edu.brown.cs.board.TileType.WHEAT;
import static edu.brown.cs.board.TileType.WOOD;
import static edu.brown.cs.catan.Settings.NUM_BRICK_TILE;
import static edu.brown.cs.catan.Settings.NUM_DESERT_TILE;
import static edu.brown.cs.catan.Settings.NUM_ORE_TILE;
import static edu.brown.cs.catan.Settings.NUM_SHEEP_TILE;
import static edu.brown.cs.catan.Settings.NUM_WHEAT_TILE;
import static edu.brown.cs.catan.Settings.NUM_WOOD_TILE;
import static edu.brown.cs.catan.Settings.ROLL_NUMS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.GameSettings.BoardLayout;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Board Class. Functions as a container class for all of the board data.
 */
public class Board {
  private Collection<Tile> _tiles;
  private Map<IntersectionCoordinate, Intersection> _intersections;
  private Map<PathCoordinate, Path> _paths;
  private final List<HexCoordinate> PORT_LOCATION;
  private final static int DEPTH = 2;
  private final static int DEPTH_EXTENDED = 3;

  // Neighbor offsets in this project's hex coordinate system (from fillEdges)
  private static final int[][] HEX_NEIGHBORS = {
      {0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}, {1, 0, 1}
  };

  // -------------------------------------------------------------------------
  // Public constructors
  // -------------------------------------------------------------------------

  /**
   * Builds a board from a BoardSpec (used for custom maps and predefined
   * scenarios). Land tiles and sea tiles are taken directly from the spec.
   */
  public Board(BoardSpec spec) {
    Map<IntersectionCoordinate, Intersection> intersections = new HashMap<>();
    Map<PathCoordinate, Path> paths = new HashMap<>();
    _tiles = new ArrayList<>();
    PORT_LOCATION = Collections.emptyList();

    // Land tiles must be added before sea tiles so fillSeaTile can find them.
    for (TileSpec ts : spec.getLandTiles()) {
      HexCoordinate coord = new HexCoordinate(ts.getX(), ts.getY(), ts.getZ());
      addTileFromSpec(ts, coord, intersections, paths);
    }

    for (TileSpec ts : spec.getSeaTiles()) {
      HexCoordinate coord = new HexCoordinate(ts.getX(), ts.getY(), ts.getZ());
      Tile seaTile = new Tile(coord, SEA, intersections);
      if (ts.getPortType() != null) {
        seaTile.setPorts(new Port(ts.getPortType()));
      }
      _tiles.add(seaTile);
    }

    _intersections = intersections;
    _paths = paths;
    markWaterPaths();
  }

  /**
   * Marks every path that is adjacent to at least one SEA tile as a water
   * path. Called at the end of every board build so ship placement works.
   * The two tiles adjacent to a path are the HexCoordinates common to both
   * endpoint IntersectionCoordinates.
   */
  private void markWaterPaths() {
    Set<HexCoordinate> seaCoords = new HashSet<>();
    for (Tile t : _tiles) {
      if (t.getType() == TileType.SEA) {
        seaCoords.add(t.getCoordinate());
      }
    }
    for (Map.Entry<PathCoordinate, Path> entry : _paths.entrySet()) {
      PathCoordinate pc = entry.getKey();
      Set<HexCoordinate> adj1 = new HashSet<>();
      adj1.add(pc.get_startCoord().getCoord1());
      adj1.add(pc.get_startCoord().getCoord2());
      adj1.add(pc.get_startCoord().getCoord3());
      Set<HexCoordinate> adj2 = new HashSet<>();
      adj2.add(pc.get_endCoord().getCoord1());
      adj2.add(pc.get_endCoord().getCoord2());
      adj2.add(pc.get_endCoord().getCoord3());
      adj1.retainAll(adj2);
      for (HexCoordinate hc : adj1) {
        if (seaCoords.contains(hc)) {
          entry.getValue().setWaterPath(true);
          break;
        }
      }
    }
  }

  /**
   * Builds a board from GameSettings. Dispatches to the appropriate layout.
   */
  public Board(GameSettings settings) {
    if (settings.boardLayout == BoardLayout.CUSTOM
        && settings.customBoardSpec != null) {
      // Delegate fully to spec constructor, then copy state
      Board tmp = new Board(settings.customBoardSpec);
      _tiles = tmp._tiles;
      _intersections = tmp._intersections;
      _paths = tmp._paths;
      PORT_LOCATION = tmp.PORT_LOCATION;
      return;
    }

    if (settings.boardLayout == BoardLayout.EXTENDED_56) {
      PORT_LOCATION = setExtendedPortLocations();
      buildExtended(settings);
      return;
    }

    // STANDARD (and SEAFARERS scenario loaded from spec handled by caller)
    PORT_LOCATION = setPortLocations();
    buildStandard(settings);
  }

  // -------------------------------------------------------------------------
  // Standard board build (DEPTH=2, 19 land tiles)
  // -------------------------------------------------------------------------

  private void buildStandard(GameSettings settings) {
    List<TileType> availTiles = new ArrayList<>();
    int[] rollNums = ROLL_NUMS;
    if (settings.isStandard) {
      availTiles = standardBoard();
      rollNums = Settings.STANDARD_ROLL_NUMS;
    } else {
      addTiles(availTiles, WOOD, NUM_WOOD_TILE);
      addTiles(availTiles, BRICK, NUM_BRICK_TILE);
      addTiles(availTiles, SHEEP, NUM_SHEEP_TILE);
      addTiles(availTiles, WHEAT, NUM_WHEAT_TILE);
      addTiles(availTiles, ORE, NUM_ORE_TILE);
      addTiles(availTiles, DESERT, NUM_DESERT_TILE);
      do {
        Collections.shuffle(availTiles);
      } while (availTiles.get(0) == DESERT);
    }

    Map<IntersectionCoordinate, Intersection> intersections = new HashMap<>();
    Map<PathCoordinate, Path> paths = new HashMap<>();
    _tiles = new ArrayList<>();
    runSpiralBuild(availTiles, rollNums, DEPTH, intersections, paths);
    addSeaTiles(intersections, getSeaPermutations(DEPTH), PORT_LOCATION,
        Settings.PORT_ORDER);
    _intersections = intersections;
    _paths = paths;
    markWaterPaths();
  }

  // -------------------------------------------------------------------------
  // Extended board build (DEPTH=3, 37 land tiles)
  // -------------------------------------------------------------------------

  private void buildExtended(GameSettings settings) {
    List<TileType> availTiles = new ArrayList<>();
    int[] rollNums = Settings.EXTENDED_ROLL_NUMS;
    addTiles(availTiles, WOOD, Settings.EXT_NUM_WOOD_TILE);
    addTiles(availTiles, BRICK, Settings.EXT_NUM_BRICK_TILE);
    addTiles(availTiles, SHEEP, Settings.EXT_NUM_SHEEP_TILE);
    addTiles(availTiles, WHEAT, Settings.EXT_NUM_WHEAT_TILE);
    addTiles(availTiles, ORE, Settings.EXT_NUM_ORE_TILE);
    addTiles(availTiles, DESERT, Settings.EXT_NUM_DESERT_TILE);
    do {
      Collections.shuffle(availTiles);
    } while (availTiles.get(0) == DESERT);

    Map<IntersectionCoordinate, Intersection> intersections = new HashMap<>();
    Map<PathCoordinate, Path> paths = new HashMap<>();
    _tiles = new ArrayList<>();
    runSpiralBuild(availTiles, rollNums, DEPTH_EXTENDED, intersections, paths);
    addSeaTiles(intersections, getSeaPermutations(DEPTH_EXTENDED),
        PORT_LOCATION, Settings.EXTENDED_PORT_ORDER);
    _intersections = intersections;
    _paths = paths;
    markWaterPaths();
  }

  // -------------------------------------------------------------------------
  // Shared spiral build logic (works for any depth)
  // -------------------------------------------------------------------------

  private void runSpiralBuild(List<TileType> availTiles, int[] rollNums,
      int depth, Map<IntersectionCoordinate, Intersection> intersections,
      Map<PathCoordinate, Path> paths) {
    int currRoll = 0;
    int currTile = 0;
    int currDepth = depth;
    int x = depth;
    int y = 0;
    int z = 0;
    int i;

    while (currDepth >= 0) {
      currRoll = addTile(availTiles.get(currTile), new HexCoordinate(x, y, z),
          intersections, paths, currRoll, currTile, rollNums);
      currTile++;

      for (i = 0; i < currDepth; i++) {
        y++;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      for (i = 0; i < currDepth; i++) {
        x--;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      for (i = 0; i < currDepth; i++) {
        z++;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      for (i = 0; i < currDepth; i++) {
        y--;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      for (i = 0; i < currDepth; i++) {
        x++;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      for (i = 1; i < currDepth; i++) {
        z--;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      z--;
      x--;
      currDepth--;
    }
  }

  private void addSeaTiles(Map<IntersectionCoordinate, Intersection> intersections,
      List<HexCoordinate> seaCoords, List<HexCoordinate> portLocations,
      Resource[] portOrder) {
    for (HexCoordinate hc : seaCoords) {
      Tile seaTile = new Tile(hc, SEA, intersections);
      if (portLocations.contains(hc)) {
        int order = portLocations.indexOf(hc);
        if (order < portOrder.length) {
          seaTile.setPorts(new Port(portOrder[order]));
        }
      }
      _tiles.add(seaTile);
    }
  }

  // -------------------------------------------------------------------------
  // Sea coordinate generation (generalised for any depth)
  // -------------------------------------------------------------------------

  private List<HexCoordinate> getSeaPermutations(int depth) {
    int d = depth + 1;
    List<HexCoordinate> coords = new ArrayList<>();

    // Generate all permutation groups from {0,...,d} triples where max = d
    int[][] seeds = buildSeeds(d);
    for (int[] seed : seeds) {
      int[] arr = seed.clone();
      coords.add(new HexCoordinate(arr[0], arr[1], arr[2]));
      while (permute(arr)) {
        coords.add(new HexCoordinate(arr[0], arr[1], arr[2]));
      }
    }
    return coords;
  }

  /**
   * Returns seeds for the sea ring at a given depth. The sea ring consists of
   * permutations of {0, b, d+1} for b in [0, d+1], where d = depth. This
   * matches the existing DEPTH=2 pattern: perms of {0,0,3}, {0,1,3}, {0,2,3},
   * {0,3,3}.
   */
  private int[][] buildSeeds(int depth) {
    int d = depth + 1;
    List<int[]> seeds = new ArrayList<>();
    for (int b = 0; b <= d; b++) {
      seeds.add(new int[] {0, b, d}); // already ascending: 0 <= b <= d
    }
    return seeds.toArray(new int[0][]);
  }

  // -------------------------------------------------------------------------
  // Port location helpers
  // -------------------------------------------------------------------------

  private List<HexCoordinate> setPortLocations() {
    List<HexCoordinate> toRet = new ArrayList<>();
    toRet.add(new HexCoordinate(0, 0, 3));
    toRet.add(new HexCoordinate(0, 2, 3));
    toRet.add(new HexCoordinate(0, 3, 2));
    toRet.add(new HexCoordinate(0, 3, 0));
    toRet.add(new HexCoordinate(2, 3, 0));
    toRet.add(new HexCoordinate(3, 2, 0));
    toRet.add(new HexCoordinate(3, 0, 0));
    toRet.add(new HexCoordinate(3, 0, 2));
    toRet.add(new HexCoordinate(2, 0, 3));
    return toRet;
  }

  private List<HexCoordinate> setExtendedPortLocations() {
    List<HexCoordinate> toRet = new ArrayList<>();
    toRet.add(new HexCoordinate(0, 0, 4));
    toRet.add(new HexCoordinate(0, 1, 4));
    toRet.add(new HexCoordinate(0, 3, 4));
    toRet.add(new HexCoordinate(0, 4, 2));
    toRet.add(new HexCoordinate(0, 4, 0));
    toRet.add(new HexCoordinate(2, 4, 0));
    toRet.add(new HexCoordinate(4, 2, 0));
    toRet.add(new HexCoordinate(4, 0, 0));
    toRet.add(new HexCoordinate(4, 0, 2));
    toRet.add(new HexCoordinate(3, 0, 4));
    toRet.add(new HexCoordinate(2, 0, 4));
    return toRet;
  }

  // -------------------------------------------------------------------------
  // Tile-adding helpers
  // -------------------------------------------------------------------------

  private void addTiles(List<TileType> availTiles, TileType type, int numTiles) {
    for (int i = 0; i < numTiles; i++) {
      availTiles.add(type);
    }
  }

  private int addTile(TileType tileType, HexCoordinate coord,
      Map<IntersectionCoordinate, Intersection> intersections,
      Map<PathCoordinate, Path> paths, Integer currRoll, Integer currTile,
      int[] rollNums) {
    if (tileType != DESERT) {
      _tiles.add(new Tile(rollNums[currRoll], coord, intersections, paths,
          tileType));
      return currRoll + 1;
    } else {
      _tiles.add(new Tile(0, coord, intersections, paths, tileType, true));
      return currRoll;
    }
  }

  /** Adds a tile from a BoardSpec TileSpec, including GOLD_FIELD and DESERT. */
  private void addTileFromSpec(TileSpec ts, HexCoordinate coord,
      Map<IntersectionCoordinate, Intersection> intersections,
      Map<PathCoordinate, Path> paths) {
    TileType type = ts.getType();
    if (type == DESERT) {
      _tiles.add(new Tile(0, coord, intersections, paths, DESERT, true));
    } else {
      _tiles.add(new Tile(ts.getRollNum(), coord, intersections, paths, type));
    }
  }

  // -------------------------------------------------------------------------
  // Permutation helper (unchanged)
  // -------------------------------------------------------------------------

  private boolean permute(int[] data) {
    int k = data.length - 2;
    while (data[k] >= data[k + 1]) {
      k--;
      if (k < 0) {
        return false;
      }
    }
    int l = data.length - 1;
    while (data[k] >= data[l]) {
      l--;
    }
    swap(data, k, l);
    int length = data.length - (k + 1);
    for (int i = 0; i < length / 2; i++) {
      swap(data, k + 1 + i, data.length - i - 1);
    }
    return true;
  }

  private void swap(int[] data, int idx1, int idx2) {
    int tmp = data[idx1];
    data[idx1] = data[idx2];
    data[idx2] = tmp;
  }

  // -------------------------------------------------------------------------
  // Standard board tile order
  // -------------------------------------------------------------------------

  private List<TileType> standardBoard() {
    List<TileType> tiles = new ArrayList<>();
    tiles.add(WHEAT);
    tiles.add(SHEEP);
    tiles.add(WOOD);
    tiles.add(BRICK);
    tiles.add(DESERT);
    tiles.add(BRICK);
    tiles.add(ORE);
    tiles.add(WHEAT);
    tiles.add(WOOD);
    tiles.add(ORE);
    tiles.add(WHEAT);
    tiles.add(SHEEP);
    tiles.add(BRICK);
    tiles.add(ORE);
    tiles.add(WOOD);
    tiles.add(SHEEP);
    tiles.add(SHEEP);
    tiles.add(WOOD);
    tiles.add(WHEAT);
    return tiles;
  }

  // -------------------------------------------------------------------------
  // Board state accessors
  // -------------------------------------------------------------------------

  public void notifyTiles(int roll) {
    for (Tile t : _tiles) {
      if (t.getRollNumber() == roll) {
        t.notifyIntersections();
      }
    }
  }

  public Collection<Tile> getTiles() {
    return Collections.unmodifiableCollection(_tiles);
  }

  public Map<IntersectionCoordinate, Intersection> getIntersections() {
    return Collections.unmodifiableMap(_intersections);
  }

  public Map<PathCoordinate, Path> getPaths() {
    return Collections.unmodifiableMap(_paths);
  }

  public HexCoordinate findRobber() {
    for (Tile t : _tiles) {
      if (t.hasRobber()) {
        return t.getCoordinate();
      }
    }
    return null;
  }

  public Set<Integer> moveRobber(HexCoordinate coord) {
    Set<Integer> playersOnTile = Collections.emptySet();
    for (Tile t : _tiles) {
      if (t.hasRobber()) {
        if (t.getCoordinate().equals(coord)) {
          throw new IllegalArgumentException(
              "The robber must be moved to a new location.");
        }
        t.hasRobber(false);
      } else if (t.getCoordinate().equals(coord)) {
        t.hasRobber(true);
        playersOnTile = t.getPlayersOnTile();
      }
    }
    return playersOnTile;
  }

  public int longestPath(Player player) {
    int max = 0;
    for (Path path : _paths.values()) {
      int longestPath = path.getLongestPath(player);
      if (longestPath > max) {
        max = longestPath;
      }
    }
    return max;
  }

  /**
   * Returns the connected components of non-SEA land tiles. Each inner set
   * contains tiles that are reachable from each other by hex adjacency. Used by
   * Seafarers island discovery.
   */
  public List<Set<Tile>> getIslands() {
    List<Tile> landTiles = new ArrayList<>();
    for (Tile t : _tiles) {
      if (t.getType() != TileType.SEA) {
        landTiles.add(t);
      }
    }

    Set<Tile> visited = new HashSet<>();
    List<Set<Tile>> islands = new ArrayList<>();

    for (Tile start : landTiles) {
      if (visited.contains(start)) {
        continue;
      }
      Set<Tile> island = new HashSet<>();
      Queue<Tile> queue = new ArrayDeque<>();
      queue.add(start);
      visited.add(start);
      while (!queue.isEmpty()) {
        Tile curr = queue.poll();
        island.add(curr);
        for (Tile neighbor : getNeighborLandTiles(curr)) {
          if (!visited.contains(neighbor)) {
            visited.add(neighbor);
            queue.add(neighbor);
          }
        }
      }
      islands.add(island);
    }
    return islands;
  }

  private List<Tile> getNeighborLandTiles(Tile tile) {
    HexCoordinate c = tile.getCoordinate();
    List<Tile> neighbors = new ArrayList<>();
    for (int[] offset : HEX_NEIGHBORS) {
      HexCoordinate neighborCoord = new HexCoordinate(
          c.getX() + offset[0], c.getY() + offset[1], c.getZ() + offset[2]);
      for (Tile t : _tiles) {
        if (t.getType() != TileType.SEA && t.getCoordinate().equals(neighborCoord)) {
          neighbors.add(t);
          break;
        }
      }
    }
    return neighbors;
  }

  @Override
  public String toString() {
    StringBuilder toRet = new StringBuilder();
    toRet.append(_tiles.toString());
    toRet.append("\n");
    toRet.append(_intersections.keySet().size());
    toRet.append("\n");
    toRet.append(_paths.keySet().size());
    return toRet.toString();
  }
}
