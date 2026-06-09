package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Toggles the "wants to special build" flag for a non-active player.
 * Any player except the current active player may call this during the
 * active player's turn. When the active player ends their turn the server
 * queues all flagged players for the Special Building Phase.
 */
public class ToggleSpecialBuild implements Action {

  public static final String ID = "toggleSpecialBuild";

  private final Referee _ref;
  private final Player _player;

  public ToggleSpecialBuild(Referee ref, int playerID) {
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(
          "No player with id: " + playerID);
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isSpecialBuildPhase) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "Special Building Phase is not enabled.", null));
    }
    Player active = _ref.currentPlayer();
    if (active != null && active.equals(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "The active player cannot toggle special build intent.", null));
    }
    boolean newVal = !_player.wantsToSpecialBuild();
    _player.setWantsToSpecialBuild(newVal);

    String msg = newVal
        ? "You have flagged intent to build this turn."
        : "You have cleared your build intent.";

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      String pMsg = p.equals(_player) ? msg
          : String.format("%s %s flagged build intent.",
              _player.getName(), newVal ? "has" : "cleared");
      toRet.put(p.getID(), new ActionResponse(true, pMsg, null));
    }
    return toRet;
  }
}
