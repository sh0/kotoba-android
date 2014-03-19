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
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ee.yutani.kotoba.DataFileWord.Sense;
import ee.yutani.kotoba.DataFileWord.WordInfo;
import ee.yutani.kotoba.DataSearch.SearchWord;
import ee.yutani.kotoba.DataTrainWord.SectionTrain;
import ee.yutani.kotoba.DataTrainWord.WordTrain;

// Dictionary view class
public class DictionaryView implements View.OnClickListener
{
    // Context
    private Activity m_activity = null;

    // Word from search
    private SearchWord m_word_search = null;

    // Word from id
    private int m_id = -1;
    private SectionTrain m_section = null;

    // Word info
    private WordInfo m_info = null;

    // Color info
    private boolean m_color_exists = false;
    private int m_color_level = 0;

    // Constructors
    public DictionaryView(Activity activity, SearchWord word)
    {
        m_activity = activity;
        m_word_search = word;
    }

    public DictionaryView(Activity activity, int id, SectionTrain section)
    {
        m_activity = activity;
        m_id = id;
        m_section = section;
    }

    // Info retrieval
    private void GetInfo()
    {
        // Search mode
        if (m_info == null && m_word_search != null)
            m_info = m_word_search.Word();

        // View mode
        if (m_info == null && m_id >= 0 && m_section != null) {
            WordTrain entry = m_section.WordEntry(m_id);
            m_info = entry.Info();
            m_color_exists = true;
            m_color_level = entry.Score();
        }
    }

    // View
    public View getView(int position, View view, ViewGroup parent)
    {
        // View
        if (view == null)
            view = LayoutInflater.from(m_activity).inflate(R.layout.dictionary_view, parent, false);

        // Views
        TextView view_score = (TextView) view.findViewById(R.id.score);
        TextView view_text_f = (TextView) view.findViewById(R.id.text_f);
        TextView view_text_k = (TextView) view.findViewById(R.id.text_k);
        TextView view_text_e = (TextView) view.findViewById(R.id.text_e);

        // Info populating
        if (m_info == null)
            GetInfo();
        assert(m_info != null);

        // Color
        if (m_color_exists) {
            view_score.setVisibility(View.VISIBLE);
            int color = 0xff808080;
            switch (m_color_level) {
                case 1: color = 0xffff4c00; break;
                case 2: color = 0xffffc300; break;
                case 3: color = 0xfffbfe00; break;
                case 4: color = 0xff74e600; break;
            }
            view_score.setBackgroundColor(color);
        } else {
            view_score.setVisibility(View.GONE);
        }

        // Reading text
        String text_r = "";
        for (String str : m_info.TextR()) {
            if (text_r.length() > 0)
                text_r += ", ";
            text_r += str;
        }

        // Kanji text
        String text_k = "";
        for (String str : m_info.TextK()) {
            if (text_k.length() > 0)
                text_k += ", ";
            text_k += str;
        }

        // Swap reading and kanji if necessary
        if (text_k.length() == 0 && text_r.length() > 0) {
            text_k = text_r;
            text_r = "";
        }

        // Reading text
        if (text_r.length() > 0) {
            view_text_f.setText(text_r);
            view_text_f.setVisibility(View.VISIBLE);
        } else {
            view_text_f.setText("");
            view_text_f.setVisibility(View.GONE);
        }

        // Kanji text
        view_text_k.setText(text_k);

        // English text
        CharSequence text_e = "";
        Sense[] sense_arr = m_info.SenseArray();
        for (int i = 0; i < sense_arr.length; i++) {
            Sense sense = sense_arr[i];
            String text_g = "";
            for (String gloss : sense.GlossArray()) {
                gloss = gloss.trim();
                if (gloss.length() > 0) {
                    if (text_g.length() > 0)
                        text_g += ", ";
                    text_g += gloss;
                }
            }
            if (i != sense_arr.length - 1)
                text_g += "\n";
            SpannableString span = new SpannableString(text_g);
            span.setSpan(new BulletSpan(12), 0, text_g.length(), 0);
            text_e = TextUtils.concat(text_e, span);
        }

        // English text
        view_text_e.setText(text_e);

        // Click handler
        view.setOnClickListener(this);

        // Return view
        return view;
    }

    // Click
    @Override public void onClick(View view)
    {
        // Info populating
        if (m_info == null)
            GetInfo();
        assert(m_info != null);

        // Sentence
        Bundle next_state = new Bundle();
        next_state.putInt(SentenceFragment.STATE_WORD_ID, m_info.Id());
        Fragment fragment = SentenceFragment.create(next_state);

        // Fragment
        FragmentTransaction ft = m_activity.getFragmentManager().beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
    }
}
