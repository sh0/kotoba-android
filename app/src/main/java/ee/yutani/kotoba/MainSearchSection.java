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

import ee.yutani.kotoba.DataSearch.SearchWord;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

// Search section
public class MainSearchSection extends Fragment implements AppMain.FragmentCallback
{
    // Result list adapter
    private class SearchAdapter extends BaseAdapter
    {
        // Words
        ArrayList<DictionaryView> m_entry = new ArrayList<DictionaryView>();

        // Constructor
        public SearchAdapter(ArrayList<SearchWord> words)
        {
            for (SearchWord word : words)
                m_entry.add(new DictionaryView(getActivity(), word));
        }

        // Gets
        @Override public int getCount() { return m_entry.size(); }
        @Override public Object getItem(int position) { return m_entry.get(position); }
        @Override public long getItemId(int position) { return position; }

        // View
        @Override public View getView(int position, View view, ViewGroup parent)
        {
            return m_entry.get(position).getView(position, view, parent);
        }
    }

    // Arguments
    public static final String STATE_QUERY = "query";
    public static final String STATE_SCROLL_INDEX = "scroll_index";
    public static final String STATE_SCROLL_TOP = "scroll_top";

    // App
    private AppMain m_app = null;
    private DataSearch m_search = null;

    // State
    private String m_state_query = "";
    private int m_state_scroll_index = -1;
    private int m_state_scroll_top = -1;

    // List
    private ListView m_result_view = null;
    private SearchAdapter m_result_adapter = null;

    // Constructor
    static MainSearchSection create(String query)
    {
        // Instance
        MainSearchSection fragment = new MainSearchSection();

        // Arguments
        Bundle state = new Bundle();
        state.putString(STATE_QUERY, query);
        fragment.setArguments(state);

        // Success
        return fragment;
    }

    // Create events
    @Override public void onCreate(Bundle state_saved)
    {
        // State
        Bundle state = state_saved;
        if (state == null)
            state = getArguments();
        if (state != null) {
            if (state.containsKey(STATE_QUERY))
                m_state_query = state.getString(STATE_QUERY);
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
        // Root
        View root_view = inflater.inflate(R.layout.search_section, container, false);

        // List view
        m_result_view = (ListView) root_view.findViewById(R.id.results);

        // Attach fragment
        m_app.FragmentAttach(this);

        // Menu
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null)
            activity.SearchActive(this);

        // Return
        return root_view;
    }

    @Override public void StateNormal()
    {
        // Database
        DataMain data = m_app.Database();
        m_search = data.Search();

        // Search
        if (m_state_query.length() > 0)
            Search(m_state_query);

        // List scroll
        if (m_state_scroll_index >= 0)
            m_result_view.setSelectionFromTop(m_state_scroll_index, m_state_scroll_top);
    }

    // Destroy events
    @Override public void onPause()
    {
        // Sentence scroll
        m_state_scroll_index = m_result_view.getFirstVisiblePosition();
        View v = m_result_view.getChildAt(0);
        m_state_scroll_top = (v == null) ? 0 : v.getTop();

        // Superclass
        super.onPause();
    }

    @Override public void onDestroyView()
    {
        // Menu
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null)
            activity.SearchDeactive();

        // Detach fragment
        if (m_app != null)
            m_app.FragmentDetach(this);

        // Superclass
        super.onDestroyView();
    }

    // State
    @Override public void onSaveInstanceState(Bundle state)
    {
        // Put query
        if (m_state_query.length() > 0)
            state.putString(STATE_QUERY, m_state_query);

        // Put scroll
        if (m_state_scroll_index >= 0) {
            state.putInt(STATE_SCROLL_INDEX, m_state_scroll_index);
            state.putInt(STATE_SCROLL_TOP, m_state_scroll_top);
        }

        // Superclass
        super.onSaveInstanceState(state);
    }

    // Search
    public void Search(String query)
    {
        // Save query string
        m_state_query = query;

        // Search
        ArrayList<SearchWord> result = m_search.Search(query);
        m_result_adapter = new SearchAdapter(result);
        m_result_view.setAdapter(m_result_adapter);

        // Focus
        m_result_view.requestFocus();
    }
}
