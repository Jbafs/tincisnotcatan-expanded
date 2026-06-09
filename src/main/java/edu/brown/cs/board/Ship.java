package edu.brown.cs.board;

import edu.brown.cs.catan.Player;

/**
 * Represents a ship piece placed on a water path.
 */
public class Ship {
  private final Player _owner;

  public Ship(Player owner) {
    _owner = owner;
  }

  public Player getPlayer() {
    return _owner;
  }
}
