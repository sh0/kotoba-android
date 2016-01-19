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
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import ee.yutani.kotoba.DataFileWord.Sense;
import ee.yutani.kotoba.DataFileWord.WordInfo;
import ee.yutani.kotoba.DataTrainWord.IdTrain;
import ee.yutani.kotoba.DataTrainWord.SectionTrain;
import ee.yutani.kotoba.DataTrainWord.WordTrain;

// Words training fragment
public class MainWordsTrain extends Fragment implements View.OnClickListener, DataTrainSelector.TrainEvent, AppMain.FragmentCallback, SharedPreferences.OnSharedPreferenceChangeListener
{
    private class ChoiceItem extends Button implements View.OnClickListener
    {
        // Variables
        private TextView m_text_f = null;
        private TextView m_text_k = null;
        private WordTrain m_word = null;

        // Constructor
        public ChoiceItem(Context context)
        {
            // Superclass
            super(context);
        }

        // View
        public View getView(View view)
        {
            // Button
            view.setOnClickListener(this);

            // Text
            m_text_f = (TextView) view.findViewById(R.id.text_f);
            m_text_k = (TextView) view.findViewById(R.id.text_k);

            // Try to init
            SetWord(m_word);

            // Return
            return view;
        }

        // Set info
        public void SetWord(WordTrain word)
        {
            // Word
            m_word = word;

            // Check
            if (m_text_k == null || m_text_f == null || m_word == null)
                return;

            // Info
            WordInfo info = m_word.Info();

            // Text
            if (m_state_mode == 1) {

                // Reading text
                String text_r = "";
                for (String str : info.TextR()) {
                    if (text_r.length() > 0)
                        text_r += ", ";
                    text_r += str;
                }
                text_r = text_r.trim();

                // Kanji text
                String text_k = "";
                for (String str : info.TextK()) {
                    if (text_k.length() > 0)
                        text_k += ", ";
                    text_k += str;
                }
                text_k = text_k.trim();

                // Swap kanji and hiragana
                if (text_r.length() > 0 && text_k.length() == 0) {
                    text_k = text_r;
                    text_r = "";
                }

                // Kanji
                m_text_k.setVisibility(View.VISIBLE);
                m_text_k.setText(text_k);

                // Furigana
                if (m_settings_furigana_training) {
                    m_text_f.setVisibility(View.VISIBLE);
                    m_text_f.setText(text_r);
                } else {
                    m_text_f.setVisibility(View.GONE);
                    m_text_f.setText("");
                }

            } else {

                // English text
                String text_e = "";
                Sense[] sense = info.SenseArray();
                for (int i = 0; i < sense.length && i < MAX_SENSE_ENTRIES; i++) {
                    String[] gloss = sense[i].GlossArray();
                    for (int j = 0; j < gloss.length && j < MAX_GLOSS_ENTRIES; j++) {
                        if (text_e.length() > 0)
                            text_e += ", ";
                        text_e += gloss[j];
                    }
                }
                text_e = text_e.trim();

                // Widgets
                m_text_k.setVisibility(View.GONE);
                m_text_f.setText(text_e);
            }
        }

        @Override public void onClick(View v)
        {
            UiAnswer(m_word);
        }
    }

    private static class ChoiceAdapter extends ArrayAdapter<ChoiceItem>
    {
        // Variables
        private LayoutInflater m_inflater;

        // Constructor
        public ChoiceAdapter(Context context, LayoutInflater inflater, List<ChoiceItem> items)
        {
            // Superclass
            super(context, R.layout.words_train_choice, R.id.text_k, items);

            // Inflater
            m_inflater = inflater;
        }

        // View
        @Override public View getView(int position, View view, ViewGroup container)
        {
            ChoiceItem item = getItem(position);
            if (view == null)
                view = m_inflater.inflate(R.layout.words_train_choice, container, false);
            return item.getView(view);
        }
    }

    // Constants
    private static final int MAX_SENSE_ENTRIES = 2;
    private static final int MAX_GLOSS_ENTRIES = 2;

    // Arguments
    public static final String STATE_MODE = "mode";
    public static final String STATE_ID = "id";
    public static final String WORD_COUNT = "word_count";
    
    public static final String STATE_TRAIN_ID_CUR = "train_id_cur";
    public static final String STATE_TRAIN_ID_LAST = "train_id_last";
    public static final String STATE_TRAIN_ID_RECENT = "train_id_recent";
    public static final String STATE_TRAIN_ID_CHOICE = "train_id_choice";
    public static final String STATE_TRAIN_LAST_CORRECT = "train_last_correct";

    public static final String STATE_SENTENCE_ACTIVE = "sentence_active";
    public static final String STATE_SENTENCE_INDEX = "sentence_index";
    public static final String STATE_SENTENCE_TOP = "sentence_top";

    // State
    private int m_word_count = -1;
    private int m_state_mode = -1;
    private int[] m_state_id = null;

    private int m_state_train_id_cur = -1;
    private int m_state_train_id_last = -1;
    private ArrayList<Integer> m_state_train_id_recent = null;
    private int[] m_state_train_id_choice = null;
    private boolean m_state_train_last_correct = false;

    private boolean m_state_sentence_active = false;
    private int m_state_sentence_index = -1;
    private int m_state_sentence_top = -1;

    // Settings
    private boolean m_settings_furigana_sentences = true;
    private boolean m_settings_furigana_training = true;

    // Training
    private IdTrain m_id = null;
    private SectionTrain m_section = null;
    private DataTrainSelector m_train = null;

    // Sentence state
    private boolean m_sentence_set = false;

    // App and data
    private AppMain m_app = null;
    private DataMain m_data = null;

    // General views
    private View m_view_root = null;

    // Question view
    private View m_view_question_root = null;
    private TextView[] m_view_question_bar = null;
    private TextView m_view_question_text_f = null;
    private TextView m_view_question_text_k = null;
    private ListView m_view_question_choice = null;

    // Answer views
    private LinearLayout m_view_answer_root = null;
    private TextView m_view_answer_text_f = null;
    private TextView m_view_answer_text_k = null;
    private TextView m_view_answer_text_e = null;

    // Sentence views
    private ListView m_view_sentence_list = null;

    // Lists
    private ChoiceAdapter m_list_choice = null;
    private SentenceView m_list_sentence = null;

    // Constructor
    static MainWordsTrain create(Bundle state)
    {
        // Instance
        MainWordsTrain fragment = new MainWordsTrain();

        // Arguments
        if (state != null)
            fragment.setArguments(state);

        // Success
        return fragment;
    }

    @Override public void onCreate(Bundle state_saved)
    {
        // State
        Bundle state = state_saved;
        if (state == null)
            state = getArguments();
        if (state != null) {
            // Parameters
            if (state.containsKey(STATE_MODE))
                m_state_mode = state.getInt(STATE_MODE);
            if (state.containsKey(STATE_ID))
                m_state_id = state.getIntArray(STATE_ID);
            if (state.containsKey(WORD_COUNT))
                m_word_count = state.getInt(WORD_COUNT);

            // Train
            if (state.containsKey(STATE_TRAIN_ID_CUR))
                m_state_train_id_cur = state.getInt(STATE_TRAIN_ID_CUR);
            if (state.containsKey(STATE_TRAIN_ID_LAST))
                m_state_train_id_last = state.getInt(STATE_TRAIN_ID_LAST);
            if (state.containsKey(STATE_TRAIN_ID_RECENT))
                m_state_train_id_recent = state.getIntegerArrayList(STATE_TRAIN_ID_RECENT);
            if (state.containsKey(STATE_TRAIN_ID_CHOICE))
                m_state_train_id_choice = state.getIntArray(STATE_TRAIN_ID_CHOICE);
            if (state.containsKey(STATE_TRAIN_LAST_CORRECT))
                m_state_train_last_correct = state.getBoolean(STATE_TRAIN_LAST_CORRECT);

            // Sentence
            if (state.containsKey(STATE_SENTENCE_ACTIVE))
                m_state_sentence_active = state.getBoolean(STATE_SENTENCE_ACTIVE);
            if (state.containsKey(STATE_SENTENCE_INDEX))
                m_state_sentence_index = state.getInt(STATE_SENTENCE_INDEX);
            if (state.containsKey(STATE_SENTENCE_TOP))
                m_state_sentence_top = state.getInt(STATE_SENTENCE_TOP);
        }

        // App
        m_app = (AppMain) getActivity().getApplicationContext();

        // Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        m_settings_furigana_sentences = settings.getBoolean("furigana_sentences", true);
        m_settings_furigana_training = settings.getBoolean("furigana_training", true);

        // Superclass
        super.onCreate(state_saved);
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences settings, String key)
    {
        m_settings_furigana_sentences = settings.getBoolean("furigana_sentences", true);
        m_settings_furigana_training = settings.getBoolean("furigana_training", true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state_saved)
    {
        // View: root
        m_view_root = inflater.inflate(R.layout.words_train_root, container, false);

        // View: question
        m_view_question_root = m_view_root.findViewById(R.id.question);
        View view_bar = m_view_root.findViewById(R.id.bar);
        m_view_question_bar = new TextView[5];
        m_view_question_bar[4] = (TextView) view_bar.findViewById(R.id.progress_a);
        m_view_question_bar[3] = (TextView) view_bar.findViewById(R.id.progress_b);
        m_view_question_bar[2] = (TextView) view_bar.findViewById(R.id.progress_c);
        m_view_question_bar[1] = (TextView) view_bar.findViewById(R.id.progress_d);
        m_view_question_bar[0] = (TextView) view_bar.findViewById(R.id.progress_e);
        m_view_question_text_f = (TextView) m_view_root.findViewById(R.id.text_f);
        m_view_question_text_k = (TextView) m_view_root.findViewById(R.id.text_k);

        // Answers choice list
        ArrayList<ChoiceItem> choice_list = new ArrayList<ChoiceItem>();
        for (int i = 0; i < m_word_count; i++) {
            ChoiceItem choice = new ChoiceItem(getActivity());
            choice_list.add(choice);
        }
        m_list_choice = new ChoiceAdapter(getActivity(), inflater, choice_list);
        m_view_question_choice = (ListView) m_view_root.findViewById(R.id.choice);
        m_view_question_choice.setAdapter(m_list_choice);
        m_view_question_choice.setVisibility(View.GONE);

        // Answer
        m_view_answer_root = (LinearLayout) m_view_root.findViewById(R.id.answer);
        m_view_answer_text_f = (TextView) m_view_root.findViewById(R.id.ans_text_f);
        m_view_answer_text_k = (TextView) m_view_root.findViewById(R.id.ans_text_k);
        m_view_answer_text_e = (TextView) m_view_root.findViewById(R.id.ans_text_e);
        m_view_answer_root.setVisibility(View.GONE);
        m_view_answer_root.setOnClickListener(this);

        // Sentences
        m_view_sentence_list = (ListView) m_view_root.findViewById(R.id.sentence);
        m_view_sentence_list.setVisibility(View.GONE);
        m_list_sentence = new SentenceView(getActivity(), m_view_sentence_list, m_settings_furigana_sentences);

        // Attach fragment
        m_app.FragmentAttach(this);

        // Return
        return m_view_root;
    }

    @Override public void StateNormal()
    {
        // Database
        m_data = m_app.Database();

        // Id check
        if (m_state_id != null)
            m_id = m_data.TrainWord().Id(m_state_id);
        if (m_id == null)
            m_id = m_data.TrainWord().Id();

        // Section and training
        m_section = m_id.Section();
        m_train = new DataTrainSelector(
            this, getActivity(), m_section,
            m_state_train_id_cur, m_state_train_id_last, m_state_train_id_recent,
            m_state_train_id_choice, m_state_train_last_correct
        );

        // UI
        UiStatistics();
        UiQuestion();
        UiLast();
        UiSentences();

        // Sentence scroll
        if (m_state_sentence_index >= 0)
            m_view_sentence_list.setSelectionFromTop(m_state_sentence_index, m_state_sentence_top);
    }

    @Override public void onResume()
    {
        // Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        settings.registerOnSharedPreferenceChangeListener(this);

        // Superclass
        super.onResume();
    }

    // Destroy events
    @Override public void onPause()
    {
        // Put ids
        if (m_id != null)
            m_state_id = m_id.Serialize();

        // Training
        if (m_train != null) {
            DataTrainQuestion next = m_train.Next();
            if (next != null) {
                m_state_train_id_cur = next.IdQuestion();
                m_state_train_id_recent = next.IdRecent();
                m_state_train_id_choice = next.IdChoice();
            }
            DataTrainQuestion last = m_train.Last();
            if (last != null) {
                m_state_train_id_last = last.IdQuestion();
                m_state_train_last_correct = last.Correct();
            }
        }

        // Sentence scroll
        m_state_sentence_index = m_view_sentence_list.getFirstVisiblePosition();
        View v = m_view_sentence_list.getChildAt(0);
        m_state_sentence_top = (v == null) ? 0 : v.getTop();

        // Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        settings.unregisterOnSharedPreferenceChangeListener(this);

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

    // UI update functions
    private void UiAnswer(WordTrain word)
    {
        // Mark sentence as not set
        m_sentence_set = false;

        // Train
        m_train.Answer(word);
    }

    private void UiQuestionDisable()
    {
        // Disable choice
        for (int i = 0; i < m_list_choice.getCount(); i++)
            m_list_choice.getItem(i).setClickable(false);

        // Disable answer
        m_view_answer_root.setEnabled(false);
    }

    // UI updates
    private void UiQuestion()
    {
        // Question
        DataTrainQuestion next = m_train.Next();
        if (next == null) {
            UiQuestionDisable();
            return;
        }

        // Word and info
        WordTrain word = next.WordQuestion();
        if (word == null) {
            UiQuestionDisable();
            return;
        }
        WordInfo info = word.Info();

        // Text
        if (m_state_mode == 0) {

            // Japanese answers
            // Reading text
            String text_r = "";
            for (String str : info.TextR()) {
                if (text_r.length() > 0)
                    text_r += ", ";
                text_r += str;
            }
            text_r = text_r.trim();

            // Kanji text
            String text_k = "";
            for (String str : info.TextK()) {
                if (text_k.length() > 0)
                    text_k += ", ";
                text_k += str;
            }
            text_k = text_k.trim();

            // Swap reading and kanji if necessary
            if (text_k.length() == 0 && text_r.length() > 0) {
                text_k = text_r;
                text_r = "";
            }

            // Set
            if (text_r.length() > 0 && m_settings_furigana_training) {
                m_view_question_text_f.setVisibility(View.VISIBLE);
                m_view_question_text_f.setText(text_r);
                m_view_question_text_k.setText(text_k);
            } else {
                m_view_question_text_f.setVisibility(View.GONE);
                m_view_question_text_f.setText("");
                m_view_question_text_k.setText(text_k);
            }

        } else {

            // English answers
            String text_e = "";
            Sense[] sense = info.SenseArray();
            for (int i = 0; i < sense.length && i < MAX_SENSE_ENTRIES; i++) {
                String[] gloss = sense[i].GlossArray();
                for (int j = 0; j < gloss.length && j < MAX_GLOSS_ENTRIES; j++) {
                    if (text_e.length() > 0)
                        text_e += ", ";
                    text_e += gloss[j];
                }
            }
            text_e = text_e.trim();

            // Set
            m_view_question_text_f.setVisibility(View.GONE);
            m_view_question_text_f.setText("");
            m_view_question_text_k.setText(text_e);
        }

        // Choice
        WordTrain[] choice = m_train.Next().WordChoice();
        for (int i = 0; i < choice.length; i++)
            m_list_choice.getItem(i).SetWord(choice[i]);
        for (int i = 0; i < m_list_choice.getCount(); i++)
            m_list_choice.getItem(i).setClickable(true);
        m_list_choice.notifyDataSetChanged();
        m_view_question_choice.setVisibility(View.VISIBLE);
    }

    private void UiLast()
    {
        // Question
        DataTrainQuestion last = m_train.Last();
        if (last == null) {
            m_view_answer_root.setVisibility(View.GONE);
            return;
        }

        // Word
        WordTrain word = last.WordQuestion();
        if (word == null) {
            m_view_answer_root.setVisibility(View.GONE);
            return;
        }
        boolean correct = last.Correct();
        WordInfo info = word.Info();

        // Visibility
        m_view_answer_root.setVisibility(View.VISIBLE);
        m_view_answer_root.setEnabled(true);

        // Color
        if (correct)
            m_view_answer_root.setBackgroundColor(0x8000ff00);
        else
            m_view_answer_root.setBackgroundColor(0x80ff0000);

        // Reading text
        String text_r = "";
        for (String str : info.TextR()) {
            if (text_r.length() > 0)
                text_r += ", ";
            text_r += str;
        }

        // Kanji text
        String text_k = "";
        for (String str : info.TextK()) {
            if (text_k.length() > 0)
                text_k += ", ";
            text_k += str;
        }

        // English text
        String text_e = "";
        Sense[] sense = info.SenseArray();
        for (int i = 0; i < sense.length && i < MAX_SENSE_ENTRIES; i++) {
            String[] gloss = sense[i].GlossArray();
            for (int j = 0; j < gloss.length && j < MAX_GLOSS_ENTRIES; j++) {
                if (text_e.length() > 0)
                    text_e += ", ";
                text_e += gloss[j];
            }
        }

        // Swap reading and kanji if necessary
        if (text_k.length() == 0 && text_r.length() > 0) {
            text_k = text_r;
            text_r = "";
        }

        // Text
        m_view_answer_text_f.setText(text_r);
        if (text_r.length() > 0) {
            m_view_answer_text_f.setVisibility(View.VISIBLE);
            m_view_answer_text_k.setLines(1);
        } else {
            m_view_answer_text_f.setVisibility(View.GONE);
            m_view_answer_text_k.setLines(2);
        }
        m_view_answer_text_k.setText(text_k);
        m_view_answer_text_e.setText(text_e);

        // Sentences
        //m_list_sentence.set_sref(m_data.sentence().get_sentence_refs(word_previous));
    }

    private void UiStatistics()
    {
        // Level bars
        int[] stats = m_section.Statistics();
        for (int i = 0; i < m_view_question_bar.length; i++) {
            m_view_question_bar[i].setText(Integer.toString(stats[i]));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) m_view_question_bar[i].getLayoutParams();
            params.weight = stats[i];
            m_view_question_bar[i].setLayoutParams(params);
        }
    }

    private void UiSentences()
    {
        // Sentence tab visiblity
        if (!m_state_sentence_active) {

            // Sentences not visible
            m_view_question_root.requestFocus();
            m_view_question_root.setVisibility(View.VISIBLE);
            m_view_sentence_list.setVisibility(View.GONE);

        } else {

            // Sentences visible
            m_view_sentence_list.requestFocus();
            m_view_sentence_list.setVisibility(View.VISIBLE);
            m_view_question_root.setVisibility(View.GONE);

            // Check if already set
            if (!m_sentence_set) {
                // Last question
                DataTrainQuestion last = m_train.Last();
                if (last == null)
                    return;

                // Word
                WordTrain word = last.WordQuestion();
                if (word == null)
                    return;

                // Set word reference
                m_list_sentence.SetSref(word.Info().SrefArray());

                // Mark as set
                m_sentence_set = true;
            }
        }
    }

    @Override public void UiPresent()
    {
        // UI
        UiStatistics();
        UiQuestion();
        UiLast();
        UiSentences();
    }

    // State saving
    @Override public void onSaveInstanceState(Bundle state)
    {
        // Put mode
        if (m_state_mode >= 0)
            state.putInt(STATE_MODE, m_state_mode);

        // Put ids
        if (m_state_id != null)
            state.putIntArray(STATE_ID, m_state_id);
        
        // Put word count
        if(m_word_count >= 0)
            state.putInt(WORD_COUNT, m_word_count);

        // Training
        if (m_state_train_id_cur >= 0)
            state.putInt(STATE_TRAIN_ID_CUR, m_state_train_id_cur);
        if (m_state_train_id_recent != null)
            state.putIntegerArrayList(STATE_TRAIN_ID_RECENT, m_state_train_id_recent);
        if (m_state_train_id_choice != null)
            state.putIntArray(STATE_TRAIN_ID_CHOICE, m_state_train_id_choice);
        if (m_state_train_id_last >= 0)
            state.putInt(STATE_TRAIN_ID_LAST, m_state_train_id_last);
        state.putBoolean(STATE_TRAIN_LAST_CORRECT, m_state_train_last_correct);

        // Sentences
        state.putBoolean(STATE_SENTENCE_ACTIVE, m_state_sentence_active);

        // Scroll
        if (m_state_sentence_index >= 0) {
            state.putInt(STATE_SENTENCE_INDEX, m_state_sentence_index);
            state.putInt(STATE_SENTENCE_TOP, m_state_sentence_top);
        }

        // Superclass
        super.onSaveInstanceState(state);
    }

    // Click on the answer
    @Override public void onClick(View v)
    {
        // Switch state
        m_state_sentence_active = !m_state_sentence_active;
        UiSentences();
    }
}
