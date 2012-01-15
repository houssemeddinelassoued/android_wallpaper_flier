package fi.harism.wallpaper.flier.prefs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class FlierColorSchemePreference extends ListPreference {

	public FlierColorSchemePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		boolean disableDependentsOrig = shouldDisableDependents();
		super.onDialogClosed(positiveResult);
		boolean disableDependentsNew = shouldDisableDependents();
		if (disableDependentsOrig != disableDependentsNew) {
			notifyDependencyChange(disableDependentsNew);
		}
	}

	@Override
	public boolean shouldDisableDependents() {
		return !"0".equals(getPersistedString(null));
	}

}
