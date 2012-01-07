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

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

public final class FlierPlane {

	private ByteBuffer mBufferLineIndices;
	private FloatBuffer mBufferVertices;
	private final float[] mProjM = new float[16], mViewM = new float[16];
	private final FlierShader mShaderPlane = new FlierShader();

	public FlierPlane() {
		ByteBuffer bBuffer = ByteBuffer.allocateDirect(6 * 3 * 4);
		mBufferVertices = bBuffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();

		final float WIDTH = 1f, HEIGHT = 0.3f, LENGTH = 1.2f, BEND = 0.3f;
		final float[] vertices = { 0f, HEIGHT, -LENGTH, WIDTH, HEIGHT, LENGTH,
				BEND, HEIGHT, LENGTH, 0f, -HEIGHT, LENGTH, -BEND, HEIGHT,
				LENGTH, -WIDTH, HEIGHT, LENGTH };
		mBufferVertices.put(vertices).position(0);

		mBufferLineIndices = ByteBuffer.allocateDirect(9 * 2);
		final byte[] indices = { 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 1, 2, 2, 3, 3,
				4, 4, 5 };
		mBufferLineIndices.put(indices).position(0);
	}

	public void onDrawFrame() {
		mShaderPlane.useProgram();

		long time = SystemClock.uptimeMillis();
		float rx = sin(time, 4000, 10f);
		float rz = sin(time, 6234, 10f);
		float ry = (float) (time % (360 * 30)) / 30;
		float scale = 0.75f + sin(time, 8345, .25f);

		final float[] modelViewProjM = new float[16];
		Matrix.setRotateM(modelViewProjM, 0, rx, 1f, 0, 0);
		Matrix.rotateM(modelViewProjM, 0, ry, 0, 1f, 0);
		Matrix.rotateM(modelViewProjM, 0, rz, 0, 0, 1f);

		Matrix.translateM(modelViewProjM, 0, 2f, .5f, 0f);

		Matrix.scaleM(modelViewProjM, 0, scale, scale, scale);

		Matrix.multiplyMM(modelViewProjM, 0, mViewM, 0, modelViewProjM, 0);
		Matrix.multiplyMM(modelViewProjM, 0, mProjM, 0, modelViewProjM, 0);

		int uModelViewProjM = mShaderPlane.getHandle("uModelViewProjM");
		int uColor = mShaderPlane.getHandle("uColor");
		int aPosition = mShaderPlane.getHandle("aPosition");
		GLES20.glUniformMatrix4fv(uModelViewProjM, 1, false, modelViewProjM, 0);
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false,
				3 * 4, mBufferVertices);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glEnable(GLES20.GL_STENCIL_TEST);
		GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0x01, 0xFFFFFFFF);
		GLES20.glStencilOp(GLES20.GL_REPLACE, GLES20.GL_REPLACE,
				GLES20.GL_REPLACE);

		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glPolygonOffset(1f, 1f);
		GLES20.glUniform4f(uColor, 1f, 1f, 1f, 1f);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);
		GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);

		GLES20.glLineWidth(1f);
		GLES20.glUniform4f(uColor, 0f, 0f, 0f, 0f);
		GLES20.glDrawElements(GLES20.GL_LINES, 18, GLES20.GL_UNSIGNED_BYTE,
				mBufferLineIndices);

		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
	}

	public void onSurfaceChanged(int width, int height) {
		float aspectRatio = (float) height / width;
		Matrix.orthoM(mProjM, 0, -3f, 3f, -aspectRatio * 2f, aspectRatio * 4f,
				1f, 21f);
		Matrix.setLookAtM(mViewM, 0, 0, 1f, 5f, 0, 0, 0, 0f, 1f, 0f);
	}

	public void onSurfaceCreated(Context ctx) {
		mShaderPlane.setProgram(ctx.getString(R.string.shader_plane_vs),
				ctx.getString(R.string.shader_plane_fs));
	}

	private float sin(long time, long frequency, float multiplier) {
		return multiplier
				* (float) Math.sin((2 * Math.PI * (time % frequency))
						/ frequency);
	}

}
