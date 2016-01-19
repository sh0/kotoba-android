/*
 * Kotoba-chan
 *
 * Copyright (C) 2013 Siim Meerits <sh0@yutani.ee>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// Package
package ee.yutani.kotoba;

// Imports
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

// Main activity class
public class MainActivity extends Activity implements
    AppMain.ActivityCallback, ActionBar.OnNavigationListener, MenuItem.OnMenuItemClickListener, SearchView.OnQueryTextListener
{
    // Saved state kays
    private static final String STATE_SECTION_ID = "section_id";

    // State
    private boolean m_state_initialize = true;
    private boolean m_state_disable = false;
    private int m_state_section = 0;

    // Application
    private AppMain m_app = null;

    // Progress dialog
    private ProgressDialog m_dialog = null;

    // Menu
    private boolean m_menu_search_enable = true;
    private boolean m_menu_settings_enable = false;

    // Search
    private SearchView m_search_view = null;
    private MainSearchSection m_search_section = null;

    // UI callbacks
    @Override protected void onCreate(Bundle state_saved)
    {
        // State restoring
        if (state_saved != null) {
            // Section
            if (state_saved.containsKey(STATE_SECTION_ID)) {
                m_state_section = state_saved.getInt(STATE_SECTION_ID);
                m_state_initialize = false;
            }
        }

        // Superclass
        super.onCreate(state_saved);

        // Main view
        setContentView(R.layout.activity_main);

        // Actionbar
        /*
        final ActionBar abar = getActionBar();
        abar.setDisplayShowTitleEnabled(false);
        abar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        */

        // Attach to main context
        m_app = (AppMain) this.getApplicationContext();
        m_app.ActivityAttach(this);
    }

    // States
    @Override public void StateNormal()
    {
        // Dismiss dialogs
        LoadingDismiss();

        // Actionbar dropdown list
        m_state_disable = true;
        /*
        final ActionBar abar = getActionBar();
        abar.setListNavigationCallbacks(
            new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new String[] {
                    getString(R.string.section_words),
                    getString(R.string.section_search)
                }
            ),
            this
        );
        if (abar.getSelectedNavigationIndex() != m_state_section)
            abar.setSelectedNavigationItem(m_state_section);
        */
        m_state_disable = false;

        // First time initialization
        if (m_state_initialize)
            StateSection(m_state_section);
    }

    private void StateSection(int section)
    {
        // Clear back-stack
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Select fragment to display
        if (section == 0) {
            Fragment fragment = MainWordsSection.create(null);
            getFragmentManager().beginTransaction().replace(R.id.container, fragment, "frag_main").commit();
        } else if (section == 1) {
            Fragment fragment = MainSearchSection.create("");
            getFragmentManager().beginTransaction().replace(R.id.container, fragment, "frag_main").commit();
        }

        // Set state
        m_state_section = section;
        m_state_initialize = false;
    }

    // Loading progress dialog
    @Override public void LoadingCreate()
    {
        // Dismiss any dialog
        if (m_dialog != null) {
            m_dialog.dismiss();
            m_dialog = null;
        }

        // Create progress dialog
        m_dialog = new ProgressDialog(this);
        m_dialog.setMessage(getString(R.string.db_loading));
        m_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        m_dialog.setCancelable(false);
        m_dialog.show();
    }

    @Override public void LoadingDismiss()
    {
        // Dismiss loading dialog
        if (m_dialog != null) {
            m_dialog.dismiss();
            m_dialog = null;
        }
    }

    @Override public void LoadingProgress(String text)
    {
        // Change dialog text
        if (m_dialog != null)
            m_dialog.setMessage(text);
    }

    @Override public void onSaveInstanceState(Bundle state)
    {
        // Section
        state.putInt(STATE_SECTION_ID, m_state_section);

        // Superclass
        super.onSaveInstanceState(state);
    }

    // Search
    public void SearchActive(MainSearchSection section)
    {
        m_search_section = section;
    }

    public void SearchDeactive()
    {
        m_search_section = null;
    }

    public void SearchEnable()
    {
        m_menu_search_enable = true;
        this.invalidateOptionsMenu();
    }

    public void SearchDisable()
    {
        m_menu_search_enable = false;
        this.invalidateOptionsMenu();
    }

    @Override public boolean onQueryTextChange(String newText)
    {
        return false;
    }

    @Override public boolean onQueryTextSubmit(String query)
    {
        if (m_search_section == null) {
            Fragment fragment = MainSearchSection.create(query);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, fragment, "frag_main");
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            m_search_section.Search(query);
        }
        return true;
    }

    // Menu
    public void SettingsEnable()
    {
        m_menu_settings_enable = true;
        this.invalidateOptionsMenu();
    }

    public void SettingsDisable()
    {
        m_menu_settings_enable = false;
        this.invalidateOptionsMenu();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        // Check
        if (!m_menu_search_enable && !m_menu_settings_enable) {
            m_search_view = null;
            return false;
        }

        // Inflate menu
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem menu_search = menu.findItem(R.id.menu_search);
        MenuItem menu_prefs = menu.findItem(R.id.preferences);

        // Search object
        m_search_view = (SearchView) menu_search.getActionView();
        m_search_view.setOnQueryTextListener(this);

        // Search focus
        m_search_view.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener(){
            public void onFocusChange(View view, boolean has_focus) {
                if (!has_focus) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        // Search
        if (m_menu_search_enable) {
            menu_search.setVisible(true);
        } else {
            menu_search.setVisible(false);
        }

        // Settings
        if (m_menu_settings_enable) {
            menu_prefs.setVisible(true);
            menu_prefs.setOnMenuItemClickListener(this);
        } else {
            menu_prefs.setVisible(false);
        }

        // Show menu
        return true;
    }

    public Activity getActivity()
    {
        return this;
    }

    @Override public boolean onMenuItemClick(MenuItem item)
    {
        // Display
        Fragment fragment = new MainPreferences();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();

        // Success
        return true;
    }

    // Change section
    @Override public boolean onNavigationItemSelected(int section, long id)
    {
        // Switch section
        if (!m_state_disable) {
            if (m_state_section != section)
                StateSection(section);
        }

        // Handled
        return true;
    }
}
