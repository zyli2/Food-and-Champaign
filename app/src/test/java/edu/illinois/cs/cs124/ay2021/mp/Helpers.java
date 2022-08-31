package edu.illinois.cs.cs124.ay2021.mp;

import static android.os.Looper.getMainLooper;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.robolectric.Shadows.shadowOf;

import android.view.View;
import android.widget.SearchView;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import edu.illinois.cs.cs124.ay2021.mp.activities.MainActivity;
import edu.illinois.cs.cs124.ay2021.mp.network.Server;
import org.hamcrest.Matcher;
import org.robolectric.shadows.ShadowLog;

/*
 * This file contains helper code used by the test suites.
 * You should not need to modify it.
 */

public class Helpers {
  public static ActivityScenario<MainActivity> startActivity() {
    ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
    scenario.moveToState(Lifecycle.State.CREATED);
    scenario.moveToState(Lifecycle.State.RESUMED);

    assertThat(Server.isRunning(true)).isTrue();
    return scenario;
  }

  public static ViewAssertion countRecyclerView(int expected) {
    return (v, noViewFoundException) -> {
      if (noViewFoundException != null) {
        throw noViewFoundException;
      }
      RecyclerView view = (RecyclerView) v;
      RecyclerView.Adapter<?> adapter = view.getAdapter();
      assert adapter != null;
      assertThat(adapter.getItemCount()).isEqualTo(expected);
    };
  }

  public static ViewAction searchFor(String query) {
    return searchFor(query, false);
  }

  public static ViewAction searchFor(String query, boolean submit) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return allOf(isDisplayed());
      }

      @Override
      public String getDescription() {
        if (submit) {
          return "Set query to " + query + " and submit";
        } else {
          return "Set query to " + query + " but don't submit";
        }
      }

      @Override
      public void perform(UiController uiController, View view) {
        SearchView v = (SearchView) view;
        v.setQuery(query, submit);
      }
    };
  }

  public static void pause(int length) {
    try {
      shadowOf(getMainLooper()).runToEndOfTasks();
      Thread.sleep(length);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void pause() {
    pause(100);
  }

  public static void configureLogging() {
    if (System.getenv("OFFICIAL_GRADING") == null) {
      ShadowLog.stream = System.out;
    }
  }
}
