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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

// Category file
public class DataFileCategory
{
    // Category class
    public class CategoryInfo
    {
        // Info
        private int m_id;
        private String m_name;
        private int m_words_num;
        private long m_words_offset;

        // Constructor
        protected CategoryInfo(int id, String name, int words_num, long words_offset)
        {
            m_id = id;
            m_name = name;
            m_words_num = words_num;
            m_words_offset = words_offset;
        }

        // Gets
        public int Id() { return m_id; }
        public String Name() { return m_name; }
        public int WordsLength() { return m_words_num; }
        public int[] WordsArray() { return WordsArray(0, m_words_num); }
        public synchronized int[] WordsArray(int offset, int num)
        {
            // Check
            assert(offset >= 0 && offset < m_words_num);
            assert(offset + num <= m_words_num);

            // IO operations
            try {
                m_ds.Seek(m_words_offset + (4 * offset));
                return m_ds.ReadIntArray(num);
            } catch (IOException ex) {
                Log.e("DataFileCategory::CategoryInfo::WordsArray", "IO error", ex);
                return null;
            }
        }
    }

    // File
    private DataStream m_ds = null;

    // Offsets
    private long m_offset_index = 0;
    private long m_offset_data = 0;

    // Entries
    private int m_entry_num = 0;
    private ArrayList<CategoryInfo> m_entry_info = new ArrayList<CategoryInfo>();

    // Constructor
    public DataFileCategory(Context context)
    {
        // Open file
        try {
            // Raw stream
            InputStream fs = context.getAssets().open("kotoba-category.kdb", AssetManager.ACCESS_RANDOM);
            m_ds = new DataStream(fs, true);
        } catch (IOException ex) {
            Log.e("DataFileCategory::DataFileCategory", "Asset opening: IO error", ex);
            return;
        }

        // Inital loading
        try {
            // Get number of entries
            m_entry_num = m_ds.ReadInt();

            // Offsets
            m_offset_index = 4;
            m_offset_data = m_offset_index + (4 * (m_entry_num + 1));

            // Read full index
            int[] index_off = m_ds.ReadIntArray(m_entry_num + 1);

            // Read basic entry info
            for (int i = 0; i < m_entry_num; i++) {
                // Seek
                m_ds.Seek(m_offset_data + index_off[i]);

                // Read data
                String name = m_ds.ReadString();
                int words_num = m_ds.ReadInt();

                // Entry
                CategoryInfo info = new CategoryInfo(i, name, words_num, m_ds.Offset());
                m_entry_info.add(info);
            }

        } catch (IOException ex) {
            Log.e("DataFileCategory::DataFileCategory", "Asset reading: IO error", ex);
            return;
        }
    }

    // Gets
    public ArrayList<CategoryInfo> InfoList() { return m_entry_info; }
}
