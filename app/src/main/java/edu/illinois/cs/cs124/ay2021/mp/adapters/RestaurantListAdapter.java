package edu.illinois.cs.cs124.ay2021.mp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import edu.illinois.cs.cs124.ay2021.mp.adapters.viewholder.RestaurantViewHolder;
import edu.illinois.cs.cs124.ay2021.mp.databinding.ItemRestaurantBinding;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;

/*
 * Helper class for our restaurant list.
 * You should not need to modify this code.
 */
public final class RestaurantListAdapter extends SortedListAdapter<Restaurant> {

  public interface Listener {
    void onClicked(Restaurant restaurant);
  }

  private final Listener listener;

  public RestaurantListAdapter(final Context context, final Listener setListener) {
    super(context, Restaurant.class, Restaurant.SORT_BY_NAME);
    listener = setListener;
  }

  @NonNull
  @Override
  protected RestaurantViewHolder onCreateViewHolder(
      @NonNull final LayoutInflater inflater, @NonNull final ViewGroup parent, final int viewType) {
    final ItemRestaurantBinding binding = ItemRestaurantBinding.inflate(inflater, parent, false);
    return new RestaurantViewHolder(binding, listener);
  }
}
