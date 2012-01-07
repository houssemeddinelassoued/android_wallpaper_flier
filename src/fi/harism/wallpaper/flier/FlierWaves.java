package fi.harism.wallpaper.flier;

import java.nio.ByteBuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;

public final class FlierWaves {

	private final FlierShader mShaderWave = new FlierShader();
	private final FlierShader mShaderWavePoint = new FlierShader();
	private ByteBuffer mVertices;

	private final FlierFbo mWaveFbo = new FlierFbo();
	private int mWidth, mHeight, mWaveSize;
	private float mXOffset;

	public FlierWaves() {
		final byte[] COORDS = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mVertices = ByteBuffer.allocateDirect(4 * 2);
		mVertices.put(COORDS).position(0);
	}

	public void onDrawFrame() {
		mShaderWave.useProgram();

		int uPositionOffset = mShaderWave.getHandle("uPositionOffset");
		int uTextureSize = mShaderWave.getHandle("uTextureSize");
		int uColor = mShaderWave.getHandle("uColor");
		int aPosition = mShaderWave.getHandle("aPosition");

		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mVertices);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mWaveFbo.getTexture(0));

		int width = mWidth;
		int height = mHeight / 4;
		long time = SystemClock.uptimeMillis();
		float dx1 = sin(time, 2000, .2f) + .2f - mXOffset;
		float dx2 = sin(time, 2345, .2f) + .2f - mXOffset;
		float dy1 = sin(time, 5000, .2f) - .2f;
		float dy2 = sin(time, 5234, .2f) - .4f;

		GLES20.glViewport(0, 0, width, height);
		GLES20.glUniform2f(uPositionOffset, dx1, dy1);
		GLES20.glUniform2f(uTextureSize, (float) width / mWaveSize,
				(float) height / mWaveSize);
		GLES20.glUniform4f(uColor, .3f, .4f, .6f, 1f);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glUniform2f(uPositionOffset, dx2, dy2);
		GLES20.glUniform4f(uColor, .5f, .6f, .8f, 1f);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glViewport(0, 0, mWidth, mHeight);
	}

	public void onSurfaceChanged(int width, int height) {
		mWidth = width;
		mHeight = height;
		mWaveSize = Math.min(width, height) / 5;

		mWaveFbo.init(mWaveSize, mWaveSize, 1);
		mWaveFbo.bind();
		mWaveFbo.bindTexture(0);
		GLES20.glClearColor(1f, 0f, 0f, 1f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_REPEAT);

		mShaderWavePoint.useProgram();
		int uPointSize = mShaderWavePoint.getHandle("uPointSize");
		int uBrightness = mShaderWavePoint.getHandle("uBrightness");
		int aPosition = mShaderWavePoint.getHandle("aPosition");

		ByteBuffer bBuffer = ByteBuffer.allocateDirect(2);
		bBuffer.put(new byte[] { 0, -1 }).position(0);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				bBuffer);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glUniform1f(uBrightness, .5f);
		GLES20.glUniform1f(uPointSize, mWaveSize + 3);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

		GLES20.glEnable(GLES20.GL_STENCIL_TEST);
		GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0x01, 0xFFFFFFFF);
		GLES20.glStencilOp(GLES20.GL_REPLACE, GLES20.GL_REPLACE,
				GLES20.GL_REPLACE);
		GLES20.glUniform1f(uBrightness, .0f);
		GLES20.glUniform1f(uPointSize, mWaveSize);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
	}

	public void onSurfaceCreated(Context ctx) {
		mShaderWavePoint.setProgram(
				ctx.getString(R.string.shader_wave_point_vs),
				ctx.getString(R.string.shader_wave_point_fs));
		mShaderWave.setProgram(ctx.getString(R.string.shader_wave_vs),
				ctx.getString(R.string.shader_wave_fs));
	}

	public void setXOffset(float xOffset) {
		mXOffset = xOffset;
	}

	private float sin(long time, long frequency, float multiplier) {
		return multiplier
				* (float) Math.sin((2 * Math.PI * (time % frequency))
						/ frequency);
	}

}
