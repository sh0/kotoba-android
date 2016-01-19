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
import java.util.LinkedList;
import java.util.Queue;

import android.os.AsyncTask;
import ee.yutani.kotoba.DataTrainWord.SectionTrain;
import ee.yutani.kotoba.DataTrainWord.WordTrain;

// Training question class
public class DataTrainQuestion extends AsyncTask<Boolean, Void, Void>
{
    // Interfaces
    public interface QuestionNextEvent
    {
        void ResultNext(DataTrainQuestion question);
    }

    public interface QuestionLastEvent
    {
        void ResultLast(DataTrainQuestion question);
    }

    // Constants
    private static final int MAX_RECENT_WORDS = 2;
    public static final int NUM_CHOICE = 5;

    // App and section
    private SectionTrain m_section = null;

    // Events
    private QuestionNextEvent m_event_next = null;
    private QuestionLastEvent m_event_last = null;

    // Settings
    private double m_settings_bias = 10.0;

    // Data
    private int m_id_next;
    private int[] m_id_choice;
    private Queue<Integer> m_id_recent = null;

    private WordTrain m_word_next;
    private WordTrain[] m_word_choice;

    private boolean m_correct = false;

    // Constructors
    public DataTrainQuestion(
        QuestionLastEvent event_last, QuestionNextEvent event_next,
        SectionTrain section, double settings_bias,
        int id_next, ArrayList<Integer> id_recent, int[] id_choice, boolean correct
    ) {
        // Section
        m_section = section;

        // Event
        m_event_last = event_last;
        m_event_next = event_next;

        // Settings
        m_settings_bias = settings_bias;

        // Other
        m_correct = correct;

        // Set ids
        m_id_next = id_next;
        m_id_recent = new LinkedList<Integer>();
        if (id_recent != null && id_recent.size() > 0) {
            for (int recent : id_recent)
                m_id_recent.add(recent);
        }
        m_id_choice = id_choice;

        // Execute
        this.execute(true);
    }

    public DataTrainQuestion(
        QuestionNextEvent event_next,
        SectionTrain section, double settings_bias,
        Queue<Integer> recent
    ) {
        // Section
        m_section = section;

        // Event
        m_event_next = event_next;

        // Settings
        m_settings_bias = settings_bias;

        // Recent
        m_id_recent = recent;
        if (m_id_recent == null)
            m_id_recent = new LinkedList<Integer>();

        // Statistics and values
        int[] stats = m_section.Statistics();
        byte[] values = m_section.WordValues();

        // Calculate probability of choosing word from certain level
        double factor_mul = m_settings_bias;
        double factor_cur = 1.0;
        double factor_total = 0.0f;
        double[] factor_levels = new double[stats.length];

        /*
        for (int i = 0; i < stats.length; i++) {
            if (stats[i] > 0) {
                factor_levels[i] = factor_cur;
                factor_total += factor_levels[i];
                factor_cur /= factor_mul;
            } else {
                factor_levels[i] = 0;
            }
        }
        */
        for (int i = stats.length - 1; i >= 0; i--) {
            factor_levels[i] = (double)stats[i] * factor_cur;
            factor_total += factor_levels[i];
            factor_cur *= factor_mul;
        }
        for (int i = 0; i < factor_levels.length; i++)
            factor_levels[i] /= factor_total;

        // Loop until valid found
        boolean word_valid = false;
        while (!word_valid) {
            // Select level
            double level_rnd = Math.random();
            int level_num = 0;
            for (level_num = 0; level_num < factor_levels.length - 1; level_num++) {
                level_rnd -= factor_levels[level_num];
                if (level_rnd <= 0.0f)
                    break;
            }
            if (stats[level_num] == 0)
                continue;

            // Select word
            int word_num = (int)(Math.random() * (double)stats[level_num]);
            word_num = Math.max(Math.min(word_num, stats[level_num] - 1), 0);
            m_id_next = 0;
            while (word_num >= 0) {
                while ((values[m_id_next++] & 0x7f) != level_num)
                    ;
                word_num--;
            }
            m_id_next--;

            // Check if recently used
            word_valid = true;
            for (Integer last : m_id_recent) {
                if (last == m_id_next) {
                    word_valid = false;
                    break;
                }
            }
        }

        // Save word in recent queue
        m_id_recent.add(m_id_next);
        while (m_id_recent.size() > MAX_RECENT_WORDS)
            m_id_recent.remove();

        // Choice generation
        int size = NUM_CHOICE > m_section.WordLength() ? m_section.WordLength() : NUM_CHOICE;
        m_id_choice = GenerateChoice(size, m_section.WordLength(), m_id_next);

        // Execute
        this.execute(false);
    }

    // Word gets
    public WordTrain WordQuestion() { return m_word_next; }
    public WordTrain[] WordChoice() { return m_word_choice; }

    // Id gets
    public int IdQuestion() { return m_id_next; }
    public ArrayList<Integer> IdRecent() { return new ArrayList<Integer>(m_id_recent); }
    public int[] IdChoice() { return m_id_choice; }

    // Other gets and sets
    public Queue<Integer> Recent() { return m_id_recent; }
    public boolean Correct() { return m_correct; }
    public void SetCorrect(boolean correct) { m_correct = correct; }

    // Choice generation
    private static int[] GenerateChoice(int size, int range, int answer)
    {
        // Check
        assert(range > size);

        // Random number array
        int[] arr = new int[size];

        // Generate unique integers
        arr[0] = answer;
        for (int i = 1; i < size; i++) {
            boolean valid = false;
            int num = 0;
            while (!valid) {
                valid = true;
                num = Math.min((int)(Math.random() * (double)range), range - 1);
                for (int j = 0; j < i; j++) {
                    if (arr[j] == num)
                        valid = false;
                }
            }
            arr[i] = num;
        }

        // Swap answer with random element
        int snum = Math.min((int)(Math.random() * (double)size), size - 1);
        int sval = arr[snum];
        arr[snum] = arr[0];
        arr[0] = sval;

        // Return
        return arr;
    }

    @Override protected Void doInBackground(Boolean... restore)
    {
        if (restore[0]) {

            // State restoring
            m_word_next = m_section.WordEntry(m_id_next);
            if (m_id_choice != null && m_id_choice.length > 0) {
                m_word_choice = new WordTrain[m_id_choice.length];
                for (int i = 0; i < m_id_choice.length; i++)
                    m_word_choice[i] = m_section.WordEntry(m_id_choice[i]);
            }

        } else {

            // Load words
            m_word_next = m_section.WordEntry(m_id_next);

            // Load choices
            m_word_choice = new WordTrain[m_id_choice.length];
            for (int i = 0; i < m_id_choice.length; i++)
                m_word_choice[i] = m_section.WordEntry(m_id_choice[i]);

        }
        return null;
    }

    @Override protected void onPostExecute(Void result)
    {
        if (m_event_last != null)
            m_event_last.ResultLast(this);
        if (m_event_next != null)
            m_event_next.ResultNext(this);
    }
}
