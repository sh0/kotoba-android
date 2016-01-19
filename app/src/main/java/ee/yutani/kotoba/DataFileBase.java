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
import java.util.ArrayList;
import java.util.TreeMap;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

// Data file words
public class DataFileBase
{
    // Cache settings
    private static final int CACHE_INITIAL_ROUNDS = 1;
    private static final int CACHE_MAX_ROUNDS = 6;

    // Base info data structure
    public static class BaseInfo implements Comparable<BaseInfo>
    {
        // Data
        private int m_id;
        private String m_text;
        private float m_match;
        private int[] m_wref;
        private byte[] m_wrank;

        // Constructor
        protected BaseInfo(int id, String text, float match, int[] wref, byte[] wrank)
        {
            m_id = id;
            m_text = text;
            m_match = match;
            m_wref = wref;
            m_wrank = wrank;
        }

        // Gets
        public int Id() { return m_id; }
        public String Text() { return m_text; }
        public float Match() { return m_match; }
        public int Weight() { return m_wref.length; }
        public int[] Wref() { return m_wref; }
        public byte[] Wrank() { return m_wrank; }

        // Comparator
        @Override public int compareTo(BaseInfo another)
        {
            if (m_match == 1.0f && another.m_match == 1.0f) {
                if (m_wref.length == another.m_wref.length)
                    return m_text.compareTo(another.m_text);
                return ((m_wref.length < another.m_wref.length) ? -1 : 1);
            } else if (m_match == 1.0f) {
                return -1;
            } else if (another.m_match == 1.0f) {
                return 1;
            } else if (m_match > 1.0f && another.m_match > 1.0f) {
                if (m_wref.length == another.m_wref.length)
                    return m_text.compareTo(another.m_text);
                return ((m_wref.length < another.m_wref.length) ? -1 : 1);
            } else if (m_match > 1.0f) {
                return -1;
            } else if (another.m_match > 1.0f) {
                return 1;
            } else if (m_match < 1.0f && another.m_match < 1.0f) {
                if (m_match == another.m_match) {
                    if (m_wref.length == another.m_wref.length)
                        return m_text.compareTo(another.m_text);
                    return ((m_wref.length < another.m_wref.length) ? -1 : 1);
                }
                return ((m_match > another.m_match) ? -1 : 1);
            }
            return 0;
        }
    }

    // File
    private DataStream m_ds = null;

    // Offsets
    private long m_offset_index = 0;
    private long m_offset_data = 0;

    // Entries
    private int m_entry_num = 0;

    // Cache
    private TreeMap<Integer, String> m_cache = new TreeMap<Integer, String>();

    // Constructor
    public DataFileBase(Context context, String fn)
    {
        // Open file
        try {
            // Raw stream
            InputStream fs = context.getAssets().open("kotoba-base_" + fn + ".kdb", AssetManager.ACCESS_RANDOM);
            m_ds = new DataStream(fs, false);
        } catch (IOException ex) {
            Log.e("DataFileBase::DataFileBase", "Asset opening: IO error", ex);
            return;
        }

        // Inital loading
        try {
            // Get number of entries
            m_entry_num = m_ds.ReadInt();

            // Offsets
            m_offset_index = 4;
            m_offset_data = m_offset_index + (4 * (m_entry_num + 1));

            // Generate cache
            CacheGenerate(0, m_entry_num, CACHE_INITIAL_ROUNDS);

        } catch (IOException ex) {
            Log.e("DataFileBase::DataFileBase", "Asset reading: IO error", ex);
            return;
        }
    }

    // Find
    public synchronized ArrayList<BaseInfo> Find(String text, int num_matches)
    {
        try {
            // Find
            int[] ret = new int[]{ -1, -1 };
            FindGeneral(ret, text);

            // Cap equal matches
            int num_left = num_matches;
            if (ret[1] - ret[0] > num_matches)
                ret[1] = ret[0] + num_matches;
            num_left -= ret[1] - ret[0];

            // Pre and post matches
            int ret_lb = ret[0] - (num_left / 2);
            if (ret_lb < 0)
                ret_lb = 0;
            int ret_ub = ret_lb + num_matches;
            if (ret_ub > m_entry_num) {
                ret_ub = m_entry_num;
                ret_lb = ret_ub - num_matches;
            }

            // Read offsets
            m_ds.Seek(m_offset_index + (4 * ret_lb));
            int[] offsets = m_ds.ReadIntArray(num_matches + 1);

            // Read data
            m_ds.Seek(m_offset_data + offsets[0]);
            byte[] data = new byte[offsets[num_matches] - offsets[0]];
            m_ds.Read(data);

            // Data buffer
            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // Entries
            ArrayList<BaseInfo> info = new ArrayList<BaseInfo>();

            // Equal matches
            for (int i = 0; i < ret[1] - ret[0]; i++) {
                int id = ret[0] + i - ret_lb;
                buf.position(offsets[id] - offsets[0]);
                BaseInfo entry = InfoEntry(ret[0] + i, buf, text);
                if (entry != null)
                    info.add(entry);
            }

            // Post matches
            for (int i = 0; i < ret_ub - ret[1]; i++) {
                int id = ret[1] + i - ret_lb;
                buf.position(offsets[id] - offsets[0]);
                BaseInfo entry = InfoEntry(ret_ub + i, buf, text);
                if (entry != null)
                    info.add(entry);
            }

            // Pre matches
            for (int i = 0; i < ret[0] - ret_lb; i++) {
                int id = i;
                buf.position(offsets[id] - offsets[0]);
                BaseInfo entry = InfoEntry(ret_lb + i, buf, text);
                if (entry != null)
                    info.add(entry);
            }

            // Result
            return info;

        } catch (IOException ex) {
            Log.e("DataFileBase::Find", "IO error", ex);
            return null;
        }
    }

    // Entry reading
    private BaseInfo InfoEntry(int id, ByteBuffer buf, String text_find) throws IOException
    {
        // String
        short text_len = buf.getShort();
        byte[] text_arr = new byte[text_len];
        buf.get(text_arr);
        String text_cmp = new String(text_arr, "UTF-8");

        // Wref
        short wref_len = buf.getShort();
        byte[] wrank = new byte[wref_len];
        int[] wref = new int[wref_len];
        for (int i = 0; i < wref_len; i++) {
            int obj = buf.getInt();
            wrank[i] = (byte)(obj >> 28);
            wref[i] = 0x0fffffff & obj;
        }

        // Matching factor
        float match = 1.0f;
        int cmp = TextCompare(text_find, text_cmp);
        if (cmp == 0) {
            if (text_find.length() < text_cmp.length())
                match = (float)text_cmp.length() / (float)text_find.length();
        } else {
            int num = 0;
            for (num = 0; num < text_find.length() && num < text_cmp.length(); num++) {
                if (text_find.charAt(num) != text_cmp.charAt(num))
                    break;
            }
            if (num == 0)
                return null;
            match = (float)num / (float)text_find.length();
        }

        // Create
        return new BaseInfo(id, text_cmp, match, wref, wrank);
    }

    // Binary tree functions
    private int BinaryDivide(int arr_s, int arr_e)
    {
        return arr_s + ((arr_e - arr_s) / 2);
    }

    private String BinaryLookup(int id) throws IOException
    {
        // Check
        assert(id >= 0 && id < m_entry_num);

        // Read offsets
        m_ds.Seek(m_offset_index + (4 * id));
        int[] offsets = m_ds.ReadIntArray(2);

        // Read data
        m_ds.Seek(m_offset_data + offsets[0]);
        byte[] data = new byte[offsets[1] - offsets[0]];
        m_ds.Read(data);

        // Data buffer
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // String
        short text_len = buf.getShort();
        byte[] text_arr = new byte[text_len];
        buf.get(text_arr);
        return new String(text_arr, "UTF-8");
    }

    // Cache
    private void CacheGenerate(int arr_s, int arr_e, int level) throws IOException
    {
        // Lookup
        int arr_m = BinaryDivide(arr_s, arr_e);
        m_cache.put(arr_m, BinaryLookup(arr_m));

        // Rescurse
        level--;
        if (level > 0) {
            CacheGenerate(arr_s, arr_m, level);
            CacheGenerate(arr_m, arr_e, level);
        }
    }

    private String CacheLookup(int arr_m, int round) throws IOException
    {
        // Lookup
        if (m_cache.containsKey(arr_m)) {
            return m_cache.get(arr_m);
        } else {
            String str = BinaryLookup(arr_m);
            if (round < CACHE_MAX_ROUNDS)
                m_cache.put(arr_m, str);
            return str;
        }
    }

    // Finds
    private void FindGeneral(int[] ret, String text) throws IOException
    {
        // Rounds
        int round = 0;

        // Array bounds
        int arr_s = 0;
        int arr_e = m_entry_num;

        // Loop
        while (arr_e > arr_s) {
            round++;
            int arr_m = BinaryDivide(arr_s, arr_e);
            String cmp_tmp = CacheLookup(arr_m, round);
            int cmp = TextCompare(text, cmp_tmp);
            if (cmp < 0) {
                arr_e = arr_m;
            } else if (cmp > 0) {
                arr_s = arr_m + 1;
            } else {
                // Split the search
                ret[0] = FindLower(arr_s, arr_m, text, round);
                ret[1] = FindUpper(arr_m, arr_e, text, round);
                return;
            }
        }

        // Results
        ret[0] = arr_s;
        ret[1] = arr_e;
    }

    private int FindLower(int arr_s, int arr_e, String text, int round) throws IOException
    {
        while (arr_e > arr_s) {
            round++;
            int arr_m = BinaryDivide(arr_s, arr_e);
            String cmp_tmp = CacheLookup(arr_m, round);
            if (TextCompare(text, cmp_tmp) <= 0) {
                arr_e = arr_m;
            } else {
                arr_s = arr_m + 1;
            }
        }
        return arr_s;
    }

    private int FindUpper(int arr_s, int arr_e, String text, int round) throws IOException
    {
        while (arr_e > arr_s) {
            round++;
            int arr_m = BinaryDivide(arr_s, arr_e);
            String cmp_tmp = CacheLookup(arr_m, round);
            if (TextCompare(text, cmp_tmp) > 0) {
                arr_s = arr_m + 1;
            } else {
                arr_e = arr_m;
            }
        }
        return arr_s;
    }

    // Text
    private int TextCompare(String text, String cmp)
    {
        if (text.length() > cmp.length())
            text = text.substring(0, cmp.length());
        else if (text.length() < cmp.length())
            cmp = cmp.substring(0, text.length());
        return text.compareTo(cmp);
    }
}
