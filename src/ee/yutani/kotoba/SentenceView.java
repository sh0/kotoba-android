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
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ee.yutani.kotoba.DataFileSentence.SentenceInfo;
import ee.yutani.kotoba.DataFileWord.Sref;

// Sentence pseudo-view class
public class SentenceView
{
    private class SentenceItem
    {
        // Variables
        private Sref m_sref = null;
        private SentenceInfo m_info = null;
        private int m_id = -1;

        // Constructor
        public SentenceItem(Sref sref)
        {
            m_sref = sref;
            m_id = m_sref.Id();
        }

        public SentenceItem(int id)
        {
            m_id = id;
        }

        // View
        public View getView(View view)
        {
            // Get sentence
            assert(m_id >= 0);
            if (m_info == null)
                m_info = m_app.Database().FileSentence().InfoEntry(m_id);

            // English
            TextView text_en = (TextView) view.findViewById(R.id.text_en);
            text_en.setText(m_info.TextEn());
            TextPaint tp = text_en.getPaint();

            // Japanese
            TextView text_jp_k = (TextView) view.findViewById(R.id.text_jp_k);
            FuriganaView text_jp_f = (FuriganaView) view.findViewById(R.id.text_jp_f);

            // Show either furigana or normal version
            if (m_show_furigana) {
                text_jp_f.setVisibility(View.VISIBLE);
                text_jp_k.setVisibility(View.GONE);
                int mark_s = -1;
                int mark_e = -1;
                if (m_sref != null) {
                    mark_s = m_sref.Start();
                    mark_e = m_sref.End();
                }
                text_jp_f.text_set(tp, m_info.TextJp(), mark_s, mark_e);
            } else {
                text_jp_f.setVisibility(View.GONE);
                text_jp_k.setVisibility(View.VISIBLE);
                String text = TextNormalize(m_info.TextJp());
                if (m_sref != null && m_sref.Start() >= 0 && m_sref.End() > m_sref.Start()) {
                    SpannableString span = new SpannableString(text);
                    span.setSpan(new StyleSpan(Typeface.BOLD), m_sref.Start(), m_sref.End(), 0);
                    text_jp_k.setText(span);
                } else {
                    text_jp_k.setText(text);
                }
            }

            // Return
            return view;
        }

        // Text normalization
        private String TextNormalize(String str)
        {
            String ret = "";
            int off = str.indexOf('{');
            while (off >= 0) {
                // Prefix
                if (off > 0)
                    ret += str.substring(0, off);
                str = str.substring(off + 1);

                // Separator
                off = str.indexOf(';');
                if (off < 0)
                    break;
                if (off > 0)
                    ret += str.substring(0, off);
                str = str.substring(off + 1);

                // End bracket
                off = str.indexOf('}');
                if (off < 0)
                    break;
                str = str.substring(off + 1);

                // Next bracket
                off = str.indexOf('{');
            }
            ret += str;
            return ret;
        }
    }

    private static class SentenceAdapter extends ArrayAdapter<SentenceItem>
    {
        // Variables
        private LayoutInflater m_inflater;

        // Constructor
        public SentenceAdapter(Context context, LayoutInflater inflater)
        {
            // Superclass
            super(context, R.layout.sentence_item, R.id.text_en);

            // Inflater
            m_inflater = inflater;
        }

        // View
        @Override public View getView(int position, View view, ViewGroup container)
        {
            SentenceItem item = getItem(position);
            if (view == null)
                view = m_inflater.inflate(R.layout.sentence_item, container, false);
            return item.getView(view);
        }
    }

    // App
    private AppMain m_app = null;

    // Variables
    private ListView m_view = null;
    private Context m_context = null;
    private SentenceAdapter m_adapter = null;

    // Settings
    private boolean m_show_furigana = true;

    // Constructor
    public SentenceView(Context context, ListView view, boolean show_furigana)
    {
        // Variables
        m_view = view;
        m_context = context;

        // Settings
        m_show_furigana = show_furigana;

        // Sentence
        m_app = (AppMain) context.getApplicationContext();
    }

    // Set sentences
    public void SetNormal(int[] list)
    {
        m_adapter = new SentenceAdapter(m_context, LayoutInflater.from(m_context));
        for (int id : list)
            m_adapter.add(new SentenceItem(id));
        m_view.setAdapter(m_adapter);
    }

    public void SetSref(Sref[] list)
    {
        m_adapter = new SentenceAdapter(m_context, LayoutInflater.from(m_context));
        for (Sref sref : list)
            m_adapter.add(new SentenceItem(sref));
        m_view.setAdapter(m_adapter);
    }
}
