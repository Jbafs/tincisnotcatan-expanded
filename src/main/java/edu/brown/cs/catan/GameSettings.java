package edu.brown.cs.catan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.brown.cs.board.BoardSpec;

/**
 * Represents one game of Catan's settings.
 */
public class GameSettings {

  public enum BoardLayout {
    STANDARD, EXTENDED_56, SEAFARERS, CUSTOM
  }

  public final int numPlayers;
  public final int winningPointCount;
  public final String[] COLORS = {
      "#BF2720", // red   (player 1)
      "#115EC9", // blue  (player 2)
      "#DFA629", // gold  (player 3)
      "#EDEAD9", // white (player 4)
      "#2E8B57", // green (player 5)
      "#9400D3"  // purple(player 6)
  };
  public final boolean isDecimal;
  public final boolean isDynamic;
  public final boolean isStandard;
  public final BoardLayout boardLayout;
  public final boolean isSeafarers;
  public final boolean isSpecialBuildPhase;
  public final String scenarioId;
  public final BoardSpec customBoardSpec;

  public GameSettings(JsonObject settings) {
    int numPlayers = Settings.DEFAULT_NUM_PLAYERS;
    try {
      numPlayers = settings.get("numPlayers").getAsInt();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing numPlayers parameter");
    }
    int winningPointCount = Settings.WINNING_POINT_COUNT;
    try {
      winningPointCount = settings.get("victoryPoints").getAsInt();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing victoryPoints parameter");
    }
    boolean isDecimal = false;
    try {
      isDecimal = settings.get("isDecimal").getAsBoolean();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isDecimal parameter");
    }
    boolean isDynamic = false;
    if (isDecimal) {
      try {
        isDynamic = settings.get("isDynamic").getAsBoolean();
      } catch (NullPointerException e) {
        System.out.println("SETTINGS missing isDynamic parameter");
      }
    }
    boolean isStandard = false;
    try {
      isStandard = settings.get("isStandard").getAsBoolean();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isStandard parameter");
    }

    // Board layout
    BoardLayout boardLayout = BoardLayout.STANDARD;
    try {
      JsonElement bl = settings.get("boardLayout");
      if (bl != null && !bl.isJsonNull()) {
        boardLayout = BoardLayout.valueOf(bl.getAsString());
      }
    } catch (IllegalArgumentException e) {
      System.out.println("SETTINGS invalid boardLayout value");
    }

    boolean isSeafarers = false;
    try {
      JsonElement sf = settings.get("isSeafarers");
      if (sf != null && !sf.isJsonNull()) {
        isSeafarers = sf.getAsBoolean();
      }
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isSeafarers parameter");
    }

    boolean isSpecialBuildPhase = false;
    try {
      JsonElement sbp = settings.get("isSpecialBuildPhase");
      if (sbp != null && !sbp.isJsonNull()) {
        isSpecialBuildPhase = sbp.getAsBoolean();
      }
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isSpecialBuildPhase parameter");
    }

    String scenarioId = null;
    try {
      JsonElement sid = settings.get("scenarioId");
      if (sid != null && !sid.isJsonNull()) {
        scenarioId = sid.getAsString();
      }
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing scenarioId parameter");
    }

    BoardSpec customBoardSpec = null;
    try {
      JsonElement cbs = settings.get("customBoardSpec");
      if (cbs != null && !cbs.isJsonNull()) {
        customBoardSpec = BoardSpec.fromJson(cbs.getAsJsonObject());
      }
    } catch (Exception e) {
      System.out.println("SETTINGS failed to parse customBoardSpec: " + e.getMessage());
    }

    this.winningPointCount = winningPointCount;
    this.numPlayers = numPlayers;
    this.isDecimal = isDecimal;
    this.isDynamic = isDynamic;
    this.isStandard = isStandard;
    this.boardLayout = boardLayout;
    this.isSeafarers = isSeafarers;
    this.isSpecialBuildPhase = isSpecialBuildPhase;
    this.scenarioId = scenarioId;
    this.customBoardSpec = customBoardSpec;
  }

  // Default Settings
  public GameSettings() {
    this.numPlayers = Settings.DEFAULT_NUM_PLAYERS;
    this.winningPointCount = Settings.WINNING_POINT_COUNT;
    this.isDecimal = false;
    this.isDynamic = false;
    this.isStandard = false;
    this.boardLayout = BoardLayout.STANDARD;
    this.isSeafarers = false;
    this.isSpecialBuildPhase = false;
    this.scenarioId = null;
    this.customBoardSpec = null;
  }

}
