package edu.illinois.cs.cs124.ay2021.mp.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// MP2 Part 4: Identify relationships between restaurants
public class RelatedRestaurants {
  // What should the String in the map and the String and Integer in the inner map be?
  // first String is the restaurant ID
  // inner Integer is the relationship
  // inner String is all the restaurants related to the outer restaurant
  private Map<String, Map<String, Integer>> restaurantRelationships = new HashMap<>();
  // List of restaurants from restaurantIDs to restaurant
  private final Map<String, Restaurant> restaurantMap = new HashMap<>();
  private final Map<String, Restaurant> restaurantMapN = new HashMap<>();

  public RelatedRestaurants(
      final List<Restaurant> restaurants, final List<Preference> preferences) {
    for (Restaurant restaurant : restaurants) {
      restaurantRelationships.put(restaurant.getId(), new HashMap<String, Integer>());
      restaurantMap.put(restaurant.getId(), restaurant);
      restaurantMapN.put(restaurant.getName(), restaurant);
    }
    for (Preference preference : preferences) {
      for (String restaurantID : preference.getRestaurantIDs()) {
        if (restaurantID != null && restaurantRelationships.get(restaurantID) != null) {
          for (String compareID : preference.getRestaurantIDs()) {
            if (!restaurantID.equals(compareID) && restaurantRelationships.get(compareID) != null) {
              Map<String, Integer> relationship = restaurantRelationships.get(restaurantID);
              relationship.put(compareID, relationship.getOrDefault(compareID, 0) + 1);
            }
          }
        }
      }
    }
  }

  public Map<String, Integer> getRelated(final String restaurantID) {
    Map<String, Integer> returnMap = restaurantRelationships.get(restaurantID);
    if (returnMap != null) {
      return returnMap;
    } else {
      return new HashMap<>();
    }
  }

  public List<Restaurant> getRelatedInOrder(final String restaurantID) {

    // Check the restaurantID, define what is invalid?
    if (!restaurantMap.containsKey(restaurantID)) {
      throw new IllegalArgumentException();
    }

    // Retrieve the related restaurants
    // Somehow convert the list of restaurantIDs to a list of restaurants
    List<Restaurant> relatedRest = new ArrayList<>();
    List<String> relatedStr = new ArrayList<>();
    Map<String, Integer> related = getRelated(restaurantID);
    Map<String, Integer> withName = new HashMap<>();

    for (String id : related.keySet()) {
      String name = restaurantMap.get(id).getName();
      withName.put(restaurantMap.get(id).getName(), related.get(id));
      relatedStr.add(name);
    }

    Collections.sort(
        relatedStr,
        (r1, r2) -> {
          if (withName.get(r2) - withName.get(r1) == 0) {
            return r1.compareTo(r2);
          } else {
            return withName.get(r2) - withName.get(r1);
          }
        });

    for (String id : relatedStr) {
      relatedRest.add(restaurantMapN.get(id));
      for (Restaurant res : relatedRest) {
        System.out.println(res.getName() + " , " + related.get(id) + " , " + id);
      }
    }

    return relatedRest;
  }

  public Set<Restaurant> getConnectedTo(final String restaurantID) {
    // Check the restaurantID
    if (!restaurantMap.containsKey(restaurantID)) {
      throw new IllegalArgumentException();
    }
    Set<Restaurant> toReturn = new HashSet<>();
    // the graph is the map from calling getRelated(RestaurantID)
    Map<String, Integer> graph = getRelated(restaurantID);
    Set<String> valid = new HashSet<>();

    for (String neighbor : graph.keySet()) {
      Set<String> visited = new HashSet<>();
      traverse(graph, neighbor, visited, valid, 1);
    }

    valid.remove(restaurantID);

    for (String id : valid) {
      toReturn.add(restaurantMap.get(id));
    }

    return toReturn;
  }

  private void traverse(
      final Map<String, Integer> graph,
      final String node,
      final Set<String> visited,
      final Set<String> valid,
      final int distance) {
    Map<String, Integer> neighbors = getRelated(node);
    if (graph.size() == 1) {
      return;
    }

    if (visited.contains(node)) {
      return;
    }
    visited.add(node);
    if (graph.get(node) < 2) {
      return;
    }
    valid.add(node);
    System.out.println(graph.get(node));
    if (distance == 0) {
      return;
    }

    for (String neighbor : neighbors.keySet()) {
      traverse(neighbors, neighbor, visited, valid, 0);
    }
  }
}
