package edu.brown.cs.board;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.brown.cs.catan.Resource;

/**
 * Immutable description of a single tile in a BoardSpec.
 */
public class TileSpec {
  private final int _x;
  private final int _y;
  private final int _z;
  private final TileType _type;
  private final int _rollNum;
  private final Resource _portType; // null means no port

  public TileSpec(int x, int y, int z, TileType type, int rollNum,
      Resource portType) {
    _x = x;
    _y = y;
    _z = z;
    _type = type;
    _rollNum = rollNum;
    _portType = portType;
  }

  public int getX() {
    return _x;
  }

  public int getY() {
    return _y;
  }

  public int getZ() {
    return _z;
  }

  public TileType getType() {
    return _type;
  }

  public int getRollNum() {
    return _rollNum;
  }

  public Resource getPortType() {
    return _portType;
  }

  /**
   * Deserializes a TileSpec from a JSON object produced by the map editor.
   * Expected format:
   * {"x":2,"y":0,"z":0,"type":"WHEAT","rollNum":9}
   * {"x":0,"y":0,"z":3,"type":"SEA","portType":"WILDCARD"}
   */
  public static TileSpec fromJson(JsonObject obj) {
    int x = obj.get("x").getAsInt();
    int y = obj.get("y").getAsInt();
    int z = obj.get("z").getAsInt();
    TileType type = TileType.valueOf(obj.get("type").getAsString());
    int rollNum = 0;
    if (obj.has("rollNum")) {
      rollNum = obj.get("rollNum").getAsInt();
    }
    Resource portType = null;
    JsonElement portEl = obj.get("portType");
    if (portEl != null && !portEl.isJsonNull()) {
      portType = Resource.valueOf(portEl.getAsString());
    }
    return new TileSpec(x, y, z, type, rollNum, portType);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("x", _x);
    obj.addProperty("y", _y);
    obj.addProperty("z", _z);
    obj.addProperty("type", _type.name());
    obj.addProperty("rollNum", _rollNum);
    if (_portType != null) {
      obj.addProperty("portType", _portType.name());
    }
    return obj;
  }
}
