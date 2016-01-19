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
import java.util.ArrayList;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import ee.yutani.kotoba.DataTrainWord.IdTrain;
import ee.yutani.kotoba.DataTrainWord.SectionTrain;

// Fragment class
public class MainWordsSection extends Fragment implements AppMain.FragmentCallback
{
    private static final int SECTION_TYPE_HEADER = 0;
    private static final int SECTION_TYPE_ENTRY = 1;
    private static final int SECTION_TYPE_COUNT = 2;

    private interface SectionInterface
    {
        public String getItem();
        public int getItemViewType();
        public View getView(View view, LayoutInflater inflater, ViewGroup container);
        public void Update();
    }

    private class SectionHeader implements SectionInterface
    {
        // Title
        private String m_title;

        // Constructor
        public SectionHeader(String title)
        {
            m_title = title;
        }

        // Gets
        @Override public String getItem() { return m_title; }
        @Override public int getItemViewType() { return SECTION_TYPE_HEADER; }

        // View
        @Override public View getView(View view, LayoutInflater inflater, ViewGroup container)
        {
            // View
            if (view == null)
                view = inflater.inflate(R.layout.words_section_header, container, false);

            // Textview
            TextView text_view = (TextView) view;
            text_view.setText(m_title);

            // Return
            return view;
        }

        // Update
        @Override public void Update() { }
    }

    private class SectionEntry implements SectionInterface, View.OnClickListener
    {
        // Buttons
        private class Button
        {
            // Variables
            private String m_title;
            private int m_id;

            // Constructor
            public Button(String title, int id)
            {
                m_title = title;
                m_id = id;
            }

            // Gets
            public String Title() { return m_title; }
            public int Id() { return m_id; }
        }

        // Button click event object
        private class ButtonEvent implements View.OnClickListener, MenuItem.OnMenuItemClickListener
        {
            // Variables
            private int m_button;

            // Constructor
            public ButtonEvent(int button) { m_button = button; }

            // Events
            @Override public void onClick(View view) { event(m_button); }
            @Override public boolean onMenuItemClick(MenuItem item) { event(m_button); return true; }
        }

        // Variables
        private Context m_context;
        private SectionTrain m_section;
        private ArrayList<Button> m_button;

        // Progress view
        private TextView[] m_progress = null;

        // Constructor
        public SectionEntry(Context context, SectionTrain section)
        {
            // Properties
            m_context = context;
            m_section = section;

            // Buttons
            m_button = new ArrayList<Button>();
            m_button.add(new Button(getString(R.string.words_menu_train_english), 1));
            m_button.add(new Button(getString(R.string.words_menu_train_japanese), 2));
            m_button.add(new Button(getString(R.string.words_menu_view), 3));
            m_button.add(new Button(getString(R.string.words_menu_reset), 4));
        }

        // Gets
        @Override public String getItem()
        {
            // Text
            String[] split = m_section.Name().split("/");
            String text = split[0];
            if (split.length > 1)
                text = split[1];
            return text;
        }
        @Override public int getItemViewType() { return SECTION_TYPE_ENTRY; }

        // View
        @Override public View getView(View view, LayoutInflater inflater, ViewGroup container)
        {
            // View
            if (view == null)
                view = inflater.inflate(R.layout.words_section_item, container, false);

            // Title
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(getItem());

            // Bar
            View bar = view.findViewById(R.id.bar);
            m_progress = new TextView[5];
            m_progress[4] = (TextView) bar.findViewById(R.id.progress_a);
            m_progress[3] = (TextView) bar.findViewById(R.id.progress_b);
            m_progress[2] = (TextView) bar.findViewById(R.id.progress_c);
            m_progress[1] = (TextView) bar.findViewById(R.id.progress_d);
            m_progress[0] = (TextView) bar.findViewById(R.id.progress_e);

            // Update
            Update();

            // Main button
            RelativeLayout button_main = (RelativeLayout) view.findViewById(R.id.main);
            button_main.setOnClickListener(new ButtonEvent(0));

            // Popup button
            ImageButton button_popup = (ImageButton) view.findViewById(R.id.popup);
            button_popup.setOnClickListener(this);

            // Return
            return view;
        }

        // Update
        @Override public void Update()
        {
            // Check
            if (m_progress == null)
                return;

            // Statistics
            int[] stats = m_section.Statistics();

            // Progress
            for (int i = 0; i < m_progress.length; i++) {
                if (m_progress[i] == null)
                    continue;
                m_progress[i].setText(Integer.toString(stats[i]));
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) m_progress[i].getLayoutParams();
                params.weight = stats[i];
                m_progress[i].setLayoutParams(params);
            }
        }

        // Button click
        protected void event(int button)
        {
            SectionEvent(m_section, button);
        }

        // Menu popup
        @Override public void onClick(View view)
        {
            // Popup
            PopupMenu popup = new PopupMenu(m_context, view);
            //popup.getMenuInflater().inflate(R.menu.progress_menu, popup.getMenu());

            // Menu
            Menu menu = popup.getMenu();
            for (Button button : m_button) {
                MenuItem item = menu.add(button.Title());
                item.setOnMenuItemClickListener(new ButtonEvent(button.Id()));
            }

            // Show
            popup.show();
        }
    }

    // List view adapter with progress bar and buttons
    private class SectionAdapter extends BaseAdapter
    {
        // Variables
        private LayoutInflater m_inflater;
        private ArrayList<SectionInterface> m_items;

        // Constructor
        public SectionAdapter(LayoutInflater inflater, ArrayList<SectionInterface> items)
        {
            m_inflater = inflater;
            m_items = items;
            notifyDataSetChanged();
        }

        // Types
        @Override public int getItemViewType(int position) { return m_items.get(position).getItemViewType(); }
        @Override public int getViewTypeCount() { return SECTION_TYPE_COUNT; }

        // Items
        @Override public int getCount() { return m_items.size(); }
        @Override public String getItem(int position) { return m_items.get(position).getItem(); }
        @Override public long getItemId(int position) { return position; }

        // View
        @Override public View getView(int position, View view, ViewGroup container)
        {
            return m_items.get(position).getView(view, m_inflater, container);
        }

        // Update
        public void Update()
        {
            for (SectionInterface section : m_items)
                section.Update();
        }

        // List adapter overrides
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int position) { return false; }
    }

    // Arguments
    public static final String STATE_ID = "id";
    public static final String STATE_SCROLL_INDEX = "list_index";
    public static final String STATE_SCROLL_TOP = "list_top";

    // State
    private int[] m_state_id = null;
    private int m_state_scroll_index = -1;
    private int m_state_scroll_top = -1;

    // Section
    private AppMain m_app = null;
    private DataMain m_data = null;
    private IdTrain m_id = null;
    private SectionTrain[] m_section = null;
    private TreeMap<String, ArrayList<SectionTrain>> m_map = new TreeMap<String, ArrayList<SectionTrain>>();

    // View
    private LayoutInflater m_inflater = null;
    private ListView m_list_view = null;
    private SectionAdapter m_list_adapter = null;

    // Reset dialog
    private AlertDialog m_reset_dialog = null;

    // Constructor
    static MainWordsSection create(Bundle state)
    {
        // Instance
        MainWordsSection fragment = new MainWordsSection();

        // Arguments
        if (state != null)
            fragment.setArguments(state);

        // Success
        return fragment;
    }

    // Creation events
    @Override public void onCreate(Bundle state_saved)
    {
        // State
        Bundle state = state_saved;
        if (state == null)
            state = getArguments();
        if (state != null) {
            if (state.containsKey(STATE_ID))
                m_state_id = state.getIntArray(STATE_ID);
            if (state.containsKey(STATE_SCROLL_INDEX))
                m_state_scroll_index = state.getInt(STATE_SCROLL_INDEX);
            if (state.containsKey(STATE_SCROLL_TOP))
                m_state_scroll_top = state.getInt(STATE_SCROLL_TOP);
        }

        // App
        m_app = (AppMain) this.getActivity().getApplicationContext();

        // Superclass
        super.onCreate(state_saved);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state_saved)
    {
        // View
        m_inflater = inflater;
        View root_view = inflater.inflate(R.layout.words_section, container, false);
        m_list_view = (ListView) root_view.findViewById(R.id.list);

        // Attach fragment
        m_app.FragmentAttach(this);

        // Menu
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null)
            activity.SettingsEnable();

        // Return
        return root_view;
    }

    @Override public void StateNormal()
    {
        // Database
        m_data = m_app.Database();

        // Id
        if (m_state_id != null)
            m_id = m_data.TrainWord().Id(m_state_id);
        if (m_id == null)
            m_id = m_data.TrainWord().Id();

        // Sections
        assert(m_id != null);
        assert(m_id.Section() != null);
        m_section = m_id.Section().SectionArray();

        // Map
        m_map.clear();
        for (SectionTrain section : m_section) {
            // Text
            String[] split = section.Name().split("/");
            String title = "";
            if (split.length > 1)
                title = split[0];

            // Insert
            if (!m_map.containsKey(title))
                m_map.put(title, new ArrayList<SectionTrain>());
            m_map.get(title).add(section);
        }

        // Populate list
        ArrayList<SectionInterface> progress_list = new ArrayList<SectionInterface>();
        for (String key : m_map.keySet()) {

            // Header
            if (key.length() > 0)
                progress_list.add(new SectionHeader(key));

            // Section list
            ArrayList<SectionTrain> list = m_map.get(key);
            for (SectionTrain section : list) {
                SectionEntry item = new SectionEntry(getActivity(), section);
                progress_list.add(item);
            }
        }

        // Attach list
        m_list_adapter = new SectionAdapter(m_inflater, progress_list);
        m_list_view.setAdapter(m_list_adapter);

        // List scroll
        if (m_state_scroll_index >= 0)
            m_list_view.setSelectionFromTop(m_state_scroll_index, m_state_scroll_top);
    }

    // Destroy events
    @Override public void onPause()
    {
        // Ids
        if (m_id != null)
            m_state_id = m_id.Serialize();

        // Sentence scroll
        m_state_scroll_index = m_list_view.getFirstVisiblePosition();
        View v = m_list_view.getChildAt(0);
        m_state_scroll_top = (v == null) ? 0 : v.getTop();

        // Dialog
        if (m_reset_dialog != null) {
            m_reset_dialog.dismiss();
            m_reset_dialog = null;
        }

        // Superclass
        super.onPause();
    }

    @Override public void onDestroyView()
    {
        // Menu
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null)
            activity.SettingsDisable();

        // Detach fragment
        if (m_app != null)
            m_app.FragmentDetach(this);

        // Superclass
        super.onDestroyView();
    }

    // Menu click event
    public void SectionEvent(SectionTrain section, int button)
    {
        // Button action
        Fragment next_fragment = null;
        switch (button) {
            case 0:
                // Main
                if (section.SectionLength() > 0) {
                    Bundle next_state = new Bundle();
                    next_state.putIntArray(STATE_ID, section.Id().Serialize());
                    next_fragment = create(next_state);
                }
                break;

            case 1:
            case 2:
                // Test
                {
                    Bundle next_state = new Bundle();
                    if (button == 1) {
                        // English answers
                        next_state.putInt(MainWordsTrain.STATE_MODE, 0);
                    } else {
                        // Japanese answers
                        next_state.putInt(MainWordsTrain.STATE_MODE, 1);
                    }
                    next_state.putIntArray(STATE_ID, section.Id().Serialize());
                    
                    int word_count = DataTrainQuestion.NUM_CHOICE > section.WordLength() ? section.WordLength() : DataTrainQuestion.NUM_CHOICE;
                    next_state.putInt("word_count", word_count);
                    
                    next_fragment = MainWordsTrain.create(next_state);
                }
                break;

            case 3:
                // View
                {
                    Bundle next_state = new Bundle();
                    next_state.putIntArray(STATE_ID, section.Id().Serialize());
                    next_fragment = MainWordsView.create(next_state);
                }
                break;

            case 4:
                // Reset
                {
                    // Text
                    String[] split = section.Name().split("/");
                    String title = split[0];
                    if (split.length > 1)
                        title = split[1];

                    // Reset action class
                    class ResetAlert implements DialogInterface.OnClickListener
                    {
                        // Section reference
                        private SectionTrain m_s = null;

                        // Constructor
                        public ResetAlert(SectionTrain section)
                        {
                            m_s = section;
                        }

                        // Events
                        public void onClick(DialogInterface dialog, int id)
                        {
                            m_s.Reset();
                            m_list_adapter.Update();
                            dialog.dismiss();
                        }
                    }

                    // Build dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Reset confirmation");
                    builder.setMessage("Are you sure you want to reset training data in category " + title + "?");
                    builder.setCancelable(true);
                    builder.setPositiveButton("Yes", new ResetAlert(section));
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

                    // Show dialog
                    m_reset_dialog = builder.create();
                    m_reset_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override public void onDismiss(DialogInterface dialog) {
                            m_reset_dialog = null;
                        }
                    });
                    m_reset_dialog.show();
                }
        }

        // Transition
        if (next_fragment != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.container, next_fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();
        }
    }

    // State
    @Override public void onSaveInstanceState(Bundle state)
    {
        // Put ids
        if (m_state_id != null)
            state.putIntArray(STATE_ID, m_state_id);

        // Put scroll
        if (m_state_scroll_index >= 0) {
            state.putInt(STATE_SCROLL_INDEX, m_state_scroll_index);
            state.putInt(STATE_SCROLL_TOP, m_state_scroll_top);
        }

        // Superclass
        super.onSaveInstanceState(state);
    }
}
