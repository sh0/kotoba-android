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
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// Preferences fragment
public class MainPreferences extends PreferenceFragment
{
    // Training reptetition preference
    /*
    public class TrainingRepeatPreference extends DialogPreference
    {
        // Constructor
        public TrainingRepeatPreference(Context context, AttributeSet attrs)
        {
            // Superclass
            super(context, attrs);

            // Layout
            setDialogLayoutResource(R.layout.numberpicker_dialog);
            setPositiveButtonText(android.R.string.ok);
            setNegativeButtonText(android.R.string.cancel);
            setDialogIcon(null);
        }

        // Events
        @Override protected void onDialogClosed(boolean positive_result)
        {
            if (positive_result) {

            }
        }

        @Override protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
        {
            if (restorePersistedValue) {
                // Restore existing state
                mCurrentValue = this.getPersistedInt(DEFAULT_VALUE);
            } else {
                // Set default state from the XML attribute
                mCurrentValue = (Integer) defaultValue;
                persistInt(mCurrentValue);
            }
        }

        @Override protected Object onGetDefaultValue(TypedArray a, int index)
        {
            return a.getInteger(index, DEFAULT_VALUE);
        }
    }
    */

    // Create events
    @Override public void onCreate(Bundle savedInstanceState)
    {
        // Superclass
        super.onCreate(savedInstanceState);

        // Load preferences
        addPreferencesFromResource(R.xml.preferences);

        // Training repetition dialog
        /*
        Preference training_repeat = this.findPreference("training_repeat");
        training_repeat.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override public boolean onPreferenceClick(Preference preference) {

                return true;
            }
        });
        */
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state_saved)
    {
        // Search disable
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null)
            activity.SearchDisable();

        // Superclass
        return super.onCreateView(inflater, container, state_saved);
    }

    // Destroy events
    @Override public void onDestroyView()
    {
        // Search enable
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null)
            activity.SearchEnable();

        // Superclass
        super.onDestroyView();
    }
}
