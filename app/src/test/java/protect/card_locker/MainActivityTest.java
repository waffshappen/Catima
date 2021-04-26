package protect.card_locker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.BarcodeFormat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.w3c.dom.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class MainActivityTest
{
    private SharedPreferences prefs;

    @Test
    public void initiallyNoLoyaltyCards() {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        assertNotNull(activity);

        TextView helpText = activity.findViewById(R.id.helpText);
        assertEquals(View.VISIBLE, helpText.getVisibility());

        TextView noMatchingCardsText = activity.findViewById(R.id.noMatchingCardsText);
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());

        RecyclerView list = activity.findViewById(R.id.list);
        assertEquals(View.GONE, list.getVisibility());
    }

    @Test
    public void onCreateShouldInflateLayout() {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertNotNull(menu);

        // The settings, import/export, groups, search and add button should be present
        assertEquals(menu.size(), 6);
        assertEquals("Search", menu.findItem(R.id.action_search).getTitle().toString());
        assertEquals("Groups", menu.findItem(R.id.action_manage_groups).getTitle().toString());
        assertEquals("Import/Export", menu.findItem(R.id.action_import_export).getTitle().toString());
        assertEquals("Privacy Policy", menu.findItem(R.id.action_privacy_policy).getTitle().toString());
        assertEquals("About", menu.findItem(R.id.action_about).getTitle().toString());
        assertEquals("Settings", menu.findItem(R.id.action_settings).getTitle().toString());
    }

    @Test
    public void clickAddStartsScan()
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        activity.findViewById(R.id.fabAdd).performClick();

        ShadowActivity shadowActivity = shadowOf(activity);
        assertEquals(shadowActivity.peekNextStartedActivityForResult().intent.getComponent(), new ComponentName(activity, ScanActivity.class));
    }

    @Test
    public void addOneLoyaltyCard()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = mainActivity.findViewById(R.id.helpText);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        RecyclerView list = mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getAdapter().getItemCount());

        DBHelper db = TestHelpers.getEmptyDb(mainActivity);
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 0);

        assertEquals(View.VISIBLE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        db.close();
    }

    @Test
    public void addFourLoyaltyCardsTwoStarred()  // Main screen showing starred cards on top correctly
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();
        activityController.visible();

        TextView helpText = mainActivity.findViewById(R.id.helpText);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        RecyclerView list = mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getAdapter().getItemCount());

        DBHelper db = TestHelpers.getEmptyDb(mainActivity);
        db.insertLoyaltyCard("storeB", "note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 0);
        db.insertLoyaltyCard("storeA", "note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 0);
        db.insertLoyaltyCard("storeD", "note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 1);
        db.insertLoyaltyCard("storeC", "note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 1);

        assertEquals(View.VISIBLE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();
        activityController.visible();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(4, list.getAdapter().getItemCount());
        assertEquals("storeC", ((TextView) list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.store)).getText());
        assertEquals("storeD", ((TextView) list.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.store)).getText());
        assertEquals("storeA", ((TextView) list.findViewHolderForAdapterPosition(2).itemView.findViewById(R.id.store)).getText());
        assertEquals("storeB", ((TextView) list.findViewHolderForAdapterPosition(3).itemView.findViewById(R.id.store)).getText());

        db.close();
    }

    @Test
    public void testGroups()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        DBHelper db = TestHelpers.getEmptyDb(mainActivity);

        TabLayout groupTabs = mainActivity.findViewById(R.id.groups);

        // No group tabs by default
        assertEquals(0, groupTabs.getTabCount());

        // Having at least one group should create two tabs: One all and one for each group
        db.insertGroup("One");
        activityController.pause();
        activityController.resume();
        assertEquals(2, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());

        // Adding another group should have it added to the end
        db.insertGroup("Alphabetical two");
        activityController.pause();
        activityController.resume();
        assertEquals(3, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());
        assertEquals("Alphabetical two", groupTabs.getTabAt(2).getText().toString());

        // Removing a group should also change the list
        db.deleteGroup("Alphabetical two");
        activityController.pause();
        activityController.resume();
        assertEquals(2, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());

        // Removing the last group should make the tabs disappear
        db.deleteGroup("One");
        activityController.pause();
        activityController.resume();
        assertEquals(0, groupTabs.getTabCount());

        db.close();
    }

    @Test
    public void testFiltering()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        MainActivity mainActivity = (MainActivity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = mainActivity.findViewById(R.id.helpText);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        RecyclerView list = mainActivity.findViewById(R.id.list);
        TabLayout groupTabs = mainActivity.findViewById(R.id.groups);

        DBHelper db = TestHelpers.getEmptyDb(mainActivity);
        db.insertLoyaltyCard("The First Store", "Initial note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 0);
        db.insertLoyaltyCard("The Second Store", "Secondary note", null, new BigDecimal("0"), null, "cardId", null, BarcodeFormat.UPC_A, Color.BLACK, 0);

        db.insertGroup("Group one");
        List<Group> groups = new ArrayList<>();
        groups.add(db.getGroup("Group one"));
        db.setLoyaltyCardGroups(1, groups);

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        mainActivity.mFilter = "store";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        mainActivity.mFilter = "first";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        mainActivity.mFilter = "initial";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        mainActivity.mFilter = "second";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        mainActivity.mFilter = "company";

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        mainActivity.mFilter = "";

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        db.close();
    }
}