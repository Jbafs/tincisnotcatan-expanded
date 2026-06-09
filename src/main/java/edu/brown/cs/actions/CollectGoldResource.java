package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * FollowUpAction triggered when a gold field tile is rolled.
 * The player chooses which resource(s) to collect (1 per settlement, 2 per city).
 * The client sends: {"action":"collectGoldResource","player":N,"resources":{"WOOD":1,...}}
 */
public class CollectGoldResource implements FollowUpAction {

  public static final String ID = "collectGoldResource";

  private final int _playerID;
  private final int _numGold;
  private Referee _ref;
  private Player _player;
  private JsonObject _params;
  private boolean _isSetUp = false;

  public CollectGoldResource(int playerID, int numGold) {
    _playerID = playerID;
    _numGold = numGold;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject params) {
    if (playerID != _playerID) {
      throw new IllegalArgumentException("Wrong player for gold collection");
    }
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player with id: %d", playerID));
    }
    _params = params;
    _isSetUp = true;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetUp) {
      throw new UnsupportedOperationException(
          "CollectGoldResource must be set up before execution.");
    }

    Map<Resource, Integer> collected = new HashMap<>();
    if (_params != null && _params.has("resources")) {
      JsonObject resObj = _params.get("resources").getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : resObj.entrySet()) {
        try {
          Resource res = Resource.valueOf(entry.getKey().toUpperCase());
          int count = entry.getValue().getAsInt();
          if (count > 0) {
            collected.put(res, count);
          }
        } catch (IllegalArgumentException e) {
          // ignore unknown resource names
        }
      }
    }

    // Award chosen resources (capped at numGold total)
    int total = 0;
    for (Map.Entry<Resource, Integer> entry : collected.entrySet()) {
      int toAdd = Math.min(entry.getValue(), _numGold - total);
      if (toAdd > 0) {
        _player.addResource(entry.getKey(), toAdd, _ref.getBank());
        total += toAdd;
      }
    }

    _ref.removeFollowUp(this);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true,
          p.equals(_player) ? "You collected resources from the gold field."
              : _player.getName() + " collected resources from the gold field.",
          null));
    }
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject data = new JsonObject();
    data.addProperty("message",
        String.format("You are on a gold field! Choose %d resource(s) to collect.", _numGold));
    data.addProperty("numGold", _numGold);
    return data;
  }

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public int getPlayerID() {
    return _playerID;
  }

  @Override
  public String getVerb() {
    return "collect gold field resources";
  }
}
