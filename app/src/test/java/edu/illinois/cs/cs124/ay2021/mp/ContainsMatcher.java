package edu.illinois.cs.cs124.ay2021.mp;

import android.view.View;
import android.widget.TextView;
import androidx.test.espresso.matcher.BoundedMatcher;
import org.hamcrest.Description;

/*
 * This file contains helper code used by the test suites.
 * You should not need to modify it.
 */

public final class ContainsMatcher extends BoundedMatcher<View, TextView> {
  private final String text;
  private final boolean ignoreCase;

  public ContainsMatcher(String setText, boolean setIgnoreCase) {
    super(TextView.class);
    text = setText;
    ignoreCase = setIgnoreCase;
  }

  @Override
  protected boolean matchesSafely(TextView item) {
    try {
      if (!ignoreCase) {
        return item.getText().toString().contains(text);
      } else {
        return item.getText().toString().toLowerCase().contains(text.toLowerCase());
      }
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("(looking for text " + text + ")");
  }

  public static ContainsMatcher containsText(String text) {
    return new ContainsMatcher(text, true);
  }

  @SuppressWarnings("unused")
  public static ContainsMatcher containsTextWithCase(String text) {
    return new ContainsMatcher(text, false);
  }
}
