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
import java.util.Arrays;

import ee.yutani.kotoba.DataFileCategory.CategoryInfo;
import ee.yutani.kotoba.DataFileWord.WordInfo;
import ee.yutani.kotoba.DataUserWord.WordUser;

// Word training class
public class DataTrainWord
{
    // Constants
    public static final int TRAIN_LEVELS = 5;

    // Id structure
    public class IdTrain
    {
        // Ids
        private int m_id[] = null;

        // Constructors
        public IdTrain() { m_id = new int[]{}; }
        public IdTrain(int[] id) { m_id = id; }
        public IdTrain(IdTrain id, int append)
        {
            m_id = new int[id.m_id.length + 1];
            for (int i = 0; i < id.m_id.length; i++)
                m_id[i] = id.m_id[i];
            m_id[m_id.length - 1] = append;
        }

        // Append constructor
        public IdTrain Append(int append)
        {
            return new IdTrain(this, append);
        }

        // Serialization
        public int[] Serialize() { return m_id; }

        // Section
        public SectionTrain Section()
        {
            // Root
            SectionTrain section = m_root;

            // Loop
            for (int i = 0; i < m_id.length; i++) {
                if (m_id[i] < 0 || m_id[i] >= section.SectionLength())
                    return section;
                section = section.SectionEntry(m_id[i]);
            }

            // Return last selection
            return section;
        }
    }

    // Section interface
    public interface SectionTrain
    {
        // Info
        public IdTrain Id();
        public String Name();

        // Words
        public int WordLength();
        public WordTrain WordEntry(int id);
        public WordTrain[] WordArray(int offset, int size);
        public byte[] WordValues();

        // Statistics
        public int[] Statistics();

        // Reset
        public void Reset();

        // Sections
        public int SectionLength();
        public SectionTrain SectionEntry(int id);
        public SectionTrain[] SectionArray();
    }

    // Partition structure
    public class PartitionTrain implements SectionTrain
    {
        // Parent and id
        private CategoryTrain m_parent = null;
        private int m_id;

        // Statistics
        private int[] m_stats = new int[TRAIN_LEVELS];

        // Offset and size
        private int m_offset;
        private int m_size;

        // Ids
        private int m_id_start;
        private int m_id_end;

        // Constructor
        protected PartitionTrain(CategoryTrain parent, int id, byte[] values, int offset, int size, int id_start, int id_end)
        {
            // Parent and id
            m_parent = parent;
            m_id = id;

            // Offset and size
            m_offset = offset;
            m_size = size;

            // Ids
            m_id_start = id_start;
            m_id_end = id_end;

            // Reset statistics
            for (int i = 0; i < m_stats.length; i++)
                m_stats[i] = 0;

            // Generate statistics
            for (int i = 0; i < m_size; i++) {
                int level = (values[i] & 0x7f);
                if (level < m_stats.length)
                    m_stats[level]++;
                else
                    m_stats[m_stats.length - 1]++;
            }
        }

        // Offset and size
        public int Offset() { return m_offset; }
        public int Size() { return m_size; }

        // Ids
        public int IdStart() { return m_id_start; }
        public int IdEnd() { return m_id_end; }

        // Words
        @Override public int WordLength() { return m_size; }
        @Override public WordTrain WordEntry(int id) { return m_parent.WordEntry(m_offset + id); }
        @Override public WordTrain[] WordArray(int offset, int size) { return m_parent.WordArray(m_offset + offset, size); }
        @Override public byte[] WordValues()
        {
            return Arrays.copyOfRange(m_parent.WordValues(), m_offset, m_offset + m_size);
        }

        // Info
        @Override public IdTrain Id() { return new IdTrain(m_parent.Id(), m_id); }
        @Override public String Name() { return Integer.toString(m_offset + 1) + " - " + Integer.toString(m_offset + m_size + 1); }

        // Sections
        @Override public int SectionLength() { return 0; }
        @Override public SectionTrain SectionEntry(int id) { return null; }
        @Override public SectionTrain[] SectionArray() { return null; }

        // Statistics
        @Override public int[] Statistics() { return m_stats; }

        // Reset
        @Override public void Reset()
        {
            m_parent.Reset(m_offset, m_size);
        }

        public void Refresh(byte[] values)
        {
            // Reset statistics
            for (int i = 0; i < m_stats.length; i++)
                m_stats[i] = 0;

            // Generate statistics
            for (int i = 0; i < m_size; i++) {
                int level = (values[i] & 0x7f);
                if (level < m_stats.length)
                    m_stats[level]++;
                else
                    m_stats[m_stats.length - 1]++;
            }
        }

        // Updates
        public void StatisticsScore(int score_old, int score_new)
        {
            // Check
            if (score_old == score_new)
                return;

            // Transition
            if (score_old > m_stats.length)
                score_old = m_stats.length - 1;
            m_stats[score_old]--;
            if (score_new > m_stats.length)
                score_new = m_stats.length - 1;
            m_stats[score_new]++;
        }
    }

    // Category structure
    public class CategoryTrain implements SectionTrain
    {
        // Parent and id
        private SectionTrain m_parent = null;
        private int m_id = -1;

        // Category
        private DataFileCategory.CategoryInfo m_info;

        // Values
        private byte[] m_value = null;

        // Statistics
        private int[] m_stats = new int[TRAIN_LEVELS];

        // Partitions
        private ArrayList<PartitionTrain> m_partition = new ArrayList<PartitionTrain>();

        // Constructor
        protected CategoryTrain(SectionTrain parent, int id, CategoryInfo info)
        {
            // Parent and id
            m_parent = parent;
            m_id = id;

            // Category
            m_info = info;

            // Partition
            Partition();
        }

        // Partitioning
        public void Partition()
        {
            // Load word values
            int[] words = m_info.WordsArray();
            m_value = new byte[words.length];
            for (int i = 0; i < words.length; i++)
                m_value[i] = m_user_word.TrainQuick(words[i]);

            // Partitions
            m_partition.clear();
            for (int i = 0; i < m_value.length; i += m_settings_partition_size) {
                // Size
                int size = m_settings_partition_size;
                if (size > m_value.length - i)
                    size = m_value.length - i;

                // Create
                PartitionTrain partition = new PartitionTrain(
                    this, m_partition.size(),
                    Arrays.copyOfRange(m_value, i, i + size),
                    i, size, words[i], words[i + size - 1]
                );
                m_partition.add(partition);
            }

            // Reset statistics
            for (int i = 0; i < m_stats.length; i++)
                m_stats[i] = 0;

            // Statistics generation
            for (PartitionTrain partition : m_partition) {
                int[] stats = partition.Statistics();
                for (int i = 0; i < stats.length; i++)
                    m_stats[i] += stats[i];
            }
        }

        // Info
        @Override public IdTrain Id() { return new IdTrain(m_parent.Id(), m_id); }
        @Override public String Name() { return m_info.Name(); }

        // Words
        @Override public int WordLength() { return m_value.length; }
        @Override public WordTrain WordEntry(int id)
        {
            int[] rid = m_info.WordsArray(id, 1);
            return new WordTrain(rid[0]);
        }
        @Override public WordTrain[] WordArray(int offset, int size)
        {
            // Words in category
            int[] rid = m_info.WordsArray(offset, size);

            // Generate array
            WordTrain[] list = new WordTrain[size];
            for (int i = 0; i < size; i++)
                list[i] = new WordTrain(rid[i]);
            return list;
        }
        @Override public byte[] WordValues()
        {
            return m_value;
        }

        // Sections
        @Override public int SectionLength() { return m_partition.size(); }
        @Override public SectionTrain SectionEntry(int id) { return m_partition.get(id); }
        @Override public SectionTrain[] SectionArray()
        {
            SectionTrain[] list = new SectionTrain[m_partition.size()];
            for (int i = 0; i < m_partition.size(); i++)
                list[i] = m_partition.get(i);
            return list;
        }

        // Statistics
        @Override public int[] Statistics() { return m_stats; }

        // Reset
        @Override public void Reset()
        {
            // Database
            int[] rid = m_info.WordsArray();
            m_user_word.TrainReset(rid);

            // Refresh data
            m_root.Refresh();
        }

        public void Reset(int offset, int size)
        {
            // Database
            int[] rid = m_info.WordsArray(offset, size);
            m_user_word.TrainReset(rid);

            // Refresh data
            m_root.Refresh();
        }

        public void Refresh()
        {
            // Reload word values
            int[] words = m_info.WordsArray();
            for (int i = 0; i < words.length; i++)
                m_value[i] = m_user_word.TrainQuick(words[i]);

            // Partition
            for (PartitionTrain partition : m_partition)
                partition.Refresh(Arrays.copyOfRange(m_value, partition.m_offset, partition.m_offset + partition.m_size));

            // Reset statistics
            for (int i = 0; i < m_stats.length; i++)
                m_stats[i] = 0;

            // Statistics generation
            for (PartitionTrain partition : m_partition) {
                int[] stats = partition.Statistics();
                for (int i = 0; i < stats.length; i++)
                    m_stats[i] += stats[i];
            }
        }

        // Updates
        public void StatisticsScore(int id, int score_old, int score_new)
        {
            // Bounds
            if (score_old < 0)
                score_old = 0;
            if (score_old >= m_stats.length)
                score_old = m_stats.length - 1;
            if (score_new < 0)
                score_new = 0;
            if (score_new >= m_stats.length)
                score_new = m_stats.length - 1;

            // Check
            if (score_old == score_new)
                return;

            // Value
            int[] rid = m_info.WordsArray();
            for (int i = 0; i < rid.length; i++) {
                if (rid[i] == id) {
                    m_value[i] = (byte)((m_value[i] & 0x80) | score_new);
                    break;
                }
            }

            // Category
            m_stats[score_old]--;
            m_stats[score_new]++;

            // Partition
            for (PartitionTrain partition : m_partition) {
                if (partition.IdStart() <= id && partition.IdEnd() >= id) {
                    partition.StatisticsScore(score_old, score_new);
                    break;
                }
            }
        }
    }

    public class RootTrain implements SectionTrain
    {
        // Categories
        private ArrayList<CategoryTrain> m_category = new ArrayList<CategoryTrain>();

        // Constructor
        protected RootTrain()
        {
            // Categories
            ArrayList<CategoryInfo> list = m_file_category.InfoList();
            for (int i = 0; i < list.size(); i++)
                m_category.add(new CategoryTrain(this, i, list.get(i)));
        }

        // Partition
        public void Partition()
        {
            // Force repartitioning
            for (CategoryTrain category : m_category)
                category.Partition();
        }

        // Info
        @Override public IdTrain Id() { return new IdTrain(); }
        @Override public String Name() { return "All words"; }

        // Words
        @Override public int WordLength() { return 0; }
        @Override public WordTrain WordEntry(int id) { return null; }
        @Override public WordTrain[] WordArray(int offset, int size) { return null; }
        @Override public byte[] WordValues() { return null; }

        // Sections
        @Override public int SectionLength() { return m_category.size(); }
        @Override public SectionTrain SectionEntry(int id) { return m_category.get(id); }
        @Override public SectionTrain[] SectionArray() {
            SectionTrain[] list = new SectionTrain[m_category.size()];
            for (int i = 0; i < m_category.size(); i++)
                list[i] = m_category.get(i);
            return list;
        }

        // Statistics
        @Override public int[] Statistics()
        {
            int[] stats = new int[TRAIN_LEVELS];
            for (int i = 0; i < stats.length; i++)
                stats[i] = 0;
            for (CategoryTrain category : m_category) {
                int[] cstats = category.Statistics();
                for (int i = 0; i < cstats.length; i++)
                    stats[i] += cstats[i];
            }
            return stats;
        }

        // Reset
        @Override public void Reset()
        {
            for (CategoryTrain category : m_category)
                category.Reset();
        }

        // Refresh
        public void Refresh()
        {
            for (CategoryTrain category : m_category)
                category.Refresh();
        }

        // Categories
        public int CategoryLength() { return m_category.size(); }
        public CategoryTrain CategoryEntry(int id) { return m_category.get(id); }
    }

    // Word structure
    public class WordTrain
    {
        // Word
        private WordInfo m_info;
        private WordUser m_user;

        // Category
        private CategoryTrain[] m_cref;

        // Constructors
        protected WordTrain(int id)
        {
            // Load
            m_info = m_file_word.InfoEntry(id);
            m_user = m_user_word.TrainEntry(id);

            // Crefs
            CrefGenerate();
        }

        protected WordTrain(WordInfo info, WordUser user)
        {
            // Copy
            m_info = info;
            m_user = user;

            // Crefs
            CrefGenerate();
        }

        // Crefs
        private void CrefGenerate()
        {
            // Categories
            int[] cref = m_info.CrefArray();
            m_cref = new CategoryTrain[cref.length];
            for (int i = 0; i < cref.length; i++) {
                assert(m_root.CategoryLength() > cref[i] && cref[i] >= 0);
                m_cref[i] = m_root.CategoryEntry(cref[i]);
            }
        }

        // Info
        public WordInfo Info() { return m_info; }

        // Score
        public int Score() { return m_user.Score(); }
        public void SetScore(int score_new)
        {
            // Bounds
            if (score_new < 0)
                score_new = 0;
            if (score_new >= TRAIN_LEVELS)
                score_new = TRAIN_LEVELS - 1;

            // Check
            int score_old = m_user.Score();
            if (score_old == score_new)
                return;

            // Change
            m_user.SetScore(score_new);

            // Category statistics
            for (int i = 0; i < m_cref.length; i++)
                m_cref[i].StatisticsScore(m_info.Id(), score_old, score_new);
        }
    }

    // Data
    private DataFileWord m_file_word = null;
    private DataFileCategory m_file_category = null;
    private DataUserWord m_user_word = null;

    // Settings
    private int m_settings_partition_size = 50;

    // Root
    private RootTrain m_root = null;

    // Constructor
    public DataTrainWord(DataFileWord file_word, DataFileCategory file_category, DataUserWord user_word)
    {
        // Data
        m_file_word = file_word;
        m_file_category = file_category;
        m_user_word = user_word;

        // Root
        m_root = new RootTrain();
    }

    // Id functions
    public IdTrain Id() { return new IdTrain(); }
    public IdTrain Id(int[] id) { return new IdTrain(id); }
}
