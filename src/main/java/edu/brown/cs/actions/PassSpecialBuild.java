package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Called by the current special-build player to end their mini-turn.
 * When the last player passes, the Special Building Phase ends and the
 * next main player's RollDice follow-up is queued.
 */
public class PassSpecialBuild implements Action {

  public static final String ID = "passSpecialBuild";

  private final Referee _ref;
  private final Player _player;

  public PassSpecialBuild(Referee ref, int playerID) {
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(
          "No player with id: " + playerID);
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (_ref.getGameStatus() != GameStatus.SPECIAL_BUILD) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "Not currently in a Special Building Phase.", null));
    }
    List<Integer> queue = _ref.getSpecialBuildQueue();
    if (queue.isEmpty() || queue.get(0) != _player.getID()) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false, "It is not your special build turn.", null));
    }

    boolean phaseOver = _ref.advanceSpecialBuild(_player.getID());

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    if (phaseOver) {
      // Special build phase ended — add RollDice for the main player
      Player mainPlayer = _ref.currentPlayer();
      boolean hasKnight = mainPlayer != null
          && mainPlayer.getDevCards().get(DevelopmentCard.KNIGHT) != 0;
      if (mainPlayer != null) {
        FollowUpAction nextFollowUp = hasKnight
            ? new KnightOrDice(mainPlayer.getID())
            : new RollDice(mainPlayer.getID());
        Collection<FollowUpAction> followUps = new ArrayList<>();
        followUps.add(nextFollowUp);
        _ref.addFollowUp(followUps);
      }
      for (Player p : _ref.getPlayers()) {
        String msg = p.equals(_player)
            ? "Special building phase ended. It is now " +
              (mainPlayer != null ? mainPlayer.getName() : "the next player") + "'s turn."
            : "Special building phase ended. It is now " +
              (mainPlayer != null ? mainPlayer.getName() : "the next player") + "'s turn.";
        toRet.put(p.getID(), new ActionResponse(true, msg, null));
      }
    } else {
      // Next special-build player's turn
      Player nextSpecialBuilder = _ref.currentPlayer();
      for (Player p : _ref.getPlayers()) {
        String msg = p.equals(_player)
            ? "You have passed your special build turn."
            : String.format("%s passed. It is now %s's special build turn.",
                _player.getName(),
                nextSpecialBuilder != null ? nextSpecialBuilder.getName() : "?");
        toRet.put(p.getID(), new ActionResponse(true, msg, null));
      }
    }
    return toRet;
  }
}
