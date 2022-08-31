package edu.illinois.cs.cs124.ay2021.mp.network;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.illinois.cs.cs124.ay2021.mp.application.EatableApplication;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/*
 * Restaurant API server.
 *
 * Normally this code would run on a separate machine from your app, which would make requests to it over the internet.
 * However, for our MP we have this run inside the app alongside the rest of your code, to allow you to gain experience
 * with full-stack app development.
 * You are both developing the client (the Android app) and the server that it requests data from.
 * This is a very common programming paradigm and one used by most or all of the smartphone apps that you use regularly.
 *
 * You will need to some of the code here and make changes starting with MP1.
 */
public final class Server extends Dispatcher {
  // You may find this useful for debugging
  @SuppressWarnings("unused")
  private static final String TAG = Server.class.getSimpleName();

  // Stores the JSON string containing information about all of the restaurants created during
  // server startup
  private final String restaurantsJson;
  private final String preferencesJson;

  // Helper method for the GET /restaurants route, called by the dispatch method below
  private MockResponse getRestaurants() {
    return new MockResponse()
        // Indicate that the request succeeded (HTTP 200 OK)
        .setResponseCode(HttpURLConnection.HTTP_OK)
        // Load the JSON string with restaurant information into the body of the response
        .setBody(restaurantsJson)
        /*
         * Set the HTTP header that indicates that this is JSON with the utf-8 charset.
         * There are some special characters in our data set, so it's important to mark it as utf-8 so it is parsed
         * properly by clients.
         */
        .setHeader("Content-Type", "application/json; charset=utf-8");
  }

  // Helper method for the GET /preferences route, called by the dispatch method below
  private MockResponse getPreferences() {
    return new MockResponse()
        // Indicate that the request succeeded (HTTP 200 OK)
        .setResponseCode(HttpURLConnection.HTTP_OK)
        // Load the JSON string with restaurant information into the body of the response
        .setBody(preferencesJson)
        /*
         * Set the HTTP header that indicates that this is JSON with the utf-8 charset.
         * There are some special characters in our data set, so it's important to mark it as utf-8 so it is parsed
         * properly by clients.
         */
        .setHeader("Content-Type", "application/json; charset=utf-8");
  }

  /*
   * Server request dispatcher.
   * Responsible for parsing the HTTP request and determining how to respond.
   * You will need to understand this code and augment it starting with MP2.
   */
  @NonNull
  @Override
  public MockResponse dispatch(@NonNull final RecordedRequest request) {
    try {
      // Reject malformed requests
      if (request.getPath() == null || request.getMethod() == null) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }
      /*
       * We perform a few normalization steps before we begin route dispatch, since this makes the if-else statement
       * below simpler.
       */
      // Normalize the path by removing trailing slashes and replacing multiple repeated slashes
      // with single slashes
      String path = request.getPath().replaceFirst("/*$", "").replaceAll("/+", "/");
      // Normalize the request method by converting to uppercase
      String method = request.getMethod().toUpperCase();

      // Main route dispatch tree, dispatching routes based on request path and type
      if (path.equals("") && method.equals("GET")) {
        // This route is used by the client during startup, so don't remove
        return new MockResponse().setBody("CS 124").setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.equals("/restaurants") && method.equals("GET")) {
        // Return the JSON list of restaurants for a GET request to the path /restaurants
        return getRestaurants();
      } else if (path.equals("/preferences") && method.equals("GET")) {
        return getPreferences();
      }

      // If the route didn't match above, then we return a 404 NOT FOUND
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
          // If we don't set a body here Volley will choke with a strange error
          // Normally a 404 for a web API would not need a body
          .setBody("Not Found");
    } catch (Exception e) {
      // Return a HTTP 500 if an exception is thrown
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
  }

  /*
   * Load restaurant information from a CSV file and create a JSON array.
   * You will need to modify this code for MP1, and replicate it with some small changes using new data that we'll
   * provide starting with MP2.
   */
  public static String loadRestaurants() {
    String input =
        new Scanner(Server.class.getResourceAsStream("/restaurants.csv"), "UTF-8")
            .useDelimiter("\\A")
            .next();
    CSVReader csvReader = new CSVReaderBuilder(new StringReader(input)).withSkipLines(1).build();
    ArrayNode restaurants = JsonNodeFactory.instance.arrayNode();
    for (String[] parts : csvReader) {
      ObjectNode restaurant = JsonNodeFactory.instance.objectNode();
      restaurant.put("id", parts[0]);
      restaurant.put("name", parts[1]);
      restaurant.put("cuisine", parts[2]);
      restaurant.put("url", parts[3]);
      restaurants.add(restaurant);
    }
    return restaurants.toPrettyString();
  }
  // MP2 Part 1: Convert preferences CSV to JSON
  public static String loadPreferences() {
    String input =
        new Scanner(Server.class.getResourceAsStream("/preferences.csv"), "UTF-8")
            .useDelimiter("\\A")
            .next();
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    for (String line : input.split("\n")) {
      String[] parts = line.trim().split(",");
      ObjectNode node = JsonNodeFactory.instance.objectNode();
      ArrayNode inputArray = JsonNodeFactory.instance.arrayNode();
      node.put("id", parts[0]);
      for (int i = 1; i < parts.length; i++) {
        inputArray.add(parts[i]);
      }
      node.put("restaurantIDs", inputArray);
      array.add(node);
    }
    return array.toPrettyString();
  }

  // Number of restaurants that we expect to find in the CSV file
  // Normally this wouldn't be hardcoded but it's useful for testing
  public static final int RESTAURANT_COUNT = 255;

  /*
   * You do not need to modify the code below.
   * However, you may want to understand how it works.
   * It implements the singleton pattern and initializes the server when Server.start() is called.
   * We also check to make sure that no other servers are running on the same machine, which can cause problems.
   */
  public static void start() {
    if (!isRunning(false)) {
      new Server();
    }
    if (!isRunning(true)) {
      throw new IllegalStateException("Server should be running");
    }
  }

  private static final int RETRY_COUNT = 8;
  private static final long RETRY_DELAY = 512;

  public static boolean isRunning(final boolean wait) {
    return isRunning(wait, RETRY_COUNT, RETRY_DELAY);
  }

  /*
   * You do not need to modify the code below.
   * However, you may want to understand how it works.
   * It determines whether a server is running by making a GET request for the / path and checking the response body.
   */
  public static boolean isRunning(final boolean wait, final int retryCount, final long retryDelay) {
    for (int i = 0; i < retryCount; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(EatableApplication.SERVER_URL).get().build();
      try {
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
          if (Objects.requireNonNull(response.body()).string().equals("CS 124")) {
            return true;
          } else {
            throw new IllegalStateException(
                "Another server is running on port " + EatableApplication.DEFAULT_SERVER_PORT);
          }
        }
      } catch (IOException ignored) {
        if (!wait) {
          break;
        }
        try {
          Thread.sleep(retryDelay);
        } catch (InterruptedException ignored1) {
        }
      }
    }
    return false;
  }

  private Server() {
    restaurantsJson = loadRestaurants();
    preferencesJson = loadPreferences();

    Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.OFF);
    try {
      MockWebServer server = new MockWebServer();
      server.setDispatcher(this);
      server.start(EatableApplication.DEFAULT_SERVER_PORT);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException(e.getMessage());
    }
  }
}
