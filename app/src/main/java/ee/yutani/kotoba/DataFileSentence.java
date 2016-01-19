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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

// Data file words
public class DataFileSentence
{
    // Sentence class
    public static class SentenceInfo
    {
        // Data
        private int m_id;
        private String m_text_jp;
        private String m_text_en;

        // Constructor
        protected SentenceInfo(int id, String text_jp, String text_en)
        {
            m_id = id;
            m_text_jp = text_jp;
            m_text_en = text_en;
        }

        // Gets
        public int Id() { return m_id; }
        public String TextJp() { return m_text_jp; }
        public String TextEn() { return m_text_en; }
    }

    // File
    private DataStream m_ds = null;

    // Offsets
    private long m_offset_index = 0;
    private long m_offset_data = 0;

    // Entries
    private int m_entry_num = 0;

    // Constructor
    public DataFileSentence(Context context)
    {
        // Open file
        try {
            // Raw stream
            InputStream fs = context.getAssets().open("kotoba-sentence.kdb", AssetManager.ACCESS_RANDOM);
            m_ds = new DataStream(fs, false);
        } catch (IOException ex) {
            Log.e("DataFileSentence::DataFileSentence", "Asset opening: IO error", ex);
            return;
        }

        // Inital loading
        try {
            // Get number of entries
            m_entry_num = m_ds.ReadInt();

            // Offsets
            m_offset_index = 4;
            m_offset_data = m_offset_index + (4 * (m_entry_num + 1));

        } catch (IOException ex) {
            Log.e("DataFileSentence::DataFileSentence", "Asset reading: IO error", ex);
            return;
        }
    }

    // Gets
    public int InfoLength() { return m_entry_num; }
    public synchronized SentenceInfo InfoEntry(int id)
    {
        // Checks
        assert(id >= 0 && id < m_entry_num);

        // Reading
        try {
            // Read offsets
            m_ds.Seek(m_offset_index + (4 * id));
            int[] offsets = m_ds.ReadIntArray(2);

            // Read data
            m_ds.Seek(m_offset_data + offsets[0]);
            byte[] data = new byte[offsets[1] - offsets[0]];
            m_ds.Read(data);

            // Parse data
            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // Japanese
            short text_jp_len = buf.getShort();
            byte[] text_jp_arr = new byte[text_jp_len];
            buf.get(text_jp_arr);
            String text_jp = new String(text_jp_arr, "UTF-8");

            // English
            short text_en_len = buf.getShort();
            byte[] text_en_arr = new byte[text_en_len];
            buf.get(text_en_arr);
            String text_en = new String(text_en_arr, "UTF-8");

            // Entry
            return new SentenceInfo(id, text_jp, text_en);

        } catch (IOException ex) {
            Log.e("DataFileSentence::InfoEntry", "IO error", ex);
            return null;
        }
    }
}
