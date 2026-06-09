package edu.brown.cs.catan;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.board.Board;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.gamestats.GameStats;

public interface Referee {

  void startNextTurn();

  Turn getTurn();

  Player currentPlayer();

  void playDevCard();

  int addPlayer(String name);

  int addPlayer(String name, String color);

  DevelopmentCard getDevCard();

  boolean devCardDeckIsEmpty();

  GameSettings getGameSettings();

  Referee getReadOnlyReferee();

  Map<Resource, Double> getBankRates(int playerID);

  Board getBoard();

  Player getPlayerByID(int id);

  Collection<Player> getPlayers();

  boolean hasLongestRoad(int playerID);

  boolean hasLargestArmy(int playerID);

  int getNumPublicPoints(int playerID);

  int getNumTotalPoints(int playerID);

  Bank getBank();

  GameStatus getGameStatus();

  void setGameStatus(GameStatus state);

  FollowUpAction getNextFollowUp(int playerID);

  void addFollowUp(Collection<FollowUpAction> actions);

  public void removeFollowUp(FollowUpAction action);

  List<Integer> getTurnOrder();

  Setup getSetup();

  Player getWinner();

  GameStats getGameStats();

  boolean removePlayer(int id);

  List<Integer> getSpecialBuildQueue();

  /**
   * Removes playerID from the head of the special build queue, clears their
   * flag, and transitions back to PROGRESS when the queue empties.
   * Returns true when the special build phase has ended.
   */
  boolean advanceSpecialBuild(int playerID);

  /**
   * Called when a settlement is placed during PROGRESS on a Seafarers board.
   * Returns true if this is the player's first settlement on a new island.
   */
  boolean notifyIslandDiscovery(Player p, Intersection intersection);

  public enum GameStatus {
    WAITING,      // Waiting for players (pre-game)
    SETUP,        // Placement of settlements
    PROGRESS,     // Regular game in progress
    SPECIAL_BUILD // Between turns: opted-in players build before next roll
  }

}
