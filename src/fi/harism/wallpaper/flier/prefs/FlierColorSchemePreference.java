/*
   Copyright 2012 Harri Smått

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.wallpaper.flier.prefs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Color scheme list selector.
 */
public class FlierColorSchemePreference extends ListPreference {

	/**
	 * Default constructor.
	 */
	public FlierColorSchemePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		// Get current value for disabling dependents.
		boolean disableDependentsOrig = shouldDisableDependents();
		// Call super class implemention.
		super.onDialogClosed(positiveResult);
		// Get new/current value for disabling dependents.
		boolean disableDependentsNew = shouldDisableDependents();
		// If disable dependents value has changed do notification.
		if (disableDependentsOrig != disableDependentsNew) {
			notifyDependencyChange(disableDependentsNew);
		}
	}

	@Override
	public boolean shouldDisableDependents() {
		return !"0".equals(getPersistedString(null));
	}

}
