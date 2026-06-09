package edu.brown.cs.board;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable description of a complete board layout used by Board constructor.
 * Supports custom maps from the editor and predefined scenarios.
 */
public class BoardSpec {
  private final String _name;
  private final List<TileSpec> _tiles;
  private final int _suggestedPlayers;
  private final int _suggestedVictoryPoints;

  public BoardSpec(String name, List<TileSpec> tiles, int suggestedPlayers,
      int suggestedVictoryPoints) {
    _name = name;
    _tiles = new ArrayList<>(tiles);
    _suggestedPlayers = suggestedPlayers;
    _suggestedVictoryPoints = suggestedVictoryPoints;
  }

  public String getName() {
    return _name;
  }

  public List<TileSpec> getTiles() {
    return new ArrayList<>(_tiles);
  }

  public List<TileSpec> getLandTiles() {
    return _tiles.stream()
        .filter(t -> t.getType() != TileType.SEA)
        .collect(Collectors.toList());
  }

  public List<TileSpec> getSeaTiles() {
    return _tiles.stream()
        .filter(t -> t.getType() == TileType.SEA)
        .collect(Collectors.toList());
  }

  public int getSuggestedPlayers() {
    return _suggestedPlayers;
  }

  public int getSuggestedVictoryPoints() {
    return _suggestedVictoryPoints;
  }

  /**
   * Deserializes a BoardSpec from JSON (produced by the map editor or a
   * scenario file).
   */
  public static BoardSpec fromJson(JsonObject json) {
    String name = json.has("name") ? json.get("name").getAsString() : "Custom";
    int suggestedPlayers = json.has("suggestedPlayers")
        ? json.get("suggestedPlayers").getAsInt() : 4;
    int suggestedVP = json.has("suggestedVictoryPoints")
        ? json.get("suggestedVictoryPoints").getAsInt() : 10;

    List<TileSpec> tiles = new ArrayList<>();
    JsonArray tilesArr = json.getAsJsonArray("tiles");
    for (int i = 0; i < tilesArr.size(); i++) {
      tiles.add(TileSpec.fromJson(tilesArr.get(i).getAsJsonObject()));
    }
    return new BoardSpec(name, tiles, suggestedPlayers, suggestedVP);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("name", _name);
    obj.addProperty("suggestedPlayers", _suggestedPlayers);
    obj.addProperty("suggestedVictoryPoints", _suggestedVictoryPoints);
    JsonArray tilesArr = new JsonArray();
    for (TileSpec ts : _tiles) {
      tilesArr.add(ts.toJson());
    }
    obj.add("tiles", tilesArr);
    return obj;
  }
}
