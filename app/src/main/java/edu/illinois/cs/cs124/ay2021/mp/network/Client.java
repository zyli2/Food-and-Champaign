package edu.illinois.cs.cs124.ay2021.mp.network;

import android.os.Build;
import android.util.Log;
import com.android.volley.Cache;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.cs.cs124.ay2021.mp.application.EatableApplication;
import edu.illinois.cs.cs124.ay2021.mp.models.Preference;
import edu.illinois.cs.cs124.ay2021.mp.models.RelatedRestaurants;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
 * Client object used by the app to interact with the restaurant API server.
 *
 * This class uses what is called a singleton pattern, as described more below.
 * We create a static method that will only create a client the first time it is called, and mark the constructor as
 * private to prevent others from creating additional instances of the client.
 *
 * You will need to understand some of the code here and make changes starting with MP2.
 */
public final class Client {
  // You may find this useful when debugging
  private static final String TAG = Client.class.getSimpleName();

  // We are using the Jackson JSON serialization library to deserialize data from the server
  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private Map<String, Restaurant> restaurantMap;

  private RelatedRestaurants relatedRest;

  public Restaurant getRestaurantForID(final String id) {
    return restaurantMap.get(id);
  }

  public void setRelatedRest(final List<Restaurant> r, final List<Preference> p) {
    relatedRest = new RelatedRestaurants(r, p);
  }

  public String mostRelated(final String restaurantID) {
    List<Restaurant> related = relatedRest.getRelatedInOrder(restaurantID);
    if (related.size() == 0) {
      return "";
    }
    String mostRelatedRest = related.get(0).getName();
    return mostRelatedRest;
  }

  public String connectedSize(final String restaurantID) {
    Set<Restaurant> connectedRestaurant = relatedRest.getConnectedTo(restaurantID);
    int size = connectedRestaurant.size();
    return String.valueOf(size);
  }

  /*
   * Retrieve and deserialize a list of restaurants from the backend server.
   * Takes as an argument a callback method to call when the request completes which will be passed the deserialized
   * list of restaurants received from the server.
   * We will discuss callbacks in more detail once you need to augment this code in MP2.
   */
  public void getRestaurants(final Consumer<List<Restaurant>> callback) {
    /*
     * Construct the request itself.
     * We use a StringRequest allowing us to receive a String from the server.
     * The String will be valid JSON containing a list of restaurant objects which we can deserialize into instances of
     * our Restaurant model.
     */
    StringRequest restaurantsRequest =
        new StringRequest(
            Request.Method.GET,
            EatableApplication.SERVER_URL + "restaurants/",
            response -> {
              // This code runs on success
              try {
                /*
                 * Deserialize the String into a List<Restaurant> using Jackson.
                 * The new TypeReference<>() {} is the bit of magic required to have Jackson return a List with the
                 * correct type.
                 */
                List<Restaurant> restaurants =
                    objectMapper.readValue(response, new TypeReference<>() {});
                for (Restaurant restaurantInfo : restaurants) {
                  restaurantMap.put(restaurantInfo.getId(), restaurantInfo);
                }

                // Call the callback method and pass it the list of restaurants
                callback.accept(restaurants);
              } catch (Exception e) {
                Log.e(TAG, e.toString());
                // There are better approaches than returning null here, but we need to do something
                // to make sure that the callback returns even on error
                callback.accept(null);
              }
            },
            error -> {
              // This code runs on failure
              Log.e(TAG, error.toString());
              // There are better approaches than returning null here, but we need to do something
              // to make sure that the callback returns even on error
              callback.accept(null);
            });
    // Actually queue and run the request
    requestQueue.add(restaurantsRequest);
  }

  // MP2 Part 2: Client-server communication
  public void getPreferences(final Consumer<List<Preference>> callback) {
    /*
     * Construct the request itself.
     * We use a StringRequest allowing us to receive a String from the server.
     * The String will be valid JSON containing a list of restaurant objects which we can deserialize into instances of
     * our Restaurant model.
     */
    StringRequest preferencesRequest =
        new StringRequest(
            Request.Method.GET,
            EatableApplication.SERVER_URL + "preferences/",
            response -> {
              // This code runs on success
              try {
                /*
                 * Deserialize the String into a List<Preference> using Jackson.
                 * The new TypeReference<>() {} is the bit of magic required to have Jackson return a List with the
                 * correct type.
                 */
                List<Preference> preferences =
                    objectMapper.readValue(response, new TypeReference<>() {});
                // Call the callback method and pass it the list of restaurants
                callback.accept(preferences);
              } catch (Exception e) {
                Log.e(TAG, e.toString());
                // There are better approaches than returning null here, but we need to do something
                // to make sure that the callback returns even on error
                callback.accept(null);
              }
            },
            error -> {
              // This code runs on failure
              Log.e(TAG, error.toString());
              // There are better approaches than returning null here, but we need to do something
              // to make sure that the callback returns even on error
              callback.accept(null);
            });
    // Actually queue and run the request
    requestQueue.add(preferencesRequest);
  }

  /*
   * You do not need to modify the code below.
   * However, you may want to understand how it works.
   * It implements the singleton pattern and initializes the client when Client.start() is called.
   * The client tests to make sure it can connect to the backend server on startup.
   * We also initialize the client somewhat differently depending on whether we are testing your code or actually
   * running the app.
   */
  private static final int INITIAL_CONNECTION_RETRY_DELAY = 1000;
  private static Client instance;
  private boolean connected = false;

  public boolean getConnected() {
    return connected;
  }

  public static Client start() {
    if (instance == null) {
      instance = new Client(Build.FINGERPRINT.equals("robolectric"));
    }
    return instance;
  }

  private static final int MAX_STARTUP_RETRIES = 8;
  private static final int THREAD_POOL_SIZE = 4;

  private final RequestQueue requestQueue;

  private Client(final boolean testing) {
    // Quiet Volley's otherwise verbose logging
    VolleyLog.DEBUG = false;

    // remember to initialize variables in the constructor, declaring a variable in a class
    // is only declaring an attribute to that class
    restaurantMap = new HashMap<String, Restaurant>();

    Cache cache = new NoCache();
    Network network = new BasicNetwork(new HurlStack());
    HttpURLConnection.setFollowRedirects(true);

    if (testing) {
      requestQueue =
          new RequestQueue(
              cache,
              network,
              THREAD_POOL_SIZE,
              new ExecutorDelivery(Executors.newSingleThreadExecutor()));
    } else {
      requestQueue = new RequestQueue(cache, network);
    }

    URL serverURL;
    try {
      serverURL = new URL(EatableApplication.SERVER_URL);
    } catch (MalformedURLException e) {
      Log.e(TAG, "Bad server URL: " + EatableApplication.SERVER_URL);
      return;
    }

    new Thread(
            () -> {
              for (int i = 0; i < MAX_STARTUP_RETRIES; i++) {
                try {
                  HttpURLConnection connection = (HttpURLConnection) serverURL.openConnection();
                  String body =
                      new BufferedReader(new InputStreamReader(connection.getInputStream()))
                          .lines()
                          .collect(Collectors.joining("\n"));
                  if (!body.equals("CS 124")) {
                    throw new IllegalStateException("Invalid response from server");
                  }
                  connection.disconnect();
                  connected = true;
                  requestQueue.start();
                  break;
                } catch (Exception e) {
                  Log.e(TAG, e.toString());
                }
                try {
                  Thread.sleep(INITIAL_CONNECTION_RETRY_DELAY);
                } catch (InterruptedException ignored) {
                }
              }
            })
        .start();
  }
}
