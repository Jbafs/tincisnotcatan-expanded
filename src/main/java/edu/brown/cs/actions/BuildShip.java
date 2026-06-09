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

public class BuildShip implements Action {

  public static final String ID = "buildShip";
  private final Referee _ref;
  private final Player _player;
  private final Path _path;

  public BuildShip(Referee ref, int playerID, IntersectionCoordinate start,
      IntersectionCoordinate end) {
    assert ref != null && start != null && end != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player with id: %d", playerID));
    }
    _path = ref.getBoard().getPaths().get(new PathCoordinate(start, end));
    if (_path == null) {
      throw new IllegalArgumentException(
          "No path between the given coordinates");
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    GameStatus status = _ref.getGameStatus();
    if (status == GameStatus.PROGRESS || status == GameStatus.SPECIAL_BUILD) {
      if (!_ref.currentPlayer().equals(_player)) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false, "You cannot build when it is not your turn", null));
      }
    }
    if (_player.numShips() <= 0) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "You have no ships remaining", null));
    }
    if (!_player.canBuildShip()) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "You cannot afford to build a ship", null));
    }
    if (!_path.canPlaceShip(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "You cannot build a ship there", null));
    }
    _player.buildShip();
    _player.useShip();
    _path.placeShip(_player);

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    String broadcastMsg = String.format("%s built a Ship", _player.getName());
    for (Player p : _ref.getPlayers()) {
      String msg = p.equals(_player) ? "You built a Ship" : broadcastMsg;
      toReturn.put(p.getID(), new ActionResponse(true, msg, null));
    }
    return toReturn;
  }
}
