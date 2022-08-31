package edu.illinois.cs.cs124.ay2021.mp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static edu.illinois.cs.cs124.ay2021.mp.ContainsMatcher.containsText;
import static edu.illinois.cs.cs124.ay2021.mp.Helpers.countRecyclerView;
import static edu.illinois.cs.cs124.ay2021.mp.Helpers.startActivity;
import static edu.illinois.cs.cs124.ay2021.mp.RecyclerViewMatcher.withRecyclerView;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.illinois.cs.cs124.ay2021.mp.activities.MainActivity;
import edu.illinois.cs.cs124.ay2021.mp.activities.RestaurantActivity;
import edu.illinois.cs.cs124.ay2021.mp.application.EatableApplication;
import edu.illinois.cs.cs124.ay2021.mp.models.Preference;
import edu.illinois.cs.cs124.ay2021.mp.models.RelatedRestaurants;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;
import edu.illinois.cs.cs124.ay2021.mp.network.Client;
import edu.illinois.cs.cs124.ay2021.mp.network.Server;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/*
 * This is the MP2 test suite.
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
 * Version 1, updated 11/2/2021.
 */

@RunWith(Enclosed.class)
public final class MP2Test {
  // Number of expected restaurants
  private static final int RESTAURANT_COUNT = 255;
  // Number of expected preferences
  private static final int PREFERENCE_COUNT = 45;

  // Random number generator, seeded to provide a reproducible random number stream
  private static final Random random = new Random(124);
  // List of restaurants used for testing
  private static final List<Restaurant> restaurants;
  // Map between restaurant ID and restaurant, used during testing
  private static final Map<String, Restaurant> restaurantMap;

  static {
    // Before testing begins, load the restaurant list using Server.loadRestaurants so that we have
    // a loaded list for testing purposes
    ObjectMapper objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      // Make sure the restaurant list is the right size
      restaurants = objectMapper.readValue(Server.loadRestaurants(), new TypeReference<>() {});
      if (restaurants.size() != RESTAURANT_COUNT) {
        throw new IllegalStateException("Wrong restaurant count");
      }
      restaurantMap =
          restaurants.stream().collect(Collectors.toMap(Restaurant::getId, Function.identity()));
    } catch (JsonProcessingException e) {
      // static blocks can't throw checked exceptions, so we convert to an unchecked exception
      throw new IllegalStateException(e);
    }
  }

  // Load expected preferences from the CSV file, to avoid assuming that loadPreferences works
  private static final Map<String, String[]> expectedPreferences =
      // This somewhat odd syntax is how you initialize a HashMap in Java
      new HashMap<>() {
        {
          String input =
              new Scanner(Server.class.getResourceAsStream("/preferences.csv"), "UTF-8")
                  .useDelimiter("\\A")
                  .next();
          for (String line : input.split("\n")) {
            this.put(
                line.split(",")[0], Arrays.stream(line.split(",")).skip(1).toArray(String[]::new));
          }
        }
      };

  // Unit tests that don't require simulating the entire app
  public static class UnitTests {
    // Create an HTTP client to test the server with
    static OkHttpClient httpClient = new OkHttpClient();

    static {
      // Start the API server
      Server.start();
    }

    // Test whether Server.loadPreferences() works properly
    @Graded(points = 20)
    @Test(timeout = 1000L)
    public void testLoadPreferences() throws JsonProcessingException {
      // Load and deserialize preferences
      JsonNode preferences = new ObjectMapper().readTree(Server.loadPreferences());
      // Preferences should be a JSON array
      assertWithMessage("Preferences is not a JSON array")
          .that(preferences instanceof ArrayNode)
          .isTrue();
      // Preferences should have the expected size
      assertWithMessage("Preferences is not the right size")
          .that(preferences)
          .hasSize(PREFERENCE_COUNT);

      // Mark which preference values we've seen
      Set<String> seenPreferences = new HashSet<>();
      // Check each deserialized preference value
      for (JsonNode preference : preferences) {
        // Preference nodes should have an id key
        assertWithMessage("Preference object does not have key id")
            .that(preference.has("id"))
            .isTrue();
        // The id key should be a String
        assertWithMessage("Preference id is not a String").that(preference.get("id").isTextual());
        // The id key should contain a valid UUID
        String id = preference.get("id").asText();
        try {
          //noinspection ResultOfMethodCallIgnored
          UUID.fromString(id);
        } catch (Exception e) {
          assertWithMessage("Preference id is not a valid UUID: " + e).fail();
        }
        // The id should be in a set that we loaded from the same file
        assertThat(expectedPreferences.containsKey(id));
        // Mark that we saw this id so that we can make sure we saw all expected ids later
        seenPreferences.add(id);

        // Preference nodes should have a restaurantIDs key
        assertWithMessage("Preference object does not have key restaurantIDs")
            .that(preference.has("restaurantIDs"))
            .isTrue();
        // The restaurantIDs key should contain a JSON array
        JsonNode restaurants = preference.get("restaurantIDs");
        assertWithMessage("restaurantIDs key of preference object is not a JSON array")
            .that(restaurants)
            .isInstanceOf(ArrayNode.class);
        // Convert the JSON array to a List<String>
        List<String> foundRestaurantIDs = new ArrayList<>();
        for (JsonNode restaurant : restaurants) {
          foundRestaurantIDs.add(restaurant.textValue());
        }
        // Compare our String[] against the expected String[]
        String[] expectedRestaurantIDs = expectedPreferences.get(id);
        assertWithMessage("Didn't find expected array of restaurant IDs")
            .that(expectedRestaurantIDs)
            .isNotNull();
        assertWithMessage("Wrong number of restaurants")
            .that(foundRestaurantIDs.size())
            .isEqualTo(expectedRestaurantIDs.length);
        // Check membership of the two arrays
        for (String expect : expectedRestaurantIDs) {
          assertWithMessage(
                  "Expected restaurant ID " + expect + " was not found for preference " + id)
              .that(foundRestaurantIDs.contains(expect))
              .isTrue();
        }
      }
      // Confirm that we saw all expected preference values
      assertWithMessage("Didn't see all expected preferences")
          .that(seenPreferences.equals(expectedPreferences.keySet()))
          .isTrue();
    }

    // Test whether the GET /preferences server route works properly
    @Graded(points = 10)
    @Test(timeout = 1000L)
    public void testPreferencesRoute() throws IOException {
      // Formulate a GET request to the API server for the /preferences route
      Request courseRequest =
          new Request.Builder().url(EatableApplication.SERVER_URL + "preferences/").build();
      // Execute the request
      Response courseResponse = httpClient.newCall(courseRequest).execute();
      // The request should have succeeded
      assertWithMessage("Request should have succeeded")
          .that(courseResponse.code())
          .isEqualTo(HttpStatus.SC_OK);
      // The response body should not be null
      ResponseBody body = courseResponse.body();
      assertWithMessage("Response body should not be null").that(body).isNotNull();
      // The response body should be a JSON array with the expected size
      JsonNode preferenceList = new ObjectMapper().readTree(body.string());
      assertWithMessage("Preference list is not the right size")
          .that(preferenceList)
          .hasSize(PREFERENCE_COUNT);
    }
  }

  // Integration tests that require simulating the entire app
  @SuppressWarnings("CommentedOutCode")
  @RunWith(AndroidJUnit4.class)
  @LooperMode(LooperMode.Mode.PAUSED)
  public static class IntegrationTests {
    // Client used during testing
    private static final Client client = Client.start();

    static {
      // Set up logging so that you can see log output during testing
      Helpers.configureLogging();
    }

    // After each test ensure that the client was able to connect to the API server
    @After
    public void checkClient() {
      assertWithMessage("Client should be connected").that(client.getConnected());
    }

    // Retrieve a list of restaurant preferences from the backend API server
    private List<Preference> getPreferences() throws ExecutionException, InterruptedException {
      // Create and execute the request
      CompletableFuture<List<Preference>> completableFuture = new CompletableFuture<>();
      client.getPreferences(completableFuture::complete);
      List<Preference> preferences = completableFuture.get();
      // The request should have succeeded, and returned the right number of preferences
      assertWithMessage("Request failed").that(preferences).isNotNull();
      assertWithMessage("Preferences list is not the right size")
          .that(preferences)
          .hasSize(PREFERENCE_COUNT);
      // Return the preferences since this is used as a helper method by the relationship tests
      return preferences;
    }

    // Test that the backend server returns the preferences properly
    // Don't need to do much more here than call the helper method
    @Graded(points = 10)
    @Test(timeout = 1000L)
    public void testClientGetPreferences() throws InterruptedException, ExecutionException {
      getPreferences();
    }

    // Test that the new restaurant view activity works correctly
    @Test(timeout = 10000L)
    @Graded(points = 10)
    public void testRestaurantView() {
      // Start the MainActivity so that it can perform any necessary initialization
      startActivity();
      // Get a list of restaurant IDs to test
      List<String> restaurantIDs =
          restaurants.stream().map(Restaurant::getId).collect(Collectors.toList());
      for (int i = 0; i < 16; i++) {
        // Retrieve the ID and other information about this restaurant
        String restaurantID = restaurantIDs.get(random.nextInt(restaurantIDs.size()));
        Restaurant restaurant = restaurantMap.get(restaurantID);
        assertThat(restaurant).isNotNull();
        // Create and configure the Intent that should launch the Restaurant view activity
        Intent intent =
            new Intent(ApplicationProvider.getApplicationContext(), RestaurantActivity.class);
        intent.putExtra("id", restaurantID);
        // Launch the intent
        ActivityScenario<RestaurantActivity> restaurantScenario = ActivityScenario.launch(intent);
        restaurantScenario.moveToState(Lifecycle.State.CREATED);
        restaurantScenario.moveToState(Lifecycle.State.RESUMED);
        // Check the view for the text that we expect: the restaurant name and cuisine values
        onView(isRoot()).check(matches(hasDescendant(containsText(restaurant.getName()))));
        onView(isRoot()).check(matches(hasDescendant(containsText(restaurant.getCuisine()))));
      }
    }

    // Test that clicking on a restaurant in the list view launches the restaurant view properly
    @Test(timeout = 10000L)
    @Graded(points = 10)
    public void testOnClickLaunch() {
      // Restaurants in the main view should still be sorted, so we use a sorted list so that
      // we know what to expect
      List<Restaurant> sortedRestaurants = new ArrayList<>(restaurants);
      sortedRestaurants.sort(Restaurant.SORT_BY_NAME);

      // Start the main activity
      ActivityScenario<MainActivity> scenario = startActivity();
      scenario.onActivity(
          activity -> {
            // Check that the right number of restaurants is displayed in the list view
            onView(withId(R.id.recycler_view)).check(countRecyclerView(RESTAURANT_COUNT));
            for (int i = 0; i < 8; i++) {
              // Pick a random restaurant to click on
              int position = random.nextInt(restaurants.size());
              Restaurant restaurant = sortedRestaurants.get(position);
              // Scroll to the right place
              onView(withId(R.id.recycler_view)).perform(scrollToPosition(position));
              // Check that the right restaurant is shown at that position, and perform the click
              onView(withRecyclerView(R.id.recycler_view).atPosition(position))
                  .check(
                      (view, unused) -> {
                        assertThat(matches(hasDescendant(containsText(restaurant.getName()))));
                        view.performClick();
                      });
              // Check to make sure that the right Intent is launched
              Intent started = shadowOf(activity).getNextStartedActivity();
              assertWithMessage("Didn't start activity").that(started).isNotNull();
              // The Intent should contain the restaurant ID
              String id = started.getStringExtra("id");
              assertWithMessage("Didn't launch activity properly")
                  .that(id)
                  .isEqualTo(restaurant.getId());
            }
          });
    }

    // Avoid regenerating relationships repeatedly during testing
    private final Map<String, Map<String, Integer>> cachedRelationships = new HashMap<>();

    // Helper method for relationship testing
    private void testRelationship(
        String first, String second, Integer expected, RelatedRestaurants related) {
      // Compute relationships for the first ID if we haven't already
      Map<String, Integer> firstRelated =
          cachedRelationships.getOrDefault(first, related.getRelated(first));
      assertThat(firstRelated).isNotNull();
      cachedRelationships.put(first, firstRelated);
      // Compute relationships for the second ID if we haven't already
      Map<String, Integer> secondRelated =
          cachedRelationships.getOrDefault(second, related.getRelated(second));
      assertThat(secondRelated).isNotNull();
      cachedRelationships.put(second, secondRelated);
      // Test computed relationship against pre-computed values
      assertThat(firstRelated.get(second)).isEqualTo(expected);
      // Relationships should be symmetric
      assertThat(secondRelated.get(first)).isEqualTo(expected);
    }

    @Graded(points = 20)
    @Test(timeout = 1000L)
    public void testRelatedRestaurants() throws ExecutionException, InterruptedException {
      List<Preference> preferences = getPreferences();
      RelatedRestaurants related = new RelatedRestaurants(restaurants, preferences);

      // These precomputed results were generated by the code below
      // Normally you'd write a few of these by hand, but for the purposes of grading it's helpful
      // to have a few more than you'd normally use, to discourage memoization
      testRelationship(
          "ce04b72e-dd92-479a-ab7c-0177ae484f6f",
          "7a3c27ff-463b-4982-8f2a-ec5ef498f477",
          null,
          related);
      testRelationship(
          "839dbef4-61a1-49bb-8082-bb5eba6c7f0d",
          "238786f3-1766-405f-a297-5f8f5224ba13",
          null,
          related);
      testRelationship(
          "028ab5ab-2d99-4185-92ab-47391fab4aab",
          "ecdcb4fc-df03-4753-adde-0e6088fd67f9",
          null,
          related);
      testRelationship(
          "5e41341c-aa14-4347-83c9-9202addac97d",
          "a4b90429-cfcc-4831-a8db-a22388238225",
          null,
          related);
      testRelationship(
          "d276e998-b654-4b7e-8921-732576b99820",
          "f508b5de-41da-4938-b0e4-372052a53c5c",
          null,
          related);
      testRelationship(
          "7f923c86-5a4b-4cac-87e0-6cc88cf27cea",
          "150673ce-307f-4697-afc1-f94bf2eb1dc3",
          null,
          related);
      testRelationship(
          "f3ebf7cf-4f94-4efa-a0af-654d50ba093b",
          "5eb9ffb4-d37a-4160-ad0a-acd60d5658bf",
          null,
          related);
      testRelationship(
          "0cb36db9-13aa-4cd9-ae0f-3ed97d3ed801",
          "ac718ac2-4ccf-45a8-9a2a-7f97e9642c4a",
          null,
          related);
      testRelationship(
          "5e556dbc-51c4-4d7e-8400-0cd44af9bd86",
          "274bdb8c-d746-40f7-ae00-781d28fecbea",
          null,
          related);
      testRelationship(
          "7c66b81e-ef1d-4c1a-8847-b92f8293466b",
          "a8d4d47a-0ed1-4ef8-9e31-fd52991c1eee",
          1,
          related);
      testRelationship(
          "aa0055c4-a695-4fc3-9d24-c11a8ab6684b",
          "7c66b81e-ef1d-4c1a-8847-b92f8293466b",
          1,
          related);
      testRelationship(
          "2c78d28b-4aa0-4518-8134-81a5ea2a14b4",
          "77825645-c1a5-4c73-a76c-e56e840bc490",
          2,
          related);
      testRelationship(
          "839dbef4-61a1-49bb-8082-bb5eba6c7f0d",
          "307fb45b-81e5-4a07-b033-399b589e1153",
          1,
          related);
      testRelationship(
          "edb0634a-7116-4780-9a25-a853631b69f8",
          "178e4690-cab1-4b1d-bd1a-161b72bdaaf8",
          1,
          related);
      testRelationship(
          "7154e35e-3f54-4bc3-a1a1-9335b63f18a9",
          "f6c71062-7773-4a87-98a2-a9550a9addc5",
          1,
          related);
      testRelationship(
          "214178a5-7dcd-4dea-8049-5b18f24b52b8",
          "cbd00363-0ae0-477f-8c8e-3be787dff703",
          1,
          related);
      testRelationship(
          "edb0634a-7116-4780-9a25-a853631b69f8",
          "d7896727-5aa4-4e87-8076-f36112e8e0da",
          1,
          related);
      testRelationship(
          "8d810579-997c-497c-a80e-d6e37b48f081",
          "67083e98-2811-440f-bf06-075542ff24c1",
          1,
          related);
      testRelationship(
          "307fb45b-81e5-4a07-b033-399b589e1153",
          "d7896727-5aa4-4e87-8076-f36112e8e0da",
          1,
          related);
      testRelationship(
          "edb0634a-7116-4780-9a25-a853631b69f8",
          "032900f9-6285-42e4-a664-bba64282417f",
          1,
          related);
      testRelationship(
          "ee955584-68ee-4c70-bb75-4a24317222aa",
          "c102c13f-a593-407e-90a3-cc198db7ddfb",
          1,
          related);
      testRelationship(
          "093bb670-6a58-4b08-a40b-8f666b96978b",
          "f3743902-06c1-4c09-a3f0-d1240c093919",
          1,
          related);
      testRelationship(
          "07b903ce-154e-492f-9d3d-c1aaa7c2f460",
          "2aa4cdb6-6e9d-4c1e-9848-e5a5f7ccf26f",
          2,
          related);
      testRelationship(
          "67083e98-2811-440f-bf06-075542ff24c1",
          "f6c71062-7773-4a87-98a2-a9550a9addc5",
          1,
          related);
      testRelationship(
          "c413dcf8-bcc5-4e8c-85ef-6fea73ba4099",
          "cef64bfd-7440-41cc-8863-bf06443726f2",
          1,
          related);
      testRelationship(
          "093bb670-6a58-4b08-a40b-8f666b96978b",
          "5e41341c-aa14-4347-83c9-9202addac97d",
          1,
          related);
      testRelationship(
          "e3d0291b-1463-4336-9cd2-d2102d98f6d9",
          "9247635d-53f9-4231-a4ad-d386bee69f0f",
          1,
          related);
      testRelationship(
          "c058069c-7c33-4eed-8c95-0e7366db0928",
          "07b903ce-154e-492f-9d3d-c1aaa7c2f460",
          1,
          related);
      testRelationship(
          "2aa4cdb6-6e9d-4c1e-9848-e5a5f7ccf26f",
          "839dbef4-61a1-49bb-8082-bb5eba6c7f0d",
          1,
          related);
      testRelationship(
          "689d2091-4329-4455-ac70-67afb849b6d9",
          "02fb03a3-d8e9-4ec1-8c76-bac21219e6ba",
          1,
          related);
      testRelationship(
          "9d697062-0e78-41fd-8670-20f0eca805d3",
          "18114bcc-b21b-44e2-819b-56a84b64c2ed",
          1,
          related);
      testRelationship(
          "e81f1297-373f-4df2-b754-0ca4e363c589",
          "0f783417-d41e-496e-9291-d27cb30207d9",
          1,
          related);
      testRelationship(
          "839dbef4-61a1-49bb-8082-bb5eba6c7f0d",
          "2aa4cdb6-6e9d-4c1e-9848-e5a5f7ccf26f",
          1,
          related);
      testRelationship(
          "f6c71062-7773-4a87-98a2-a9550a9addc5",
          "9247635d-53f9-4231-a4ad-d386bee69f0f",
          1,
          related);
      testRelationship(
          "08b72d2a-e528-4675-81e2-e0a13e8c0f0e",
          "9247635d-53f9-4231-a4ad-d386bee69f0f",
          2,
          related);
      testRelationship(
          "5e41341c-aa14-4347-83c9-9202addac97d",
          "7a3c27ff-463b-4982-8f2a-ec5ef498f477",
          1,
          related);
      testRelationship(
          "d54bbb0c-609a-483c-adf9-f10f1f07411a",
          "08b72d2a-e528-4675-81e2-e0a13e8c0f0e",
          2,
          related);
      testRelationship(
          "d11b0a93-c87e-405b-93ec-9fdd0bdb3b49",
          "08b72d2a-e528-4675-81e2-e0a13e8c0f0e",
          1,
          related);
      testRelationship(
          "a8d4d47a-0ed1-4ef8-9e31-fd52991c1eee",
          "9247635d-53f9-4231-a4ad-d386bee69f0f",
          2,
          related);
      testRelationship(
          "a1e8c7cd-d21a-47bb-991c-dd8098c75134",
          "7a3c27ff-463b-4982-8f2a-ec5ef498f477",
          1,
          related);
      testRelationship(
          "02fb03a3-d8e9-4ec1-8c76-bac21219e6ba",
          "77825645-c1a5-4c73-a76c-e56e840bc490",
          3,
          related);
      testRelationship(
          "e4b97946-55e2-4003-a077-a5387f723b3a",
          "178e4690-cab1-4b1d-bd1a-161b72bdaaf8",
          2,
          related);
      testRelationship(
          "e4b97946-55e2-4003-a077-a5387f723b3a",
          "2aa4cdb6-6e9d-4c1e-9848-e5a5f7ccf26f",
          1,
          related);
      testRelationship(
          "8d810579-997c-497c-a80e-d6e37b48f081",
          "8181bd2f-3f33-4200-b9af-224a8b8dd537",
          2,
          related);
      testRelationship(
          "c058069c-7c33-4eed-8c95-0e7366db0928",
          "33fb1f35-bb05-4f68-ae69-2125b083a1b9",
          1,
          related);
      testRelationship(
          "25c84fa4-bac7-495b-a467-bed7a5455ef1",
          "67083e98-2811-440f-bf06-075542ff24c1",
          1,
          related);
      testRelationship(
          "77825645-c1a5-4c73-a76c-e56e840bc490",
          "8307e49e-946c-4ee0-b04b-e80e37296fcc",
          3,
          related);
      testRelationship(
          "77825645-c1a5-4c73-a76c-e56e840bc490",
          "7a3c27ff-463b-4982-8f2a-ec5ef498f477",
          1,
          related);
      testRelationship(
          "178e4690-cab1-4b1d-bd1a-161b72bdaaf8",
          "7154e35e-3f54-4bc3-a1a1-9335b63f18a9",
          1,
          related);
      testRelationship(
          "8d810579-997c-497c-a80e-d6e37b48f081",
          "354ea76e-4f32-4f00-9e4c-ac3a5659af27",
          1,
          related);
      testRelationship(
          "f3ebf7cf-4f94-4efa-a0af-654d50ba093b",
          "7c66b81e-ef1d-4c1a-8847-b92f8293466b",
          3,
          related);
      testRelationship(
          "5e41341c-aa14-4347-83c9-9202addac97d",
          "0fe46aff-43ca-4ba7-a375-0c7068419ee5",
          2,
          related);
      testRelationship(
          "faff5e95-3c10-4d6c-8717-dcfe088dc2f6",
          "7154e35e-3f54-4bc3-a1a1-9335b63f18a9",
          1,
          related);
      testRelationship(
          "cbb5c524-8dc2-45ce-9d17-32504375889d",
          "2aa4cdb6-6e9d-4c1e-9848-e5a5f7ccf26f",
          1,
          related);
      testRelationship(
          "032900f9-6285-42e4-a664-bba64282417f",
          "178e4690-cab1-4b1d-bd1a-161b72bdaaf8",
          1,
          related);
      testRelationship(
          "08b72d2a-e528-4675-81e2-e0a13e8c0f0e",
          "5c437a4b-8f33-4dc3-8670-63952c149754",
          1,
          related);
      testRelationship(
          "8181bd2f-3f33-4200-b9af-224a8b8dd537",
          "307fb45b-81e5-4a07-b033-399b589e1153",
          1,
          related);
      testRelationship(
          "4736da51-895e-43ca-a17d-42c892708bee",
          "f4d76ba2-5c6a-4516-b488-1234a2755660",
          1,
          related);
      testRelationship(
          "2c78d28b-4aa0-4518-8134-81a5ea2a14b4",
          "ee955584-68ee-4c70-bb75-4a24317222aa",
          1,
          related);
      testRelationship(
          "0fe46aff-43ca-4ba7-a375-0c7068419ee5",
          "c413dcf8-bcc5-4e8c-85ef-6fea73ba4099",
          1,
          related);
      testRelationship(
          "e4b97946-55e2-4003-a077-a5387f723b3a",
          "f4d76ba2-5c6a-4516-b488-1234a2755660",
          1,
          related);
      testRelationship(
          "362748f3-ece7-4b70-8e5d-2025f5acae41",
          "cef64bfd-7440-41cc-8863-bf06443726f2",
          3,
          related);
      testRelationship(
          "e3d0291b-1463-4336-9cd2-d2102d98f6d9",
          "e4b97946-55e2-4003-a077-a5387f723b3a",
          1,
          related);
      testRelationship(
          "fe6c9179-b653-4185-b04c-e2445c091fe4",
          "77825645-c1a5-4c73-a76c-e56e840bc490",
          1,
          related);

      // Test bad restaurant IDs
      testRelationship(
          "c301d45a-90e9-4b4e-8cd0-1b04939e7bf5",
          "77825645-c1a5-4c73-a76c-e56e840bc490",
          null,
          related);
      testRelationship(
          "c7421950-05d8-426d-8ef7-5787b6bb4e02",
          "d6f5c8a6-31bf-4f79-8091-379e7e2afcb9",
          null,
          related);

      // How most of the test cases above were generated, using the solution set
      /*
      int nullCount = 0;
      int nonNullCount = 0;
      while (nonNullCount < 64) {
        Restaurant first = restaurants.get(random.nextInt(restaurants.size()));
        Restaurant second = restaurants.get(random.nextInt(restaurants.size()));
        if (first.getId().equals(second.getId())) {
          continue;
        }
        Map<String, Integer> relationships = related.getRelated(first.getId());
        Integer expected = relationships.get(second.getId());
        if (expected == null && nullCount++ > 8) {
          continue;
        }
        System.out.println("testRelationship(\"" + first.getId() + "\", \"" + second.getId() + "\", " + expected + ", related);");
        nonNullCount++;
      }
       */
    }
  }
}
