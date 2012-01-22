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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import fi.harism.wallpaper.flier.R;

/**
 * Color chooser dialog preference. Implementation is loosely borrowed from Mark
 * Murphy's color mixer project on GitHub
 * (https://github.com/commonsguy/cwac-colormixer). Layout used for dialog is a
 * 99% copy from him with only very small changes.
 */
public class FlierColorPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {

	// SeekBar instances.
	private SeekBar mSeekBarR, mSeekBarG, mSeekBarB;
	// Current color value.
	private int mValue;
	// Color preview View.
	private View mViewColor;

	/**
	 * Default constructor.
	 */
	public FlierColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Getter for current color from SeekBar's.
	 * 
	 * @return Color value.
	 */
	private int getCurrentColor() {
		return Color.rgb(mSeekBarR.getProgress(), mSeekBarG.getProgress(),
				mSeekBarB.getProgress());
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		// Set initial values for views.
		mViewColor.setBackgroundColor(mValue);
		mSeekBarR.setProgress(Color.red(mValue));
		mSeekBarG.setProgress(Color.green(mValue));
		mSeekBarB.setProgress(Color.blue(mValue));
	}

	@Override
	public View onCreateDialogView() {
		// Inflate color preference layout.
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.preference_color, null);
		// Fetch layout views for later use.
		mViewColor = view.findViewById(R.id.color_view);
		mSeekBarR = (SeekBar) view.findViewById(R.id.color_red_seekbar);
		mSeekBarR.setMax(255);
		mSeekBarR.setOnSeekBarChangeListener(this);
		mSeekBarG = (SeekBar) view.findViewById(R.id.color_green_seekbar);
		mSeekBarG.setMax(255);
		mSeekBarG.setOnSeekBarChangeListener(this);
		mSeekBarB = (SeekBar) view.findViewById(R.id.color_blue_seekbar);
		mSeekBarB.setMax(255);
		mSeekBarB.setOnSeekBarChangeListener(this);

		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			int color = getCurrentColor();
			if (callChangeListener(color)) {
				mValue = color;
				persistInt(mValue);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		return Color.parseColor(ta.getString(index));
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		mViewColor.setBackgroundColor(getCurrentColor());
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mValue = restoreValue ? getPersistedInt(mValue)
				: (Integer) defaultValue;
		if (!restoreValue) {
			persistInt(mValue);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

}
