package edu.illinois.cs.cs124.ay2021.mp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static edu.illinois.cs.cs124.ay2021.mp.ContainsMatcher.containsText;
import static edu.illinois.cs.cs124.ay2021.mp.Helpers.countRecyclerView;
import static edu.illinois.cs.cs124.ay2021.mp.Helpers.pause;
import static edu.illinois.cs.cs124.ay2021.mp.Helpers.searchFor;
import static edu.illinois.cs.cs124.ay2021.mp.RecyclerViewMatcher.withRecyclerView;
import static org.junit.Assert.assertThrows;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.illinois.cs.cs124.ay2021.mp.application.EatableApplication;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;
import edu.illinois.cs.cs124.ay2021.mp.network.Server;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/*
 * This is the MP1 test suite.
 * The code below is used to evaluate your app during testing, local grading, and official grading.
 * You will probably not understand all of the code below, but you'll need to have some understanding of how it works
 * so that you can determine what is wrong with your app and what you need to fix.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 * You may modify the code below if it is useful during your own local testing, but any changes you make will
 * be lost once you submit.
 * Please keep that in mind, particularly if you see differences between your local scores and your official scores.
 *
 * For more notes on testing, please see the MP0 test suites (MP0Test.java).
 *
 * Version 2, updated 10/24/2021.
 */
@RunWith(Enclosed.class)
public final class MP1Test {
  // Need to move this here so that it can't be modified by submitted code
  private static final int RESTAURANT_COUNT = 255;
  // Random number generator, seeded to provide a reproducible random number stream
  private static final Random random = new Random(124);
  // List of restaurants used for testing
  private static final List<Restaurant> restaurants;

  static {
    // Before testing begins, load the restaurant list using Server.loadRestaurants so that we have
    // a loaded list for testing purposes
    ObjectMapper objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      restaurants = objectMapper.readValue(Server.loadRestaurants(), new TypeReference<>() {});
      if (restaurants.size() != RESTAURANT_COUNT) {
        throw new IllegalStateException("Wrong restaurant count");
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  // Unit tests that don't require simulating the entire app
  @SuppressWarnings("SpellCheckingInspection")
  public static class UnitTests {

    // Create an HTTP client to test the server with
    private static final OkHttpClient httpClient = new OkHttpClient();

    static {
      // Start the API server
      Server.start();
    }

    // Test whether the restaurant comparator works properly
    @Graded(points = 20)
    @Test(timeout = 1000L)
    public void testRestaurantCompareByName() {
      // The list returned from Server.loadRestaurants should not be sorted, to prevent solving the
      // problem by modifying restaurants.csv
      assertWithMessage("Initial list should not be sorted")
          .that(isSortedByName(restaurants))
          .isFalse();

      // Repeat the test 32 times
      for (int i = 0; i < 32; i++) {
        // Copy the restaurant list
        List<Restaurant> sortedRestaurants = new ArrayList<>(restaurants);
        // Remove a random subset of restaurants
        for (int j = 0; j < random.nextInt(7) + 1; j++) {
          sortedRestaurants.remove(random.nextInt(sortedRestaurants.size()));
        }
        // Check to make sure that the pruned list is not sorted
        assertWithMessage("Pruned list should not be sorted")
            .that(isSortedByName(sortedRestaurants))
            .isFalse();
        // Sort the list using the restaurant SORT_BY_NAME comparator
        sortedRestaurants.sort(Restaurant.SORT_BY_NAME);
        // Check to make sure that the sorted list is sorted
        assertWithMessage("List should be sorted").that(isSortedByName(sortedRestaurants)).isTrue();
      }
    }

    // UNGRADED TEST
    // THIS TEST SHOULD NOT WORK INITIALLY
    // To enable it, remove or comment out the @Ignore annotation below
    // This test checks to make sure that you are parsing the right fields from the node JSON
    // @Ignore("Enable once you begin working on search")
    @Test(timeout = 1000L)
    public void testLoadRestaurantFields() throws IOException {
      // Build a GET request for /restaurants
      Request courseRequest =
          new Request.Builder().url(EatableApplication.SERVER_URL + "restaurants/").build();
      // Execute the request
      Response courseResponse = httpClient.newCall(courseRequest).execute();
      // The request should have succeeded
      assertWithMessage("Request should succeed")
          .that(courseResponse.code())
          .isEqualTo(HttpStatus.SC_OK);
      // The response body should not be null
      ResponseBody body = courseResponse.body();
      assertWithMessage("Body should not be null").that(body).isNotNull();
      // The response body should be a JSON array
      JsonNode restaurantList = new ObjectMapper().readTree(body.string());
      assertWithMessage("Request should return a JSON array")
          .that(restaurantList instanceof ArrayNode)
          .isTrue();
      // The JSON array should contain the correct number of restaurants
      assertWithMessage("Wrong restaurant count").that(restaurantList).hasSize(RESTAURANT_COUNT);
      // Check the JSON nodes for the correct fields
      for (JsonNode restaurantNode : restaurantList) {
        assertWithMessage("JSON is missing field id").that(restaurantNode.has("id")).isTrue();
        assertWithMessage("JSON is missing field name").that(restaurantNode.has("name")).isTrue();
        assertWithMessage("JSON is missing field cuisine")
            .that(restaurantNode.has("cuisine"))
            .isTrue();
        assertWithMessage("JSON is missing field url").that(restaurantNode.has("url")).isTrue();
      }
    }

    // Test the restaurant search method
    @SuppressWarnings("ConstantConditions")
    @Graded(points = 20)
    @Test(timeout = 1000L)
    public void testRestaurantSearch() {
      // Test corner cases
      // Empty string should return all restaurants
      assertThat(Restaurant.search(restaurants, "")).hasSize(RESTAURANT_COUNT);
      assertThat(Restaurant.search(restaurants, "  ")).hasSize(RESTAURANT_COUNT);
      // Empty string should return a copy of the list, not the original list
      assertThat(Restaurant.search(restaurants, "")).isNotSameInstanceAs(restaurants);
      // Assert that nulls throw the right exception
      assertThrows(IllegalArgumentException.class, () -> Restaurant.search(restaurants, null));
      assertThrows(IllegalArgumentException.class, () -> Restaurant.search(null, "pizza"));

      // We use a mix of inputs that either match or don't match a cuisine value
      assertThat(Restaurant.search(restaurants, "pizz")).hasSize(17);
      // The search method should not modify the passed list
      assertWithMessage("search modified the passed list")
          .that(restaurants)
          .hasSize(RESTAURANT_COUNT);
      // Test other searches
      assertThat(Restaurant.search(restaurants, "pizza")).hasSize(14);
      assertThat(Restaurant.search(restaurants, "ba")).hasSize(49);
      assertThat(Restaurant.search(restaurants, "bar")).hasSize(2);
      assertThat(Restaurant.search(restaurants, "BAR")).hasSize(2);
      assertThat(Restaurant.search(restaurants, "bar ")).hasSize(2);
      System.out.println("here");
      assertThat(Restaurant.search(restaurants, "t a")).hasSize(4);
      assertThat(Restaurant.search(restaurants, "n a")).hasSize(2);
      assertThat(Restaurant.search(restaurants, "store")).hasSize(11);
      assertThat(Restaurant.search(restaurants, "SteakHouse")).hasSize(4);
      assertThat(Restaurant.search(restaurants, "GrIll")).hasSize(24);
      assertThat(Restaurant.search(restaurants, " TharA")).hasSize(1);
      assertThat(Restaurant.search(restaurants, "juicery ")).hasSize(1);
    }
  }

  // Integration tests that require simulating the entire app
  @SuppressWarnings("SpellCheckingInspection")
  @RunWith(AndroidJUnit4.class)
  @LooperMode(LooperMode.Mode.PAUSED)
  public static class IntegrationTests {
    static {
      // Set up logging so that you can see log output during testing
      Helpers.configureLogging();
    }

    @Before
    public void startActivity() {
      // Start the MainActivity before each test
      Helpers.startActivity();
    }

    // Test that the MainActivity displays the list of restaurants properly
    // The list should be sorted, and the cuisine values should be displayed
    @Graded(points = 20)
    @Test(timeout = 10000L)
    public void testRestaurantListView() {
      // Check that we still have the full list of restaurants
      assertThat(restaurants).hasSize(RESTAURANT_COUNT);
      // Generate a sorted lists of restaurants to use
      List<Restaurant> sortedRestaurants = new ArrayList<>(restaurants);
      sortedRestaurants.sort(Restaurant.SORT_BY_NAME);
      // Check to make sure that the sorted list is actually sorted, in case SORT_BY_NAME is still
      // broken
      assertWithMessage("List should be sorted").that(isSortedByName(sortedRestaurants)).isTrue();

      // Pause to let the UI catch up
      pause();
      // Once the list is shown by the MainActivity, check to make sure it has the right number of
      // restaurants
      onView(withId(R.id.recycler_view)).check(countRecyclerView(RESTAURANT_COUNT));
      // Check the first displayed restaurant
      onView(withRecyclerView(R.id.recycler_view).atPosition(0))
          .check(matches(hasDescendant(containsText("A Taste of Both Worlds"))));

      // Count non-empty cuisine values
      int cuisineCount = 0;
      // Check a random subset of restaurants
      for (int i = 0; i < 8; i++) {
        // Pick a random restaurant from the list
        int position = random.nextInt(sortedRestaurants.size());
        // Track non-empty cuisine values
        Restaurant restaurant = sortedRestaurants.get(position);
        if (!restaurant.getCuisine().equals("")) {
          cuisineCount++;
        }
        // Scroll to the right place
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.scrollToPosition(position));
        // Make sure that the item at that position has the correct name and that the cuisine is
        // also shown
        onView(withRecyclerView(R.id.recycler_view).atPosition(position))
            .check(matches(hasDescendant(containsText(restaurant.getName()))));
        onView(withRecyclerView(R.id.recycler_view).atPosition(position))
            .check(matches(hasDescendant(containsText(restaurant.getCuisine()))));
      }
      // Make sure we see the right number of non-empty cuisine values
      assertWithMessage("Didn't find enough non-empty cuisine value")
          .that(cuisineCount)
          .isEqualTo(7);
    }

    // Test that the search bar in the MainActivity works
    // This requires both that the restaurant search work correctly and a set of changes to the
    // MainActivity
    @Graded(points = 20)
    @Test(timeout = 10000L)
    public void testRestaurantSearchFunction() {
      // Check that the right number of restaurants is shown initially
      onView(withId(R.id.recycler_view)).check(countRecyclerView(RESTAURANT_COUNT));

      // Perform a search that returns no results
      onView(withId(R.id.search)).perform(searchFor("ethiopian"));
      // Pauses are required here to let the UI catch up
      pause();
      // There should be no results shown
      onView(withId(R.id.recycler_view)).check(countRecyclerView(0));
      // Make sure that clearing the search causes the full list to be displayed again
      onView(withId(R.id.search)).perform(searchFor(""));
      pause();
      onView(withId(R.id.recycler_view)).check(countRecyclerView(RESTAURANT_COUNT));

      // Check a few more searches
      onView(withId(R.id.search)).perform(searchFor("PIZZ"));
      pause();
      onView(withId(R.id.recycler_view)).check(countRecyclerView(17));

      onView(withId(R.id.search)).perform(searchFor("Ba"));
      pause();
      onView(withId(R.id.recycler_view)).check(countRecyclerView(49));

      onView(withId(R.id.search)).perform(searchFor("Thara Thai"));
      pause();
      onView(withId(R.id.recycler_view)).check(countRecyclerView(1));
    }
  }

  // Helper method for checking the comparator
  private static boolean isSortedByName(List<Restaurant> restaurants) {
    for (int i = 0; i < restaurants.size() - 1; i++) {
      if (restaurants.get(i).getName().compareTo(restaurants.get(i + 1).getName()) > 0) {
        return false;
      }
    }
    return true;
  }
}
