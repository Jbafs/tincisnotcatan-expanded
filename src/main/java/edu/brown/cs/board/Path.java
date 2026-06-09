package edu.brown.cs.board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Setup;

/**
 * Class paths between intersection.
 *
 * @author anselvahle
 *
 */
public class Path {
  private Intersection _start;
  private Intersection _end;
  private Road _road;
  private Ship _ship;
  private boolean _isWaterPath;

  /**
   * Constructor for the class.
   *
   * @param start
   *          Intersection where the path begins.
   * @param end
   *          Intersection where the path ends.
   */
  public Path(Intersection start, Intersection end) {
    _start = start;
    _end = end;
    _road = null;
    start.addPath(this);
    end.addPath(this);
  }

  /**
   * Gets the road on this path.
   *
   * @return the road, else null.
   */
  public Road getRoad() {
    return _road;
  }

  /**
   * Gets the ship on this path.
   *
   * @return the ship, else null.
   */
  public Ship getShip() {
    return _ship;
  }

  /** Marks this path as a water path (adjacent to at least one SEA tile). */
  public void setWaterPath(boolean isWater) {
    _isWaterPath = isWater;
  }

  /** Returns true if this path is adjacent to at least one SEA tile. */
  public boolean isWaterPath() {
    return _isWaterPath;
  }

  /**
   * Returns true if the given player can place a ship on this path.
   * Requires: water path, no existing road or ship, and connectivity to a
   * player settlement/city or existing ship.
   */
  public boolean canPlaceShip(Player p) {
    if (!_isWaterPath || _road != null || _ship != null) {
      return false;
    }
    return hasShipConnectivity(_start, p) || hasShipConnectivity(_end, p);
  }

  private boolean hasShipConnectivity(Intersection i, Player p) {
    if (i.getBuilding() != null && i.getBuilding().getPlayer().equals(p)) {
      return true;
    }
    for (Path adj : i.getPaths()) {
      if (adj != this && adj._ship != null && adj._ship.getPlayer().equals(p)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if a ship can be placed adjacent to the last-built settlement
   * during setup (mirrors canPlaceSetupRoad).
   */
  public boolean canPlaceSetupShip(Setup setup) {
    if (!_isWaterPath) {
      return false;
    }
    if (setup.getLastBuiltSettlement() != null) {
      if (getStart().equals(setup.getLastBuiltSettlement())
          || getEnd().equals(setup.getLastBuiltSettlement())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Places a ship for the given player. Validates via canPlaceShip.
   */
  public void placeShip(Player p) {
    if (canPlaceShip(p)) {
      _ship = new Ship(p);
    }
  }

  /**
   * Returns true if this path's ship can be moved by the given player.
   * A ship can be moved when it is at an open end of a ship chain
   * (at least one endpoint has no other ship or building of the player).
   */
  public boolean canMoveShip(Player p) {
    if (_ship == null || !_ship.getPlayer().equals(p)) {
      return false;
    }
    return isOpenEnd(_start, p) || isOpenEnd(_end, p);
  }

  private boolean isOpenEnd(Intersection i, Player p) {
    if (i.getBuilding() != null && i.getBuilding().getPlayer().equals(p)) {
      return false;
    }
    for (Path adj : i.getPaths()) {
      if (adj != this && adj._ship != null && adj._ship.getPlayer().equals(p)) {
        return false;
      }
    }
    return true;
  }

  /** Removes the ship from this path and returns it (for move validation). */
  public void removeShip() {
    _ship = null;
  }

  @Override
  public int hashCode() {
    return 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Path other = (Path) obj;
    if (_end == null || _start == null) {
      if (other._end != null || other._start != null) {
        return false;
      }
    }
    if (((_end.equals(other._end) && _start.equals(other._start)))
        || (_end.equals(other._start) && _start.equals(other._end))) {
      return true;
    }
    return false;
  }

  /**
   * Finds the longest road associated with the input player.
   *
   * @param player
   *          Player whose roads to evaluate.
   * @return Int that is the length of the longest road for this player.
   */
  public int getLongestPath(Player player) {
    Collection<Path> visited = new ArrayList<>();
    List<Path> queue = new ArrayList<>();
    Map<Path, Integer> counts = new HashMap<>();
    queue.add(this);
    counts.put(this, 0);
    while (!queue.isEmpty()) {
      Path toVisit = queue.remove(0);
      visited.add(toVisit);
      int curr = counts.get(toVisit) + 1;
      for (Path p : toVisit.getStart().getPaths()) {
        if (p.getRoad() != null && p.getRoad().getPlayer().equals(player)) {
          if (!visited.contains(p)) {
            visited.add(p);
            counts.put(p, curr);
            queue.add(0, p);
          } else {
            if (curr - counts.get(p) == 5) {
              counts.put(p, curr);
            }
          }
        }
      }
      for (Path p : toVisit.getEnd().getPaths()) {
        if (p.getRoad() != null && p.getRoad().getPlayer().equals(player)) {
          if (!visited.contains(p)) {
            visited.add(p);
            counts.put(p, curr);
            queue.add(0, p);
          } else {
            if (curr - counts.get(p) == 5) {
              counts.put(p, curr);
            }
          }
        }
      }
    }
    int max = 0;
    for (Path p : visited) {
      int longest = counts.get(p);
      if (longest > max) {
        max = longest;
      }
    }
    return max;
  }

  /**
   * States whether or not a road can be placed in this location during setup.
   *
   * @param setup
   *          Setup to be evaluated.
   * @return A boolean stating whether or not a road can be built here.
   */
  public boolean canPlaceSetupRoad(Setup setup) {
    if (setup.getLastBuiltSettlement()!= null) {
      if (getStart().equals(setup.getLastBuiltSettlement())
          || getEnd().equals(setup.getLastBuiltSettlement())) {
        return true;
      }
    }
    return false;
  }

  /**
   * A boolean stating whether or not the input player can build a road on this
   * path.
   *
   * @param p
   *          Player who wants to build a road.
   * @return A boolean stating whether or not the player can build a road on
   *         this path.
   */
  public boolean canPlaceRoad(Player p) {
    if (_road != null) {
      return false;
    }

    if (_start.getBuilding() != null
        && _start.getBuilding().getPlayer().equals(p)) {
      return true;
    } else if (_start.getBuilding() == null) {
      for (Path path : _start.getPaths()) {
        if (path.getRoad() != null && path.getRoad().getPlayer().equals(p)) {
          return true;
        }
      }
    }
    if (_end.getBuilding() != null && _end.getBuilding().getPlayer().equals(p)) {
      return true;
    } else if (_end.getBuilding() == null) {
      for (Path path : _end.getPaths()) {
        if (path.getRoad() != null && path.getRoad().getPlayer().equals(p)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Places a road associated with the input player on this path.
   *
   * @param p
   *          Player who is trying to build a road on this path.
   */
  public void placeRoad(Player p) {
    if (canPlaceRoad(p)) {
      _road = new Road(p);
    }
  }

  /**
   * Gets the start of the path.
   *
   * @return the starting Intersection of the path.
   */
  public Intersection getStart() {
    return _start;
  }

  /**
   * Gets the end of this path.
   *
   * @return the ending intersection of this path.
   */
  public Intersection getEnd() {
    return _end;
  }

  /**
   * Gets the end opposite the input end.
   *
   * @param end
   *          Endpoint that you want the opposite of.
   * @return The intersection that is the on the opposite side of the path, else
   *         null.
   */
  public Intersection getOtherEnd(Intersection end) {
    if (end.equals(_start)) {
      return _end;
    } else if (end.equals(_end)) {
      return _start;
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    return _start + " <-->" + _end;
  }

}
