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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataDatabase extends SQLiteOpenHelper
{
    // Database files
    private static final int DB_VERSION = 3;
    private static final String DB_FILE = "kotoba.db";

    // Database
    SQLiteDatabase m_db = null;

    // Version
    private int m_version = 0;

    // Constructor
    public DataDatabase(Context context)
    {
        // Superclass
        super(context, DB_FILE, null, DB_VERSION);
    }

    // Database opening
    public void Open()
    {
        // Open database
        m_db = this.getWritableDatabase();
        m_version = VersionGet();

        // Update
        if (m_version > 0 && m_version < 3) {
            // Clear old static data tables
            m_db.execSQL("DROP TABLE IF EXISTS static_category;");
            m_db.execSQL("DROP TABLE IF EXISTS static_sentence;");
            m_db.execSQL("DROP INDEX IF EXISTS static_word_data_ih;");
            m_db.execSQL("DROP TABLE IF EXISTS static_word_data;");
            m_db.execSQL("DROP INDEX IF EXISTS static_word_sref_id;");
            m_db.execSQL("DROP TABLE IF EXISTS static_word_sref;");
            m_db.execSQL("DROP INDEX IF EXISTS static_word_cref_id;");
            m_db.execSQL("DROP INDEX IF EXISTS static_word_cref_ic;");
            m_db.execSQL("DROP TABLE IF EXISTS static_word_cref;");
        }
    }

    public SQLiteDatabase Database()
    {
        return m_db;
    }

    // Successfully initialized all modules
    public void Success()
    {
        // Update version in database if necessary
        if (m_version < DB_VERSION)
            VersionSet(DB_VERSION);
    }

    // Version helpers
    public int VersionDb()
    {
        return m_version;
    }

    public int VersionSys()
    {
        return DB_VERSION;
    }

    private int VersionGet()
    {
        // Version table
        m_db.execSQL("CREATE TABLE IF NOT EXISTS dbinfo (version INT);");

        // Get version if any
        Cursor cur = m_db.query("dbinfo", new String[]{ "version" }, null, null, null, null, null);
        if (cur.getCount() == 0) {
            cur.close();
            return 0;
        }
        if (!cur.moveToFirst()) {
            cur.close();
            return 0;
        }
        int version = cur.getInt(0);
        cur.close();
        return version;
    }

    private void VersionSet(int version)
    {
        // Version value
        m_version = version;

        // Version table
        m_db.execSQL("CREATE TABLE IF NOT EXISTS dbinfo (version INT);");

        // Get version count
        Cursor cur = m_db.query("dbinfo", new String[]{ "version" }, null, null, null, null, null);
        int count = cur.getCount();
        cur.close();
        if (count == 0) {
            // Insert
            m_db.execSQL("INSERT INTO dbinfo (version) VALUES (" + Integer.toString(m_version) + ");");
        } else {
            // Update
            m_db.execSQL("UPDATE dbinfo SET version = " + Integer.toString(m_version) + ";");
        }
    }

    // Update stubs
    @Override public void onCreate(SQLiteDatabase db)
    {

    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}
