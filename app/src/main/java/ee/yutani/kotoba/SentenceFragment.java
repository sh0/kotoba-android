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
import ee.yutani.kotoba.DataFileWord.WordInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

// Sentence fragment class
public class SentenceFragment extends Fragment implements AppMain.FragmentCallback
{
    // Arguments
    public static final String STATE_WORD_ID = "word_id";
    public static final String STATE_SCROLL_INDEX = "scroll_index";
    public static final String STATE_SCROLL_TOP = "scroll_top";

    // State
    private int m_word_id = -1;

    private int m_scroll_index = -1;
    private int m_scroll_top = -1;

    // Settings
    private boolean m_settings_furigana_sentences = true;

    // Listview
    private ListView m_listview = null;
    private SentenceView m_view_sentence = null;

    // App
    private AppMain m_app = null;

    // Constructor
    static Fragment create(Bundle state)
    {
        // Instance
        SentenceFragment fragment = new SentenceFragment();

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
            if (state.containsKey(STATE_WORD_ID))
                m_word_id = state.getInt(STATE_WORD_ID);
            if (state.containsKey(STATE_SCROLL_INDEX))
                m_scroll_index = state.getInt(STATE_SCROLL_INDEX);
            if (state.containsKey(STATE_SCROLL_TOP))
                m_scroll_top = state.getInt(STATE_SCROLL_TOP);
        }

        // App
        m_app = (AppMain) getActivity().getApplicationContext();

        // Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        m_settings_furigana_sentences = settings.getBoolean("furigana_sentences", true);

        // Superclass
        super.onCreate(state_saved);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state_saved)
    {
        // View
        m_listview = (ListView) inflater.inflate(R.layout.words_view_root, container, false);

        // List
        m_view_sentence = new SentenceView(getActivity(), m_listview, m_settings_furigana_sentences);

        // Attach fragment
        m_app.FragmentAttach(this);

        // Return
        return m_listview;
    }

    @Override public void StateNormal()
    {
        // Database
        DataMain data = m_app.Database();

        // Section
        WordInfo word = data.FileWord().InfoEntry(m_word_id);

        // List
        m_view_sentence.SetSref(word.SrefArray());

        // Scroll
        if (m_scroll_index >= 0)
            m_listview.setSelectionFromTop(m_scroll_index, m_scroll_top);
    }

    // Destroy events
    @Override public void onPause()
    {
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
        // Put state
        if (m_word_id >= 0)
            state.putInt(STATE_WORD_ID, m_word_id);

        // Put scroll
        if (m_scroll_index >= 0) {
            state.putInt(STATE_SCROLL_INDEX, m_scroll_index);
            state.putInt(STATE_SCROLL_TOP, m_scroll_top);
        }

        // Superclass
        super.onSaveInstanceState(state);
    }
}
