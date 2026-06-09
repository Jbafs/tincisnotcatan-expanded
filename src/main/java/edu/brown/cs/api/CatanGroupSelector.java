package edu.brown.cs.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.google.gson.JsonObject;

import edu.brown.cs.networking.DistinctRandom;
import edu.brown.cs.networking.Group;
import edu.brown.cs.networking.GroupSelector;
import edu.brown.cs.networking.RequestProcessor;
import edu.brown.cs.networking.User;
import edu.brown.cs.networking.UserGroup.UserGroupBuilder;

public class CatanGroupSelector implements GroupSelector {

  private static final String NUM_PLAYERS          = "numPlayersDesired";
  private static final String VICTORY_POINTS       = "victoryPoints";
  private static final String IS_DECIMAL           = "isDecimal";
  private static final String IS_DYNAMIC           = "isDynamic";
  private static final String IS_STANDARD          = "isStandard";
  private static final String BOARD_LAYOUT         = "boardLayout";
  private static final String IS_SEAFARERS         = "isSeafarers";
  private static final String IS_SPECIAL_BUILD     = "isSpecialBuildPhase";
  private static final String SCENARIO_ID          = "scenarioId";
  private static final String CUSTOM_BOARD_SPEC    = "customBoardSpec";
  private static final String GAME_REQUEST_ID      = "desiredGroupId";
  private static final String GAME_NAME_IDENTIFIER = "groupName";
  private final Collection<RequestProcessor> catanProcessors;


  public CatanGroupSelector() {
    catanProcessors = new ArrayList<>();
    catanProcessors.add(new GetGameStateProcessor());
    catanProcessors.add(new ActionProcessor());
    catanProcessors.add(new ChatProcessor());
    catanProcessors.add(new GameOverProcessor());
  }


  @Override
  public Group selectFor(User u, Collection<Group> coll) {
    Optional<Group> usersExistingGroup =
        coll.stream().filter(g -> g.hasUser(u)).findFirst();
    if (usersExistingGroup.isPresent()) {
      return usersExistingGroup.get();
    }
    System.out.println("No game has " + u);
    if (u.getFieldsAsJson().has(GAME_REQUEST_ID)) {
      Optional<Group> requested = coll.stream()
          .filter(ug -> !ug.isFull()
              && ug.identifier().equals(u.getField(GAME_REQUEST_ID)))
          .findFirst();
      if (requested.isPresent()) {
        return requested.get();
      }
      System.out.println("returning null");
      return null;
    }
    System.out.println("No game requested.");

    int desiredSize = -1;
    try {
      desiredSize = Integer.parseInt(u.getField(NUM_PLAYERS));
    } catch (NumberFormatException e) {
      return null;
    }
    if (desiredSize < 2 || desiredSize > 6) {
      System.out
          .println("ERROR: Size requested out of bounds : " + desiredSize);
      return null;
    }

    int victoryPoints = Integer.parseInt(u.getField(VICTORY_POINTS));
    boolean isDecimal = Boolean.parseBoolean(u.getField(IS_DECIMAL));
    boolean isDynamic = Boolean.parseBoolean(u.getField(IS_DYNAMIC));
    boolean isStandard = Boolean.parseBoolean(u.getField(IS_STANDARD));

    // New settings (optional — may not be present for old clients)
    String boardLayout = safeGet(u, BOARD_LAYOUT, "STANDARD");
    boolean isSeafarers = Boolean.parseBoolean(safeGet(u, IS_SEAFARERS, "false"));
    boolean isSpecialBuildPhase =
        Boolean.parseBoolean(safeGet(u, IS_SPECIAL_BUILD, "false"));
    String scenarioId = safeGet(u, SCENARIO_ID, null);
    String customBoardSpecJson = safeGet(u, CUSTOM_BOARD_SPEC, null);

    // name the game
    String name = u.hasField(GAME_NAME_IDENTIFIER)
        ? u.getField(GAME_NAME_IDENTIFIER) : "Unnamed game";

    // Build settings JSON
    JsonObject settings = new JsonObject();
    settings.addProperty("numPlayers", desiredSize);
    settings.addProperty("victoryPoints", victoryPoints);
    settings.addProperty("isDecimal", isDecimal);
    settings.addProperty("isDynamic", isDynamic);
    settings.addProperty("isStandard", isStandard);
    settings.addProperty("boardLayout", boardLayout);
    settings.addProperty("isSeafarers", isSeafarers);
    settings.addProperty("isSpecialBuildPhase", isSpecialBuildPhase);
    if (scenarioId != null) {
      settings.addProperty("scenarioId", scenarioId);
    }
    if (customBoardSpecJson != null && !customBoardSpecJson.isEmpty()) {
      try {
        com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
        settings.add("customBoardSpec", parser.parse(customBoardSpecJson));
      } catch (Exception e) {
        System.out.println("Failed to parse customBoardSpec: " + e.getMessage());
      }
    }

    System.out.println("MAKING NEW GAME!");
    return new UserGroupBuilder(CatanAPI.class)
        .withSize(desiredSize)
        .withRequestProcessors(
            Collections.unmodifiableCollection(catanProcessors))
        .withName(name)
        .withApiSettings(settings)
        .withUniqueIdentifier(DistinctRandom.getString()).build();
  }

  private static String safeGet(User u, String field, String defaultVal) {
    try {
      String val = u.getField(field);
      return val != null ? val : defaultVal;
    } catch (Exception e) {
      return defaultVal;
    }
  }

}
