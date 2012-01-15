package fi.harism.wallpaper.flier.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import fi.harism.wallpaper.flier.R;

public final class FlierBrightnessPreference extends DialogPreference {

	private SeekBar mSeekBar;
	private int mValue;

	public FlierBrightnessPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		mSeekBar.setMax(120 - 50);
		mSeekBar.setProgress(mValue - 50);
	}

	@Override
	protected View onCreateDialogView() {
		mSeekBar = (SeekBar) LayoutInflater.from(getContext()).inflate(
				R.layout.preference_brightness, null);
		return mSeekBar;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			int value = mSeekBar.getProgress() + 50;
			if (callChangeListener(value)) {
				mValue = value;
				persistInt(mValue);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		return ta.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mValue = restoreValue ? getPersistedInt(mValue)
				: (Integer) defaultValue;
		if (!restoreValue) {
			persistInt(mValue);
		}
	}

}
