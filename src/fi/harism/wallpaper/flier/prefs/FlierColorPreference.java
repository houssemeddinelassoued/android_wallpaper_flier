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

public class FlierColorPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {

	private SeekBar mSeekBarR, mSeekBarG, mSeekBarB;
	private int mValue;
	private View mViewColor;

	public FlierColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private int getCurrentColor() {
		return Color.rgb(mSeekBarR.getProgress(), mSeekBarG.getProgress(),
				mSeekBarB.getProgress());
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mViewColor.setBackgroundColor(mValue);
		mSeekBarR.setProgress(Color.red(mValue));
		mSeekBarG.setProgress(Color.green(mValue));
		mSeekBarB.setProgress(Color.blue(mValue));
	}

	@Override
	public View onCreateDialogView() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.preference_color, null);

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
