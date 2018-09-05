package org.wordpress.android.ui.screenshots.support;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.AmbiguousViewMatcherException;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.wordpress.android.R;
import org.wordpress.android.util.image.ImageType;

import java.util.Collection;
import java.util.function.Supplier;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;


public class WPScreenshotSupport {
    // HIGH-LEVEL METHODS

    public static boolean hasElement(Integer elementID) {
        return hasElement(onView(withId(elementID)));
    }

    public static boolean hasElement(ViewInteraction element) {
        try {
            element.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void scrollToThenClickOn(Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        onView(withId(elementID))
                .perform(scrollTo())
                .perform(click());
    }

    public static void clickOn(Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        onView(withId(elementID)).perform(click());
    }

    public static void clickOnCellAtIndexIn(int index, int elementID) {
        waitForAtLeastOneElementWithIdToExist(elementID);
        onView(withId(elementID))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
    }

    public static void populateTextField(Integer elementID, String text) {
        waitForElementToBeDisplayed(elementID);
        onView(withId(elementID))
                .perform(replaceText(text))
                .perform(closeSoftKeyboard());
    }

    public static void moveCaretToEndAndDisplayIn(int elementID) {
        onView(withId(elementID))
                .perform(new FlashCaretViewAction());

        // To sync between the test target and the app target
        waitOneFrame();
        waitOneFrame();
    }

    public static void selectItemAtIndexInSpinner(Integer index, Integer elementID) {
        waitForElementToBeDisplayed(elementID);
        clickOn(elementID);

        onView(
                allOf(
                        withId(R.id.text),
                        childAtPosition(withClassName(is("android.widget.DropDownListView")), index)
                )
        ).perform(click());
    }

    // WAITERS
    public static void waitForElementToBeDisplayed(final Integer elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasElement(elementID);
            }
        });
    }

    public static void waitForElementToNotBeDisplayed(final Integer elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return !hasElement(elementID);
            }
        });
    }

    public static void waitForConditionToBeTrue(Supplier<Boolean> supplier) {
        new SupplierIdler(supplier).idleUntilReady();
    }

    public static void waitForImagesOfTypeWithPlaceholder(final Integer elementID, final ImageType imageType) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasLoadedAllImagesOfTypeWithPlaceholder(elementID, imageType);
            }
        });

        // sometimes the result of `getDrawable()` isn't the placeholder, but the placeholder is still displayed
        waitOneFrame();
    }

    public static void waitForAtLeastOneElementOfTypeToExist(final Class c) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return atLeastOneElementOfTypeExists(c);
            }
        });
    }

    public static void waitForAtLeastOneElementWithIdToExist(final int elementID) {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return atLeastOneElementWithIdExists(elementID);
            }
        });
    }

    public static void waitForRecyclerViewToStopReloading() {
        waitForConditionToBeTrue(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return hasReloadingRecyclerView();
            }
        });
    }

    public static void pressBackUntilElementIsVisible(int elementID) {
        while (!hasElement(elementID)) {
            Espresso.pressBack();
        }
    }

    // Used by some methods that access the view layer directly. Because the screenshot generation code runs in
    // a different thread than the UI, the UI sometimes reports completion of an operation before repainting the
    // screen to reflect the change. Delaying by one frame ensures we're not taking a screenshot of a stale UI.
    public static void waitOneFrame() {
        try {
            Thread.sleep(17);
        } catch (Exception ex) {
            // do nothing
        }
    }

    // MATCHERS

    /**
     * Returns a matcher that ensures only a single match is returned. It is best combined with
     * other matchers to prevent an {@link AmbiguousViewMatcherException}.
     */
    public static FirstMatcher first() {
        return new FirstMatcher();
    }

    public static EmptyImageMatcher isEmptyImage() {
        return new EmptyImageMatcher();
    }

    public static PlaceholderImageMatcher isPlaceholderImage(ImageType imageType) {
        return new PlaceholderImageMatcher(imageType);
    }

    // HELPERS

    public static Boolean atLeastOneElementOfTypeExists(Class c) {
        try {
            onView(
                    allOf(
                            Matchers.<View>instanceOf(c),
                            first()
                    )
            ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Boolean atLeastOneElementWithIdExists(int elementID) {
        try {
            onView(
                    allOf(
                            withId(elementID),
                            first()
                    )
            ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Boolean hasLoadedAllImagesOfTypeWithPlaceholder(Integer elementID, ImageType imageType) {
        try {
            onView(
                    allOf(
                            withId(elementID),
                            isDisplayed(),
                            anyOf(isEmptyImage(), isPlaceholderImage(imageType)),
                            first()
                    )
            ).check(doesNotExist());

            return true;
        } catch (Throwable e) {
            return false; // There are still unloaded images
        }
    }

    public static boolean hasReloadingRecyclerView() {
        try {
            onView(
                    allOf(
                            new RefreshingRecyclerViewMatcher(),
                            first()
                    )
            ).check(doesNotExist());

            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private static Activity mCurrentActivity;
    public static Activity getCurrentActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(RESUMED);

                if (resumedActivities.iterator().hasNext()) {
                    mCurrentActivity = (Activity) resumedActivities.iterator().next();
                }
            }
        });

        return mCurrentActivity;
    }
}
