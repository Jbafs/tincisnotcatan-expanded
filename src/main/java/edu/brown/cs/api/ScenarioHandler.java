package edu.brown.cs.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import spark.Request;
import spark.Response;
import spark.Route;

public class ScenarioHandler implements Route {

  private static final JsonArray SCENARIOS = buildScenarios();

  private static JsonArray buildScenarios() {
    JsonArray arr = new JsonArray();
    arr.add(scenario("heading_for_new_shores", "Heading for New Shores", "3-4"));
    arr.add(scenario("four_islands", "The Four Islands", "3-4"));
    return arr;
  }

  private static JsonObject scenario(String id, String name, String players) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", id);
    obj.addProperty("name", name);
    obj.addProperty("players", players);
    obj.addProperty("type", "seafarers");
    return obj;
  }

  @Override
  public Object handle(Request req, Response res) {
    res.type("application/json");
    return SCENARIOS.toString();
  }
}
