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
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

/**
 * Main renderer class.
 */
public final class FlierRenderer implements GLSurfaceView.Renderer {

	// Holder for background colors.
	private FloatBuffer mBackgroundColors;
	// Application context.
	private Context mContext;
	// Fbo for offscreen rendering.
	private final FlierFbo mFbo = new FlierFbo();
	private final FlierClouds mFlierClouds = new FlierClouds(15, 15);
	private final FlierPlane mFlierPlane = new FlierPlane();
	private final FlierWaves mFlierWaves = new FlierWaves();
	// Vertices for full view rendering.
	private FloatBuffer mScreenVertices;
	// Shader for copying offscreen texture on screen.
	private final FlierShader mShaderCopy = new FlierShader();
	// Shader for rendering background gradient.
	private final FlierShader mShaderFill = new FlierShader();
	// Surface size.
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
		final float BACKGROUND_COLOR_TOP[] = { .6f, .7f, .9f };
		final float BACKGROUND_COLOR_BOTTOM[] = { .3f, .4f, .6f };
		bBuf = ByteBuffer.allocateDirect(3 * 4 * 4);
		mBackgroundColors = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBackgroundColors.put(BACKGROUND_COLOR_TOP)
				.put(BACKGROUND_COLOR_BOTTOM).put(BACKGROUND_COLOR_TOP)
				.put(BACKGROUND_COLOR_BOTTOM).position(0);
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		// Disable unneeded rendering flags.
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Set render target to fbo.
		mFbo.bind();
		mFbo.bindTexture(0);
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

		// TODO: Render scene.
		mFlierWaves.onDrawFrame();
		mFlierPlane.onDrawFrame();
		mFlierClouds.onDrawFrame();

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		mShaderCopy.useProgram();
		int aPosition = mShaderCopy.getHandle("aPosition");
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFbo.getTexture(0));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mWidth = width;
		mHeight = height;
		mFbo.init(mWidth / 2, mHeight / 2, 1, true, true);
		mFlierWaves.onSurfaceChanged(mFbo.getWidth(), mFbo.getHeight());
		mFlierPlane.onSurfaceChanged(mFbo.getWidth(), mFbo.getHeight());
		mFlierClouds.onSurfaceChanged(mFbo.getWidth(), mFbo.getHeight());
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		mShaderCopy.setProgram(mContext.getString(R.string.shader_copy_vs),
				mContext.getString(R.string.shader_copy_fs));
		mShaderFill.setProgram(mContext.getString(R.string.shader_fill_vs),
				mContext.getString(R.string.shader_fill_fs));
		mFlierWaves.onSurfaceCreated(mContext);
		mFlierPlane.onSurfaceCreated(mContext);
		mFlierClouds.onSurfaceCreated(mContext);
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
