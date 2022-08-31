package edu.illinois.cs.cs124.ay2021.mp.adapters.viewholder;

import androidx.annotation.NonNull;
import com.github.wrdlbrnft.sortedlistadapter.SortedListAdapter;
import edu.illinois.cs.cs124.ay2021.mp.adapters.RestaurantListAdapter;
import edu.illinois.cs.cs124.ay2021.mp.databinding.ItemRestaurantBinding;
import edu.illinois.cs.cs124.ay2021.mp.models.Restaurant;

/*
 * Helper class for our restaurant list.
 * You should not need to modify this code.
 */
public class RestaurantViewHolder extends SortedListAdapter.ViewHolder<Restaurant> {
  private final ItemRestaurantBinding binding;

  public RestaurantViewHolder(
      final ItemRestaurantBinding setBinding, final RestaurantListAdapter.Listener setListener) {
    super(setBinding.getRoot());
    binding = setBinding;
    binding.setListener(setListener);
  }

  @Override
  protected void performBind(@NonNull final Restaurant restaurant) {
    binding.setRestaurant(restaurant);
  }
}
