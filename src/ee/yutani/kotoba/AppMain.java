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

import android.app.Application;
import android.preference.PreferenceManager;

// Application
public class AppMain extends Application implements DataMain.Callback
{
    // Activity callbacks
    public interface ActivityCallback
    {
        // States
        public void StateNormal();

        // Loading progress dialog
        public void LoadingCreate();
        public void LoadingDismiss();
        public void LoadingProgress(String text);
    }

    public interface FragmentCallback
    {
        // States
        public void StateNormal();
    }

    // State
    private enum State {
        STATE_LOADING,
        STATE_NORMAL,
        STATE_ERROR
    }
    private State m_state = State.STATE_LOADING;

    // Database
    private DataMain m_data = null;

    // Activity and frament callbacks
    private ActivityCallback m_activity = null;
    private ArrayList<FragmentCallback> m_fragment = new ArrayList<FragmentCallback>();

    // Create
    @Override public void onCreate()
    {
        // Default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Loading stage
        m_data = new DataMain(this.getApplicationContext(), this);
        m_data.execute();

        // Superclass
        super.onCreate();
    }

    // Database loading
    @Override public void DatabaseLoadingFinished()
    {
        m_state = State.STATE_NORMAL;
        ActivityRefresh();
        FragmentRefresh();
    }

    @Override public void DatabaseLoadingProgress(String text)
    {
        if (m_activity != null)
            m_activity.LoadingProgress(text);
    }

    // Database gets
    public DataMain Database()
    {
        return m_data;
    }

    // Activity
    public void ActivityAttach(ActivityCallback callback)
    {
        m_activity = callback;
        ActivityRefresh();
    }

    public void ActivityRefresh()
    {
        if (m_activity != null) {
            switch (m_state) {
                case STATE_ERROR:
                    break;

                case STATE_LOADING:
                    m_activity.LoadingCreate();
                    break;

                case STATE_NORMAL:
                    m_activity.StateNormal();
                    break;
            }
        }
    }

    // Fragment
    public void FragmentAttach(FragmentCallback callback)
    {
        m_fragment.add(callback);
        FragmentUpdate(callback);
    }

    public void FragmentDetach(FragmentCallback callback)
    {
        m_fragment.remove(callback);
    }

    public void FragmentRefresh()
    {
        for (FragmentCallback callback : m_fragment)
            FragmentUpdate(callback);
    }

    public void FragmentUpdate(FragmentCallback callback)
    {
        switch (m_state) {
            case STATE_NORMAL:
                callback.StateNormal();
                break;
        }
    }
}
