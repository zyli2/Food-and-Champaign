package edu.illinois.cs.cs124.ay2021.mp.models;

import androidx.annotation.NonNull;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/*
 * Model storing information about a restaurant retrieved from the restaurant server.
 *
 * You will need to understand some of the code in this file and make changes starting with MP1.
 *
 * If your project builds successfully, you can safely ignore the warning about "Related problems" here.
 * It seems to be a bug in Android studio.
 */
@SuppressWarnings("unused")
public final class Restaurant implements SortedListAdapter.ViewModel {
  // Name of the restaurant
  private String name;

  // Getter for the name
  public String getName() {
    return name;
  }

  // What cuisines this restaurant serves
  private String cuisine;

  // Getter for the cuisine
  public String getCuisine() {
    return cuisine;
  }

  // Getter for returning restaurant names and cuisine
  public String getNameAndCuisine() {
    return name + " " + cuisine;
  }

  // id
  private String id;

  // Getter for id
  public String getId() {
    return id;
  }

  // You will need to add more fields here...

  public static List<Restaurant> search(final List<Restaurant> restaurants, final String search) {
    // copy that will be returned
    List<Restaurant> restaurantMatches = new ArrayList<>();

    // a copy of restaurant cuisines for comparison
    Set<String> restaurantCuisine = new HashSet<>();

    // checking if the value entered value is valid
    if (restaurants == null || search == null) {
      throw new IllegalArgumentException("bad search");
    }

    // data cleaning search
    String searchValue = search.trim().toLowerCase(Locale.ROOT);

    // returning a copy of a list of restaurants if search it's empty
    if (search.length() == 0 || searchValue.length() == 0) {
      List<Restaurant> restaurantCopy = new ArrayList<>(restaurants);
      return restaurantCopy;
    }

    // restaurant cuisine data cleaning + creating a Set copy of it
    for (int i = 0; i < restaurants.size(); i++) {
      restaurantCuisine.add(restaurants.get(i).cuisine.toLowerCase(Locale.ROOT));
    }
    // returning a list of restaurants that has the exact cuisine value of search
    // else, return a list of restaurants that contains either parts of the cuisine value or name
    // value
    if (restaurantCuisine.contains(searchValue)) {
      for (int i = 0; i < restaurants.size(); i++) {
        if (searchValue.equals(restaurants.get(i).cuisine)) {
          restaurantMatches.add(restaurants.get(i));
        }
      }
    } else {
      for (int i = 0; i < restaurants.size(); i++) {
        if (restaurants.get(i).name.toLowerCase(Locale.ROOT).contains(searchValue)
            || restaurants.get(i).cuisine.toLowerCase(Locale.ROOT).contains(searchValue)) {
          restaurantMatches.add(restaurants.get(i));
        }
      }
    }

    return restaurantMatches;
  }

  /*
   * The Jackson JSON serialization library we are using requires an empty constructor.
   * So don't remove this!
   */
  public Restaurant() {}

  /*
   * Function to compare Restaurant instances by name.
   * Currently this does not work, but you will need to implement it correctly for MP1.
   * Comparator is like Comparable, except it defines one possible ordering, not a canonical ordering for a class,
   * and so is implemented as a separate method rather than directly by the class as is done with Comparable.
   */
  public static final Comparator<Restaurant> SORT_BY_NAME =
      (Comparator.comparing(restaurant -> restaurant.name));

  // You should not need to modify this code, which is used by the list adapter component
  @Override
  public <T> boolean isSameModelAs(@NonNull final T model) {
    return equals(model);
  }

  // You should not need to modify this code, which is used by the list adapter component
  @Override
  public <T> boolean isContentTheSameAs(@NonNull final T model) {
    return equals(model);
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof Restaurant) {
      Restaurant other = (Restaurant) o;
      return this.id.equals(other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
