package edu.illinois.cs.cs124.ay2021.mp.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import edu.illinois.cs.cs124.ay2021.mp.R;
import edu.illinois.cs.cs124.ay2021.mp.adapters.RestaurantListAdapter;
import edu.illinois.cs.cs124.ay2021.mp.application.EatableApplication;
import edu.illinois.cs.cs124.ay2021.mp.databinding.ActivityRestaurantBinding;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;

// MP2 Part 3: New activity to display restaurant details
public class RestaurantActivity extends AppCompatActivity {

  // Binding to the layout defined in activity_restaurant.xml
  private ActivityRestaurantBinding binding;

  private EatableApplication application;

  private RestaurantListAdapter listAdapter;

  @Override
  protected void onCreate(@Nullable final Bundle unused) {
    super.onCreate(unused);
    // retrieves id from intent
    Intent startedIntent = getIntent();
    startedIntent.getStringExtra("id");

    application = (EatableApplication) getApplication();
    Restaurant restaurant =
        application
            .getClient()
            /*
             * What is passed to getRestaurants is a callback, which we'll discuss in more detail in the MP lessons.
             * Callbacks allow us to wait for something to complete and run code when it does.
             * In this case, once we retrieve a list of restaurants, we use it to update the contents of our list.
             */
            // storing restaurants in restaurantList allows a the UI to quickly adapt to the user
            // given
            // input
            .getRestaurantForID(startedIntent.getStringExtra("id"));

    binding = DataBindingUtil.setContentView(this, R.layout.activity_restaurant);
    binding.name.setText(
        restaurant.getName()
            + " "
            + restaurant.getCuisine()
            + application.getClient().mostRelated(startedIntent.getStringExtra("id"))
            + application.getClient().connectedSize(startedIntent.getStringExtra("id")));
  }
}
