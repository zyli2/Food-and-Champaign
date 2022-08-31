package edu.illinois.cs.cs124.ay2021.mp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertWithMessage;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.illinois.cs.cs124.ay2021.mp.activities.MainActivity;
import edu.illinois.cs.cs124.ay2021.mp.application.EatableApplication;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;
import edu.illinois.cs.cs124.ay2021.mp.network.Client;
import edu.illinois.cs.cs124.ay2021.mp.network.Server;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
 * This is the MP0 test suite.
 * The code below is used to evaluate your app during testing, local grading, and official grading.
 * You will probably not understand all of the code below, but you'll need to have some understanding of how it works
 * so that you can determine what is wrong with your app and what you need to fix.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 * You may modify the code below if it is useful during your own local testing, but any changes you make will
 * be lost once you submit.
 * Please keep that in mind, particularly if you see differences between your local scores and your official scores.
 *
 * Our test suites are broken into two parts.
 * The unit tests (in the UnitTests class) are tests that we can perform without running your app.
 * They test things like whether a specific method works properly, or the behavior of your API server.
 * Unit tests are usually fairly fast.
 *
 * The integration tests (in the IntegrationTests class) are tests that require simulating your app.
 * This allows us to test things like your API client, and higher-level aspects of your app's behavior, such as whether
 * it displays the right thing on the display.
 * Because integration tests require simulating your app, they run slower.
 *
 * Our test suites will also include a mixture of graded and ungraded tests.
 * The graded tests are marking with the `@Graded` annotation which contains a point total.
 * Ungraded tests do not have this annotation.
 * Some ungraded tests will work immediately, and are there to help you pinpoint regressions: meaning changes that
 * you made that might have broken things that were working previously.
 * The ungraded tests below were actually written by me (Geoff) during MP development.
 * Other ungraded tests are simply there to help your development process.
 */
@RunWith(Enclosed.class)
public final class MP0Test {

  // Unit tests that don't require simulating the entire app
  public static class UnitTests {

    // Create an HTTP client to test the server with
    static OkHttpClient httpClient = new OkHttpClient();

    // Static blocks run when the static class is created, very much like a constructor
    static {
      // Start the API server
      Server.start();
    }

    // THIS TEST SHOULD WORK
    // Test whether the loadRestaurants method works properly
    @Test(timeout = 1000L)
    public void testLoadRestaurants() throws JsonProcessingException {
      // Parse the String returned by loadRestaurants as JSON
      JsonNode restaurantList = new ObjectMapper().readTree(Server.loadRestaurants());
      // Check that it's a JSON array
      assertWithMessage("Restaurants is not a JSON array")
          .that(restaurantList)
          .isInstanceOf(ArrayNode.class);
      // Check that the array has the right size
      assertWithMessage("Restaurant list is not the right size")
          .that(restaurantList)
          .hasSize(Server.RESTAURANT_COUNT);
    }

    // THIS TEST SHOULD WORK
    // Test whether the GET /restaurants server route works properly
    @Test(timeout = 1000L)
    public void testRestaurantsRoute() throws IOException {
      // Formulate a GET request to the API server for the /restaurants route
      Request courseRequest =
          new Request.Builder().url(EatableApplication.SERVER_URL + "restaurants/").build();
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
      JsonNode restaurantList = new ObjectMapper().readTree(body.string());
      assertWithMessage("Restaurant list is not the right size")
          .that(restaurantList)
          .hasSize(Server.RESTAURANT_COUNT);
    }
  }

  // Integration tests that require simulating the entire app
  @RunWith(AndroidJUnit4.class)
  @LooperMode(LooperMode.Mode.PAUSED)
  public static class IntegrationTests {
    // Establish a separate API client for testing
    private static final Client client = Client.start();

    // Static blocks run when the static class is created, very much like a constructor
    static {
      // Set up logging so that you can see log output during testing
      Helpers.configureLogging();
    }

    // After each test make sure the client connected successfully
    @After
    public void checkClient() {
      assertWithMessage("Client should be connected").that(client.getConnected());
    }

    // Graded test that the activity displays the correct title
    @Graded(points = 90)
    @Test(timeout = 10000L)
    public void testActivityTitle() {
      // Start the main activity
      ActivityScenario<MainActivity> scenario = Helpers.startActivity();
      // Once the activity starts, check that it has the correct title
      scenario.onActivity(
          activity ->
              assertWithMessage("MainActivity has wrong title")
                  .that(activity.getTitle())
                  .isEqualTo("Find Restaurants"));
    }

    // THIS TEST SHOULD WORK
    // Test that the main activity displays the right number of restaurants after launch
    @Test(timeout = 10000L)
    public void testActivityRestaurantCount() {
      Helpers.startActivity();
      onView(withId(R.id.recycler_view)).check(Helpers.countRecyclerView(Server.RESTAURANT_COUNT));
    }

    // THIS TEST SHOULD WORK
    // Test that the API client retrieves the list of restaurants correctly
    @Test(timeout = 1000L)
    public void testClientGetRestaurants() throws InterruptedException, ExecutionException {
      // A CompletableFuture allows us to wait for the result of an asynchronous call
      CompletableFuture<List<Restaurant>> completableFuture = new CompletableFuture<>();
      // When getRestaurants returns, it causes the CompletableFuture to complete
      client.getRestaurants(completableFuture::complete);
      // Wait for the CompletableFuture to complete
      List<Restaurant> restaurants = completableFuture.get();
      // The List<Restaurant> should not be null, which is returned by getRestaurants when something
      // went wrong
      assertWithMessage("Request failed").that(restaurants).isNotNull();
      // Check that the List<Restaurant> has the correct size
      assertWithMessage("Restaurant list is not the right size")
          .that(restaurants)
          .hasSize(Server.RESTAURANT_COUNT);
    }
  }
}
