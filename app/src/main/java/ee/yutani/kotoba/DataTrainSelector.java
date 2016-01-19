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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import ee.yutani.kotoba.DataTrainWord.SectionTrain;
import ee.yutani.kotoba.DataTrainWord.WordTrain;

// Word selection algorithms for training
public class DataTrainSelector implements DataTrainQuestion.QuestionLastEvent, DataTrainQuestion.QuestionNextEvent
{
    public interface TrainEvent
    {
        public void UiPresent();
    }

    // App and section
    private SectionTrain m_section = null;

    // Questions
    private DataTrainQuestion m_next = null;
    private DataTrainQuestion m_last = null;
    private Queue<DataTrainQuestion> m_question = new LinkedList<DataTrainQuestion>();

    // State
    private boolean m_immediate = true;

    // Settings
    private double m_settings_bias = 10.0;

    // Event interface
    private TrainEvent m_event = null;

    // Gets
    public DataTrainQuestion Next() { return m_next; }
    public DataTrainQuestion Last() { return m_last; }

    // Constructor
    public DataTrainSelector(
        TrainEvent event, Context context, SectionTrain section,
        int id_next, int id_last, ArrayList<Integer> id_recent, int[] id_choice, boolean last_correct
    ) {
        // App and section
        m_section = section;

        // Settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        m_settings_bias = (double) Integer.parseInt(settings.getString("training_bias", "10"), 10);
        if (m_settings_bias < 1.0)
            m_settings_bias = 1.0;
        if (m_settings_bias > 100.0)
            m_settings_bias = 100.0;

        // Train event
        m_event = event;

        // Next
        if (id_next >= 0)
            m_next = new DataTrainQuestion(null, this, m_section, m_settings_bias, id_next, id_recent, id_choice, false);
        else
            m_next = new DataTrainQuestion(this, m_section, m_settings_bias, null);

        // Last
        if (id_last >= 0)
            m_last = new DataTrainQuestion(this, null, m_section, m_settings_bias, id_last, null, null, last_correct);
    }

    // Operations
    public boolean Answer(WordTrain answer)
    {
        // Answer checking
        if (answer != null && m_next != null) {
            // Check
            int score = m_next.WordQuestion().Score();
            if (answer.Info().Id() == m_next.WordQuestion().Info().Id()) {
                score++;
                m_next.SetCorrect(true);
            } else {
                score--;
                m_next.SetCorrect(false);
            }

            // Set new score
            if (score < 0)
                score = 0;
            if (score > DataTrainWord.TRAIN_LEVELS - 1)
                score = DataTrainWord.TRAIN_LEVELS - 1;
            m_next.WordQuestion().SetScore(score);

            // Swap last answer
            m_last = m_next;
            m_next = null;
        }

        // Check queue
        if (m_question.size() > 0) {

            // Push to UI
            m_next = m_question.remove();
            m_event.UiPresent();

            // Start processing next
            new DataTrainQuestion(this, m_section, m_settings_bias, m_next.Recent());

            // Immediate avilability
            return true;

        } else {

            // Still processing
            m_immediate = true;
            return false;

        }
    }

    // Events
    @Override public void ResultNext(DataTrainQuestion question)
    {
        // Queue or present immediately
        if (!m_immediate) {

            // Queue
            m_question.add(question);

        } else {

            // Set as not immediate anymore
            m_immediate = false;

            // Push to UI
            m_next = question;
            m_event.UiPresent();

            // Start processing next
            new DataTrainQuestion(this, m_section, m_settings_bias, m_next.Recent());

        }
    }

    @Override public void ResultLast(DataTrainQuestion question)
    {
        m_last = question;
        m_event.UiPresent();
    }
}
