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

import ee.yutani.kotoba.DataTrainWord.IdTrain;
import ee.yutani.kotoba.DataTrainWord.SectionTrain;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

// Words editing class
public class MainWordsView extends Fragment implements AppMain.FragmentCallback
{
    // Word list adapter
    private class WordAdapter extends BaseAdapter
    {
        // Words
        ArrayList<DictionaryView> m_entry = new ArrayList<DictionaryView>();

        // Constructor
        public WordAdapter()
        {
            for (int i = 0; i < m_section.WordLength(); i++)
                m_entry.add(new DictionaryView(getActivity(), i, m_section));
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
    public static final String STATE_ID = "id";
    public static final String STATE_SCROLL_INDEX = "scroll_index";
    public static final String STATE_SCROLL_TOP = "scroll_top";

    // State
    private int[] m_state_id = null;
    private int m_scroll_index = -1;
    private int m_scroll_top = -1;

    // App
    private AppMain m_app = null;

    // Section
    private IdTrain m_id = null;
    private SectionTrain m_section = null;

    // Listview
    private ListView m_listview = null;

    // Constructor
    static Fragment create(Bundle state)
    {
        // Instance
        MainWordsView fragment = new MainWordsView();

        // Arguments
        if (state != null)
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
            if (state.containsKey(STATE_ID))
                m_state_id = state.getIntArray(STATE_ID);
            if (state.containsKey(STATE_SCROLL_INDEX))
                m_scroll_index = state.getInt(STATE_SCROLL_INDEX);
            if (state.containsKey(STATE_SCROLL_TOP))
                m_scroll_top = state.getInt(STATE_SCROLL_TOP);
        }

        // App
        m_app = (AppMain) this.getActivity().getApplicationContext();

        // Superclass
        super.onCreate(state_saved);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state_saved)
    {
        // View
        m_listview = (ListView) inflater.inflate(R.layout.words_view_root, container, false);

        // Attach fragment
        m_app.FragmentAttach(this);

        // Return
        return m_listview;
    }

    @Override public void StateNormal()
    {
        // Database
        DataMain data = m_app.Database();

        // Ids
        if (m_state_id != null)
            m_id = data.TrainWord().Id(m_state_id);
        if (m_id == null)
            m_id = data.TrainWord().Id();

        // Section
        m_section = m_id.Section();
        assert(m_section != null);

        // List
        WordAdapter adapter = new WordAdapter();
        m_listview.setAdapter(adapter);

        // Scroll
        if (m_scroll_index >= 0)
            m_listview.setSelectionFromTop(m_scroll_index, m_scroll_top);
    }

    // Destroy events
    @Override public void onPause()
    {
        // Ids
        if (m_id != null)
            m_state_id = m_id.Serialize();

        // Scroll state
        m_scroll_index = m_listview.getFirstVisiblePosition();
        View v = m_listview.getChildAt(0);
        m_scroll_top = (v == null) ? 0 : v.getTop();

        // Superclass
        super.onPause();
    }

    @Override public void onDestroyView()
    {
        // Detach fragment
        if (m_app != null)
            m_app.FragmentDetach(this);

        // Superclass
        super.onDestroyView();
    }

    // State
    @Override public void onSaveInstanceState(Bundle state)
    {
        // Put ids
        if (m_state_id != null)
            state.putIntArray(STATE_ID, m_state_id);

        // Put scroll
        if (m_scroll_index >= 0) {
            state.putInt(STATE_SCROLL_INDEX, m_scroll_index);
            state.putInt(STATE_SCROLL_TOP, m_scroll_top);
        }

        // Superclass
        super.onSaveInstanceState(state);
    }
}
