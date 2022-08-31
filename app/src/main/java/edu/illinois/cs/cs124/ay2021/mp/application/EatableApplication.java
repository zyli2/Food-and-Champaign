package edu.illinois.cs.cs124.ay2021.mp.application;

import android.app.Application;
import android.os.Build;
import edu.illinois.cs.cs124.ay2021.mp.network.Client;
import edu.illinois.cs.cs124.ay2021.mp.network.Server;

/*
 * One instance of the Application class is created when the app is launched and persists throughout its lifetime.
 * This is unlike activities, which are created and destroyed as the user navigates to different screens in the app.
 * As a result, the Application class is a good place to store constants and initialize things that are potentially
 * needed by multiple activities, such as our restaurant API client.
 *
 * You may not need to change the code in this file, but definitely not until MP3.
 */
public final class EatableApplication extends Application {
  // Default port for the restaurant API server
  // You may modify this if needed to work around a conflict with another server running on your
  // machine
  public static final int DEFAULT_SERVER_PORT = 8989;

  // Default server URL
  // You should not need to modify this
  public static final String SERVER_URL = "http://localhost:" + DEFAULT_SERVER_PORT + "/";

  // Restaurant API client
  private Client client;

  // Getter for the restaurant API client
  public Client getClient() {
    return client;
  }

  /*
   * onCreate is called when the instance of the Application class is created.
   * We use it to initialize any state that the Application class should store.
   * For this app we also use it as an opportunity to start both our restaurant API server and client.
   */
  @Override
  public void onCreate() {
    super.onCreate();
    if (Build.FINGERPRINT.equals("robolectric")) {
      Server.start();
    } else {
      new Thread(Server::start).start();
    }
    client = Client.start();
  }
}
