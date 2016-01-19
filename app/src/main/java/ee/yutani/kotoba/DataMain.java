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
import android.os.AsyncTask;

// Main database class
public class DataMain extends AsyncTask<Void, String, Void>
{
    // Callback interface
    public interface Callback
    {
        public void DatabaseLoadingProgress(String text);
        public void DatabaseLoadingFinished();
    }

    // Context and database
    private Context m_context;
    private Callback m_callback;
    private DataDatabase m_database;

    // Files
    private DataFileCategory m_file_category = null;
    private DataFileWord m_file_word = null;
    private DataFileSentence m_file_sentence = null;
    private DataFileBase m_file_base_e = null;
    private DataFileBase m_file_base_f = null;
    private DataFileBase m_file_base_k = null;

    // User
    private DataUserWord m_user_word = null;

    // Train
    private DataTrainWord m_train_word = null;

    // Search
    private DataSearch m_search = null;

    // Constructor
    public DataMain(Context context, Callback callback)
    {
        m_context = context;
        m_callback = callback;
    }

    // Files
    public DataFileWord FileWord() { return m_file_word; }
    public DataFileSentence FileSentence() { return m_file_sentence; }
    public DataFileBase FileBaseE() { return m_file_base_e; }
    public DataFileBase FileBaseF() { return m_file_base_f; }
    public DataFileBase FileBaseK() { return m_file_base_k; }

    // Train
    public DataTrainWord TrainWord() { return m_train_word; }

    // Search
    public DataSearch Search() { return m_search; }

    // Loading
    @Override protected Void doInBackground(Void... params)
    {
        // Database
        this.publishProgress(m_context.getString(R.string.db_loading_database));
        m_database = new DataDatabase(m_context);
        m_database.Open();

        // Files
        this.publishProgress(m_context.getString(R.string.db_loading_files));
        m_file_category = new DataFileCategory(m_context);
        m_file_word = new DataFileWord(m_context);
        m_file_sentence = new DataFileSentence(m_context);
        m_file_base_e = new DataFileBase(m_context, "e");
        m_file_base_f = new DataFileBase(m_context, "f");
        m_file_base_k = new DataFileBase(m_context, "k");

        // User
        this.publishProgress(m_context.getString(R.string.db_loading_user));
        m_user_word = new DataUserWord(m_context, m_file_word, m_database.Database(), m_database.VersionDb(), m_database.VersionSys());

        // Train
        this.publishProgress(m_context.getString(R.string.db_loading_train));
        m_train_word = new DataTrainWord(m_file_word, m_file_category, m_user_word);

        // Search
        m_search = new DataSearch(m_file_base_e, m_file_base_f, m_file_base_k, m_file_word);

        // Successfully opened
        m_database.Success();

        // Return
        return null;
    }

    // Progress
    @Override protected void onProgressUpdate(String... progress)
    {
        m_callback.DatabaseLoadingProgress(progress[0]);
    }

    // Result
    @Override protected void onPostExecute(Void result)
    {
        m_callback.DatabaseLoadingFinished();
    }
}
