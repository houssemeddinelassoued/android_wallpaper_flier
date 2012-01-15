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

package fi.harism.wallpaper.flier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.widget.Toast;

/**
 * Main renderer class.
 */
public final class FlierRenderer implements GLSurfaceView.Renderer {

	// Holder for background colors.
	private FloatBuffer mBackgroundColors;
	// Application context.
	private Context mContext;
	// Clouds rendering class.
	private final FlierClouds mFlierClouds = new FlierClouds(10, 10);
	// Fbo for offscreen rendering.
	private final FlierFbo mFlierFbo = new FlierFbo();
	// Plane rendering class.
	private final FlierPlane mFlierPlane = new FlierPlane();
	// Waves rendering class.
	private final FlierWaves mFlierWaves = new FlierWaves();
	// Brightness preference.
	private float mPreferenceBrightness;
	// Render quality preference.
	private int mPreferenceQuality;
	// Boolean to indicate preferences have changed.
	private boolean mPreferencesChanged;
	// Vertices for full view rendering.
	private FloatBuffer mScreenVertices;
	// Shader for copying offscreen texture on screen.
	private final FlierShader mShaderCopy = new FlierShader();
	// Shader for rendering background gradient.
	private final FlierShader mShaderFill = new FlierShader();
	// Surface/screen dimensions.
	private int mWidth, mHeight;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            Context to read shaders from.
	 */
	public FlierRenderer(Context context) {
		mContext = context;

		// Create screen coordinates float buffer.
		final float SCREEN_COORDS[] = { -1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f };
		ByteBuffer bBuf = ByteBuffer.allocateDirect(2 * 4 * 4);
		mScreenVertices = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mScreenVertices.put(SCREEN_COORDS).position(0);

		// Create background color float buffer.
		bBuf = ByteBuffer.allocateDirect(3 * 4 * 4);
		mBackgroundColors = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	/**
	 * Loads three component RGB values from preferences.
	 * 
	 * @param resId
	 *            Color preference key resource id.
	 * @param preferences
	 *            Preferences to load value from.
	 * @return Three element float RGB array.
	 */
	private float[] loadColor(int resId, SharedPreferences preferences) {
		String key = mContext.getString(resId);
		int color = preferences.getInt(key, 0);
		float[] retVal = new float[3];
		retVal[0] = (float) Color.red(color) / 255;
		retVal[1] = (float) Color.green(color) / 255;
		retVal[2] = (float) Color.blue(color) / 255;
		return retVal;
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		// First check if preferences have changed.
		if (mPreferencesChanged) {
			int width = mWidth;
			int height = mHeight;
			switch (mPreferenceQuality) {
			case 0:
				width /= 3;
				height /= 3;
				break;
			case 1:
				width /= 2;
				height /= 2;
				break;
			}
			mFlierFbo.init(width, height, 1, true, true);
			mFlierWaves.onSurfaceChanged(width, height);
			mFlierPlane.onSurfaceChanged(width, height);
			mFlierClouds.onSurfaceChanged(width, height);
			mPreferencesChanged = false;
		}

		// Disable unneeded rendering flags.
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Set render target to fbo.
		mFlierFbo.bind();
		mFlierFbo.bindTexture(0);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);

		// Render background gradient.
		mShaderFill.useProgram();
		int positionAttribLocation = mShaderFill.getHandle("aPosition");
		GLES20.glVertexAttribPointer(positionAttribLocation, 2,
				GLES20.GL_FLOAT, false, 0, mScreenVertices);
		GLES20.glEnableVertexAttribArray(positionAttribLocation);
		int colorAttribLocation = mShaderFill.getHandle("aColor");
		GLES20.glVertexAttribPointer(colorAttribLocation, 3, GLES20.GL_FLOAT,
				false, 0, mBackgroundColors);
		GLES20.glEnableVertexAttribArray(colorAttribLocation);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Render actual scene.
		mFlierWaves.onDrawFrame();
		mFlierPlane.onDrawFrame();
		mFlierClouds.onDrawFrame();

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		mShaderCopy.useProgram();
		int uBrightness = mShaderCopy.getHandle("uBrightness");
		int aPosition = mShaderCopy.getHandle("aPosition");
		GLES20.glUniform1f(uBrightness, mPreferenceBrightness);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFlierFbo.getTexture(0));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mWidth = width;
		mHeight = height;
		mPreferencesChanged = true;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		boolean[] retVal = new boolean[1];
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER, retVal, 0);
		if (retVal[0] == false) {
			Handler handler = new Handler(mContext.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(mContext, R.string.error_shader_compiler,
							Toast.LENGTH_LONG).show();
				}
			});
		} else {
			mShaderCopy.setProgram(mContext.getString(R.string.shader_copy_vs),
					mContext.getString(R.string.shader_copy_fs));
			mShaderFill.setProgram(mContext.getString(R.string.shader_fill_vs),
					mContext.getString(R.string.shader_fill_fs));
			mFlierWaves.onSurfaceCreated(mContext);
			mFlierPlane.onSurfaceCreated(mContext);
			mFlierClouds.onSurfaceCreated(mContext);
		}
	}

	/**
	 * Updates rendering values from preferences.
	 * 
	 * @param preferences
	 *            Preferences values.
	 */
	public void setPreferences(SharedPreferences preferences) {
		String key = mContext.getString(R.string.key_general_quality);
		mPreferenceQuality = Integer.parseInt(preferences.getString(key, "1"));
		key = mContext.getString(R.string.key_general_brightness);
		mPreferenceBrightness = (float) preferences.getInt(key, 100) / 100;

		key = mContext.getString(R.string.key_colors_scheme);
		int scheme = Integer.parseInt(preferences.getString(key, "1"));
		float[] bgColorTop, bgColorBottom, waveColorFront, waveColorBack, planeColor, cloudColor, cloudOutlineColor;

		switch (scheme) {
		case 1:
			bgColorTop = new float[] { .6f, .7f, .9f };
			bgColorBottom = new float[] { .3f, .4f, .6f };
			waveColorFront = new float[] { .5f, .6f, .8f };
			waveColorBack = new float[] { .3f, .4f, .6f };
			planeColor = new float[] { .8f, .8f, .8f };
			cloudColor = new float[] { .9f, .9f, .9f };
			cloudOutlineColor = new float[] { .5f, .5f, .5f };
			break;
		case 2:
			bgColorTop = new float[] { .7f, .7f, .7f };
			bgColorBottom = new float[] { .4f, .4f, .4f };
			waveColorFront = new float[] { .6f, .6f, .6f };
			waveColorBack = new float[] { .4f, .4f, .4f };
			planeColor = new float[] { .8f, .8f, .8f };
			cloudColor = new float[] { .9f, .9f, .9f };
			cloudOutlineColor = new float[] { .5f, .5f, .5f };
			break;
		case 3:
			bgColorTop = new float[] { .9f, .6f, .7f };
			bgColorBottom = new float[] { .6f, .3f, .4f };
			waveColorFront = new float[] { .8f, .5f, .6f };
			waveColorBack = new float[] { .6f, .3f, .4f };
			planeColor = new float[] { .8f, .8f, .8f };
			cloudColor = new float[] { 1.0f, .8f, .85f };
			cloudOutlineColor = new float[] { .7f, .4f, .45f };
			break;
		case 4:
			bgColorTop = new float[] { .9f, .6f, .3f };
			bgColorBottom = new float[] { .6f, .3f, .1f };
			waveColorFront = new float[] { .7f, .4f, .1f };
			waveColorBack = new float[] { .6f, .3f, .1f };
			planeColor = new float[] { .8f, .8f, .8f };
			cloudColor = new float[] { .9f, .6f, .3f };
			cloudOutlineColor = new float[] { .6f, .3f, .1f };
			break;
		default:
			bgColorTop = loadColor(R.string.key_colors_bg_top, preferences);
			bgColorBottom = loadColor(R.string.key_colors_bg_bottom,
					preferences);
			waveColorFront = loadColor(R.string.key_colors_wave_front,
					preferences);
			waveColorBack = loadColor(R.string.key_colors_wave_back,
					preferences);
			planeColor = loadColor(R.string.key_colors_plane, preferences);
			cloudColor = loadColor(R.string.key_colors_cloud, preferences);
			cloudOutlineColor = loadColor(R.string.key_colors_cloud_outline,
					preferences);
			break;
		}

		mBackgroundColors.put(bgColorTop).put(bgColorBottom).put(bgColorTop)
				.put(bgColorBottom).position(0);
		mFlierWaves.setColors(waveColorFront, waveColorBack);
		mFlierPlane.setColor(planeColor);
		mFlierClouds.setColors(cloudColor, cloudOutlineColor);

		mPreferencesChanged = true;
	}

	/**
	 * Sets x offset for clouds. Offset is expected to be a value between [0,
	 * 1].
	 * 
	 * @param xOffset
	 *            New x offset value.
	 */
	public void setXOffset(float xOffset) {
		mFlierWaves.setXOffset(xOffset);
		mFlierClouds.setXOffset(xOffset);
	}

}
