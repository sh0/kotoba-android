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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.TreeMap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

// Words user data class
public class DataUserWord
{
    // Block parameters
    private static final int BLOCK_SIZE = 1000;

    // Word training info and functions
    public class WordUser
    {
        // Data
        int m_id;

        // Constructor
        protected WordUser(int id)
        {
            m_id = id;
        }

        // Gets
        public boolean Enabled() { return ((m_train[m_id] & 0x80) != 0); }
        public int Score() { return (m_train[m_id] & 0x7f); }

        // Sets
        public void SetEnabled(boolean enabled)
        {
            m_train[m_id] = (byte)((m_train[m_id] & 0x7f) | (enabled ? 0x80 : 0));
            TrainUpdate(m_id);
        }

        public void SetScore(int score)
        {
            m_train[m_id] = (byte)((m_train[m_id] & 0x80) + (score & 0x7f));
            TrainUpdate(m_id);
        }
    }

    // Databases
    private Context m_context;
    private SQLiteDatabase m_db;
    private DataFileWord m_file_word;

    // Prepared statements
    private SQLiteStatement m_stmt_train_update = null;

    // Values
    private byte[] m_train;

    // Constructor
    public DataUserWord(Context context, DataFileWord file_word, SQLiteDatabase db, int version_db, int version_sys)
    {
        // Databases
        m_context = context;
        m_file_word = file_word;
        m_db = db;

        // Database initialization
        if (version_db == 0) {
            // New installation
            TableCreate();
            version_db = version_sys;
        } else if (version_db < 3) {
            // Old database format
            TableUpdateV3();
            version_db = version_sys;
        } else if (version_db < version_sys) {
            // New database format but old data
            TableUpdateVX();
            version_db = version_sys;
        } else {
            // Read normally
            TableRead();
        }
    }

    // Train entries
    public int TrainLenght()
    {
        return m_train.length;
    }
    public WordUser TrainEntry(int id)
    {
        assert(id >= 0 && id < m_train.length);
        return new WordUser(id);
    }
    public byte TrainQuick(int id)
    {
        return m_train[id];
    }
    public boolean TrainQuickEnabled(int id)
    {
        return ((m_train[id] & 0x80) != 0);
    }
    public int TrainQuickScore(int id)
    {
        return (m_train[id] & 0x7f);
    }

    // Tables
    private void TableCreate()
    {
        // SQL table
        m_db.execSQL(
            "CREATE TABLE IF NOT EXISTS word_train (" +
                "id INT NOT NULL, " +
                "value BLOB NOT NULL, " +
                "ident BLOB NOT NULL" +
            ");"
        );
        m_db.execSQL("DELETE FROM word_train;");

        // Get ident array
        int[] ident = m_file_word.IdentArray();

        // Values
        m_train = new byte[ident.length];
        for (int i = 0; i < m_train.length; i++)
            m_train[i] = (byte) 0x80;
        ByteBuffer value_buf = ByteBuffer.wrap(m_train);

        // SQL transaction
        SQLiteStatement stmt_insert = m_db.compileStatement("INSERT INTO word_train (id, value, ident) VALUES (?, ?, ?)");
        m_db.beginTransaction();
        try {
            // Loop entry blocks
            int num_blocks = ident.length / BLOCK_SIZE + ((ident.length % BLOCK_SIZE == 0) ? 0 : 1);
            for (int i = 0; i < num_blocks; i++) {
                // Calculate number of entries in block
                int num_entry = ident.length - (i * BLOCK_SIZE);
                if (num_entry > BLOCK_SIZE)
                    num_entry = BLOCK_SIZE;

                // Values
                byte[] value_arr = new byte[num_entry];
                value_buf.get(value_arr);

                // Idents
                ByteBuffer buf = ByteBuffer.allocate(num_entry * 4);
                buf.asIntBuffer().put(ident, i * BLOCK_SIZE, num_entry);

                // Sql
                stmt_insert.bindLong(1, i);
                stmt_insert.bindBlob(2, value_arr);
                stmt_insert.bindBlob(3, buf.array());
                stmt_insert.executeInsert();
            }

            // Success
            m_db.setTransactionSuccessful();
        } finally {
            m_db.endTransaction();
        }
    }

    // Word migration class
    private static class TempWordHash implements Comparable<TempWordHash>
    {
        // Data
        private byte[] m_hash;

        // Constructor
        public TempWordHash(byte[] hash)
        {
            m_hash = hash;
        }

        // Equality
        @Override public boolean equals(Object another)
        {
            if (another == null)
                return false;
            if (another == this)
                return true;
            if (!(another instanceof TempWordHash))
                return false;

            TempWordHash aobj = (TempWordHash) another;
            for (int i = 0; i < m_hash.length; i++)
                if (aobj.m_hash[i] == m_hash[i])
                    return false;
            return true;
        }

        // Hashcode
        @Override public int hashCode()
        {
            int hash = 0;
            for (byte v : m_hash)
                hash += v;
            return hash;
        }

        // Compare
        @Override public int compareTo(TempWordHash another)
        {
            int sz = m_hash.length;
            if (another.m_hash.length < sz)
                sz = another.m_hash.length;
            for (int i = 0; i < m_hash.length; i++) {
                if (m_hash[i] < another.m_hash[i])
                    return -1;
                if (m_hash[i] > another.m_hash[i])
                    return 1;
            }
            return 0;
        }
    }

    private void TableUpdateV3()
    {
        try {
            // Migration map
            InputStream fs = new BufferedInputStream(m_context.getAssets().open("kotoba-migrate-v3.kdb"));

            // Get number of entries
            byte[] entry_arr = new byte[4];
            fs.read(entry_arr);
            ByteBuffer entry_buf = ByteBuffer.wrap(entry_arr);
            entry_buf.order(ByteOrder.LITTLE_ENDIAN);
            int entry_num = entry_buf.getInt();

            // Load data
            TreeMap<TempWordHash, Integer> migmap = new TreeMap<TempWordHash, Integer>();
            for (int i = 0; i < entry_num; i++) {
                // Buffers
                byte[] data_arr = new byte[20 + 4];
                fs.read(data_arr);
                ByteBuffer data_buf = ByteBuffer.wrap(data_arr);
                data_buf.order(ByteOrder.LITTLE_ENDIAN);

                // Values
                byte[] hash = new byte[20];
                data_buf.get(hash);
                int ident = data_buf.getInt();

                // Set entry
                migmap.put(new TempWordHash(hash), ident);
            }

            // Fetch old words
            TreeMap<Integer, Byte> word_map = new TreeMap<Integer, Byte>();
            Cursor cur = m_db.rawQuery(
                "SELECT hash, enabled, correct FROM user_word",
                null
            );
            if (cur.moveToFirst()) {
                do {
                    TempWordHash hash = new TempWordHash(cur.getBlob(0));
                    if (migmap.containsKey(hash)) {
                        int ident = migmap.get(hash);
                        byte value = ((cur.getInt(1) == 0) ? 0 : (byte) 0x80);
                        value |= cur.getInt(2) & 0x7f;
                        word_map.put(ident, value);
                    }
                } while (cur.moveToNext());
            }
            cur.close();

            // Get ident array
            int[] ident = m_file_word.IdentArray();

            // Construct value array
            m_train = new byte[ident.length];
            for (int i = 0; i < ident.length; i++) {
                if (word_map.containsKey(ident[i])) {
                    m_train[i] = word_map.get(ident[i]);
                } else {
                    m_train[i] = (byte)0x80;
                }
            }
            ByteBuffer value_buf = ByteBuffer.wrap(m_train);

            // SQL table
            m_db.execSQL(
                "CREATE TABLE IF NOT EXISTS word_train (" +
                    "id INT NOT NULL, " +
                    "value BLOB NOT NULL, " +
                    "ident BLOB NOT NULL" +
                ");"
            );
            m_db.execSQL("DELETE FROM word_train;");

            // Insert new words
            SQLiteStatement stmt_insert = m_db.compileStatement("INSERT INTO word_train (id, value, ident) VALUES (?, ?, ?)");
            m_db.beginTransaction();
            try {
                // Loop entry blocks
                int num_blocks = ident.length / BLOCK_SIZE + ((ident.length % BLOCK_SIZE == 0) ? 0 : 1);
                for (int i = 0; i < num_blocks; i++) {
                    // Calculate number of entries in block
                    int num_entry = ident.length - (i * BLOCK_SIZE);
                    if (num_entry > BLOCK_SIZE)
                        num_entry = BLOCK_SIZE;

                    // Values
                    byte[] value_arr = new byte[num_entry];
                    value_buf.get(value_arr);

                    // Idents
                    ByteBuffer ident_buf = ByteBuffer.allocate(num_entry * 4);
                    ident_buf.asIntBuffer().put(ident, i * BLOCK_SIZE, num_entry);

                    // Sql
                    stmt_insert.bindLong(1, i);
                    stmt_insert.bindBlob(2, value_arr);
                    stmt_insert.bindBlob(3, ident_buf.array());
                    stmt_insert.executeInsert();
                }

                // Drop old table
                m_db.execSQL("DROP TABLE IF EXISTS user_word;");

                // Success
                m_db.setTransactionSuccessful();
            } finally {
                m_db.endTransaction();
            }

        } catch (IOException ex) {
            // Error
            Log.e("DataUserWord::TableUpdateV3", "IO error", ex);
        }
    }

    private void TableUpdateVX()
    {
        // Old data
        TreeMap<Integer, Byte> map = new TreeMap<Integer, Byte>();

        // Read data
        Cursor cur = m_db.rawQuery(
            "SELECT id, value, ident FROM word_train ORDER BY id ASC",
            null
        );
        if (cur.moveToFirst()) {
            do {
                // Get data
                byte[] values = cur.getBlob(1);
                byte[] idents = cur.getBlob(2);
                ByteBuffer ident_buf = ByteBuffer.wrap(idents);

                // Copy idents
                for (int i = 0; i < values.length; i++)
                    map.put(ident_buf.getInt(), values[i]);

            } while (cur.moveToNext());
        }
        cur.close();

        // New data
        int[] ident = m_file_word.IdentArray();
        m_train = new byte[ident.length];

        // Loop new data
        for (int i = 0; i < m_train.length; i++) {
            if (map.containsKey(ident[i])) {
                m_train[i] = map.get(ident[i]);
            } else {
                m_train[i] = (byte) 0x80;
            }
        }

        // Database update
        // Insert new words
        SQLiteStatement stmt_insert = m_db.compileStatement("INSERT INTO word_train (id, value, ident) VALUES (?, ?, ?)");
        m_db.beginTransaction();
        try {
            // Delete old data
            m_db.execSQL("DELETE FROM word_train;");

            // Loop entry blocks
            int num_blocks = ident.length / BLOCK_SIZE + ((ident.length % BLOCK_SIZE == 0) ? 0 : 1);
            for (int i = 0; i < num_blocks; i++) {
                // Calculate number of entries in block
                int num_entry = ident.length - (i * BLOCK_SIZE);
                if (num_entry > BLOCK_SIZE)
                    num_entry = BLOCK_SIZE;

                // Values
                byte[] value_arr = Arrays.copyOfRange(m_train, i * BLOCK_SIZE, (i * BLOCK_SIZE) + num_entry);

                // Idents
                ByteBuffer ident_buf = ByteBuffer.allocate(num_entry * 4);
                ident_buf.asIntBuffer().put(ident, i * BLOCK_SIZE, num_entry);

                // Sql
                stmt_insert.bindLong(1, i);
                stmt_insert.bindBlob(2, value_arr);
                stmt_insert.bindBlob(3, ident_buf.array());
                stmt_insert.executeInsert();
            }

            // Success
            m_db.setTransactionSuccessful();
        } finally {
            m_db.endTransaction();
        }
    }

    private void TableRead()
    {
        // Initialize data
        m_train = new byte[m_file_word.InfoLength()];

        // Read data
        Cursor cur = m_db.rawQuery(
            "SELECT id, value FROM word_train",
            null
        );
        if (cur.moveToFirst()) {
            do {
                // Get data
                int block_id = cur.getInt(0);
                byte[] values = cur.getBlob(1);

                // Copy
                for (int i = 0; i < values.length; i++)
                    m_train[(block_id * BLOCK_SIZE) + i] = values[i];

            } while (cur.moveToNext());
        }
        cur.close();
    }

    // Training data
    protected synchronized void TrainUpdate(int id)
    {
        // Block
        int block_id = id / BLOCK_SIZE;
        int num_entry = m_train.length - (block_id * BLOCK_SIZE);
        if (num_entry > BLOCK_SIZE)
            num_entry = BLOCK_SIZE;

        // Prepared statements
        if (m_stmt_train_update == null)
            m_stmt_train_update = m_db.compileStatement("UPDATE word_train SET value = ? WHERE id = ?");

        // Sql
        m_stmt_train_update.bindBlob(1, Arrays.copyOfRange(m_train, block_id * BLOCK_SIZE, (block_id * BLOCK_SIZE) + num_entry));
        m_stmt_train_update.bindLong(2, block_id);
        m_stmt_train_update.executeUpdateDelete();
    }

    // Training data reset
    public synchronized void TrainReset(int[] id)
    {
        // Update data
        for (int i = 0; i < id.length; i++)
            m_train[id[i]] &= 0x80;

        // Database update
        SQLiteStatement stmt_update = m_db.compileStatement("UPDATE word_train SET value = ? WHERE id = ?");
        m_db.beginTransaction();
        try {
            // Loop entry blocks
            int num_blocks = m_train.length / BLOCK_SIZE + ((m_train.length % BLOCK_SIZE == 0) ? 0 : 1);
            for (int i = 0; i < num_blocks; i++) {
                // Calculate number of entries in block
                int num_entry = m_train.length - (i * BLOCK_SIZE);
                if (num_entry > BLOCK_SIZE)
                    num_entry = BLOCK_SIZE;

                // Values
                byte[] value_arr = Arrays.copyOfRange(m_train, i * BLOCK_SIZE, (i * BLOCK_SIZE) + num_entry);

                // Sql
                stmt_update.bindBlob(1, value_arr);
                stmt_update.bindLong(2, i);
                stmt_update.executeUpdateDelete();
            }

            // Success
            m_db.setTransactionSuccessful();
        } finally {
            m_db.endTransaction();
        }
    }
}
