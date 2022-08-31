package edu.illinois.cs.cs124.ay2021.mp.models;

import java.util.ArrayList;
import java.util.List;

// MP2 Part 2: Create a preference model
public class Preference {
  private String id;

  private List<String> restaurantIDs = new ArrayList<>();

  public List<String> getRestaurantIDs() {
    return restaurantIDs;
  }

  public String getId() {

    return id;
  }

  public Preference() {}
}
