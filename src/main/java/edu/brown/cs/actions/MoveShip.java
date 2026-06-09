package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.board.PathCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

public class MoveShip implements Action {

  public static final String ID = "moveShip";
  private final Referee _ref;
  private final Player _player;
  private final Path _source;
  private final Path _dest;

  public MoveShip(Referee ref, int playerID,
      IntersectionCoordinate srcStart, IntersectionCoordinate srcEnd,
      IntersectionCoordinate dstStart, IntersectionCoordinate dstEnd) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player with id: %d", playerID));
    }
    _source = ref.getBoard().getPaths().get(new PathCoordinate(srcStart, srcEnd));
    if (_source == null) {
      throw new IllegalArgumentException("Source path does not exist");
    }
    _dest = ref.getBoard().getPaths().get(new PathCoordinate(dstStart, dstEnd));
    if (_dest == null) {
      throw new IllegalArgumentException("Destination path does not exist");
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    GameStatus status = _ref.getGameStatus();
    if (status == GameStatus.PROGRESS || status == GameStatus.SPECIAL_BUILD) {
      if (!_ref.currentPlayer().equals(_player)) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false, "You cannot move a ship when it is not your turn", null));
      }
    }
    if (!_source.canMoveShip(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "You cannot move that ship", null));
    }
    // Temporarily remove the ship to check if destination is valid
    _source.removeShip();
    boolean canPlace = _dest.canPlaceShip(_player);
    if (!canPlace) {
      // Put it back — destination is not valid
      _source.placeShip(_player);
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "You cannot place the ship there", null));
    }
    _dest.placeShip(_player);

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    String broadcastMsg = String.format("%s moved a Ship", _player.getName());
    for (Player p : _ref.getPlayers()) {
      String msg = p.equals(_player) ? "You moved a Ship" : broadcastMsg;
      toReturn.put(p.getID(), new ActionResponse(true, msg, null));
    }
    return toReturn;
  }
}
