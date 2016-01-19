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
public class DataFileWord
{
    // Glossary sense data structure
    public static class Sense
    {
        // Data
        private String[] m_gloss;

        // Constructor
        protected Sense(String[] gloss)
        {
            m_gloss = gloss;
        }

        // Gets
        public int GlossLength() { return m_gloss.length; }
        public String[] GlossArray() { return m_gloss; }
    }

    // Sentence reference data structure
    public static class Sref
    {
        // Data
        private int m_id;
        private short m_start;
        private short m_end;

        // Constructor
        protected Sref(int id, short start, short end)
        {
            m_id = id;
            m_start = start;
            m_end = end;
        }

        // Gets
        public int Id() { return m_id; }
        public short Start() { return m_start; }
        public short End() { return m_end; }
    }

    // Word class
    public static class WordInfo
    {
        // Data
        private int m_id;
        private String[] m_text_k;
        private String[] m_text_r;
        private Sense[] m_sense;
        private int[] m_cref;
        private Sref[] m_sref;

        // Constructor
        protected WordInfo(int id, String[] text_k, String[] text_r, Sense[] sense, int[] cref, Sref[] sref)
        {
            m_id = id;
            m_text_k = text_k;
            m_text_r = text_r;
            m_sense = sense;
            m_cref = cref;
            m_sref = sref;
        }

        // Gets
        public int Id() { return m_id; }
        public String[] TextK() { return m_text_k; }
        public String[] TextR() { return m_text_r; }
        public int SenseLength() { return m_sense.length; }
        public Sense[] SenseArray() { return m_sense; }
        public int CrefLength() { return m_cref.length; }
        public int[] CrefArray() { return m_cref; }
        public int SrefLength() { return m_sref.length; }
        public Sref[] SrefArray() { return m_sref; }
    }

    // File
    private DataStream m_ds = null;

    // Offsets
    private long m_offset_index = 0;
    private long m_offset_data = 0;
    private long m_offset_ident = 0;

    // Index
    private int[] m_index = null;

    // Entries
    private int m_entry_num = 0;

    // Constructor
    public DataFileWord(Context context)
    {
        // Open file
        try {
            // Raw stream
            InputStream fs = context.getAssets().open("kotoba-word.kdb", AssetManager.ACCESS_RANDOM);
            m_ds = new DataStream(fs, false);
        } catch (IOException ex) {
            Log.e("DataFileWord::DataFileWord", "Asset opening: IO error", ex);
            return;
        }

        // Inital loading
        try {
            // Get number of entries
            m_entry_num = m_ds.ReadInt();

            // Offsets
            m_offset_index = 4;
            m_offset_data = m_offset_index + (4 * (m_entry_num + 1));

            // Read index
            m_index = new int[m_entry_num + 1];
            for (int i = 0; i < m_index.length; i++)
                m_index[i] = m_ds.ReadInt();

            // Ident offset
            m_offset_ident = m_offset_data + m_index[m_entry_num];

        } catch (IOException ex) {
            Log.e("DataFileWord::DataFileWord", "Asset reading: IO error", ex);
            return;
        }
    }

    // Info
    public int InfoLength() { return m_entry_num; }
    public synchronized WordInfo InfoEntry(int id)
    {
        // Checks
        assert(id >= 0 && id < m_entry_num);

        // Reading
        try {
            // Read data
            m_ds.Seek(m_offset_data + m_index[id]);
            byte[] data = new byte[m_index[id + 1] - m_index[id]];
            m_ds.Read(data);

            // Parse data
            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // Japanese (k)
            short text_k_len = buf.getShort();
            String[] text_k = new String[text_k_len];
            for (int i = 0; i < text_k_len; i++) {
                short len = buf.getShort();
                byte[] str = new byte[len];
                buf.get(str);
                text_k[i] = new String(str, "UTF-8");
            }

            // Japanese (r)
            short text_r_len = buf.getShort();
            String[] text_r = new String[text_r_len];
            for (int i = 0; i < text_r_len; i++) {
                short len = buf.getShort();
                byte[] str = new byte[len];
                buf.get(str);
                text_r[i] = new String(str, "UTF-8");
            }

            // Sense
            short sense_len = buf.getShort();
            Sense[] sense = new Sense[sense_len];
            for (int i = 0; i < sense_len; i++) {
                // Gloss
                short gloss_len = buf.getShort();
                String[] gloss = new String[gloss_len];
                for (int j = 0; j < gloss_len; j++) {
                    short len = buf.getShort();
                    byte[] str = new byte[len];
                    buf.get(str);
                    gloss[j] = new String(str, "UTF-8");
                }

                // Entry
                sense[i] = new Sense(gloss);
            }

            // Cref
            short cref_len = buf.getShort();
            int[] cref = new int[cref_len];
            for (int i = 0; i < cref_len; i++) {
                cref[i] = buf.getShort();
            }

            // Sref
            short sref_len = buf.getShort();
            Sref[] sref = new Sref[sref_len];
            for (int i = 0; i < sref_len; i++) {
                // Data
                int sref_id = buf.getInt();
                short sref_s = buf.getShort();
                short sref_e = buf.getShort();

                // Entry
                sref[i] = new Sref(sref_id, sref_s, sref_e);
            }

            // Word
            return new WordInfo(id, text_k, text_r, sense, cref, sref);

        } catch (IOException ex) {
            Log.e("DataFileWord::InfoEntry", "IO error", ex);
            return null;
        }
    }

    // Ident
    public int IdentLength() { return m_entry_num; }
    public synchronized int[] IdentArray()
    {
        try {
            // Read
            m_ds.Seek(m_offset_ident);
            byte[] arr = new byte[m_entry_num * 4];
            m_ds.Read(arr);

            // Parse data
            ByteBuffer buf = ByteBuffer.wrap(arr);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // Get ints
            int[] dst = new int[m_entry_num];
            buf.asIntBuffer().get(dst);
            return dst;

        } catch (IOException ex) {
            Log.e("DataFileWord::IdentArray", "IO error", ex);
            return null;
        }
    }
}
