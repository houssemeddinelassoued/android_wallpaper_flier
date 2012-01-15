package fi.harism.wallpaper.flier.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import fi.harism.wallpaper.flier.R;

public final class FlierPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
