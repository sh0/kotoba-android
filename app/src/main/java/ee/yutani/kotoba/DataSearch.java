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
import java.util.Collections;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;

import ee.yutani.kotoba.DataFileBase.BaseInfo;
import ee.yutani.kotoba.DataFileWord.WordInfo;

// Word searching class
public class DataSearch
{
    // Constants
    private static final int NUM_MATCHES = 30;

    public class SearchWord implements Comparable<SearchWord>
    {
        // Data
        private int m_id;
        private long m_weight;
        private int m_level;

        // Constructor
        public SearchWord(int id, long weight, float match, int rank)
        {
            // Id
            m_id = id;

            // Weight
            m_weight = 100; //weight;
            if (match == 1.0f) {
                m_weight *= 1000 * 1000;
            } else if (match > 1.0f) {
                m_weight *= 1000.0 / match;
            } else {
                m_weight *= match;
            }

            // Rank
            rank++;
            if (rank < 1)
                rank = 1;
            m_weight /= rank;

            // Level
            m_level = 0;
        }

        // Append
        public void Append(long weight, float match, int rank)
        {
            // Weight
            if (match == 1.0f) {
                weight *= 1000 * 1000;
            } else if (match > 1.0f) {
                weight *= 1000.0 / match;
            } else {
                weight *= match;
            }

            // Rank
            rank++;
            if (rank < 1)
                rank = 1;
            weight /= rank;

            // Add modified weight
            m_weight += weight;

            // Level
            m_level++;
        }

        // Rescale
        public void Rescale(long weight, float match, int rank)
        {

        }

        // Gets
        public WordInfo Word()
        {
            return m_word.InfoEntry(m_id);
        }

        // Comparison
        @Override public int compareTo(SearchWord another)
        {
            if (m_level == another.m_level) {
                if (m_weight == another.m_weight) {
                    if (m_id == another.m_id)
                        return 0;
                    return ((m_id < another.m_id) ? -1 : 1);
                }
                return ((m_weight > another.m_weight) ? -1 : 1);
            }
            return ((m_level > another.m_level) ? -1 : 1);
        }
    }

    // Bases
    private DataFileBase m_base_e = null;
    private DataFileBase m_base_f = null;
    private DataFileBase m_base_k = null;

    // Words
    private DataFileWord m_word = null;

    // Constructor
    public DataSearch(DataFileBase base_e, DataFileBase base_f, DataFileBase base_k, DataFileWord word)
    {
        // Bases
        m_base_e = base_e;
        m_base_f = base_f;
        m_base_k = base_k;

        // Words
        m_word = word;
    }

    // Search
    public ArrayList<SearchWord> Search(String query)
    {
        // Result map
        TreeMap<Integer, SearchWord> map = new TreeMap<Integer, SearchWord>();

        // Split query
        String[] split = query.split("\\s+");
        for (String entry : split)
            SearchEntry(map, entry);

        ArrayList<SearchWord> list = new ArrayList<SearchWord>(map.values());
        Collections.sort(list);
        return list;
    }

    // Search
    private void SearchEntry(TreeMap<Integer, SearchWord> map, String query)
    {
        // Temporary set
        TreeSet<Integer> map_check = new TreeSet<Integer>();

        // Latin check
        boolean is_latin = query.matches("\\p{Latin}+");
        if (is_latin) {
            // English
            query = query.toLowerCase(Locale.ENGLISH);
            ArrayList<BaseInfo> elist = m_base_e.Find(query, NUM_MATCHES);
            for (BaseInfo info : elist)
                SearchInsert(map, map_check, info);
        } else {
            // Japanese
            ArrayList<BaseInfo> flist = m_base_f.Find(query, NUM_MATCHES);
            for (BaseInfo info : flist)
                SearchInsert(map, map_check, info);
            ArrayList<BaseInfo> klist = m_base_k.Find(query, NUM_MATCHES);
            for (BaseInfo info : klist)
                SearchInsert(map, map_check, info);
        }
    }

    private void SearchInsert(TreeMap<Integer, SearchWord> map, TreeSet<Integer> map_check, BaseInfo info)
    {
        int[] wref = info.Wref();
        byte[] wrank = info.Wrank();
        for (int i = 0; i < wref.length; i++) {
            if (!map_check.contains(wref[i])) {
                map_check.add(wref[i]);
                if (map.containsKey(wref[i])) {
                    SearchWord word = map.get(wref[i]);
                    word.Append(info.Weight(), info.Match(), wrank[i]);
                } else {
                    map.put(wref[i], new SearchWord(wref[i], info.Weight(), info.Match(), wrank[i]));
                }
            } else {
                if (map.containsKey(wref[i])) {
                    SearchWord word = map.get(wref[i]);
                    word.Rescale(info.Weight(), info.Match(), wrank[i]);
                }
            }
        }
    }

}
