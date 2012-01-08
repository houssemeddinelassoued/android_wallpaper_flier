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
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import android.content.Context;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

/**
 * Class for handling cloud manipulation and rendering.
 */
public final class FlierClouds {

	// Float size in bytes.
	private static final int SIZE_FLOAT = 4;
	// Number of floats in point.
	private static final int SIZE_POINT = 4;
	// Z near and far clipping planes.
	private static final float ZNEAR = 1f, ZFAR = 6f;

	// Buffer for holding vertex/point data.
	private FloatBuffer mBufferPoints;
	// Cloud storage.
	private final Vector<Cloud> mClouds = new Vector<Cloud>();
	// Maximum point sizes for near and far clipping plane.
	private float mMaxPointSizeNear, mMaxPointSizeFar;
	// Number of points per cloud.
	private int mPointsPerCloud;
	// Projection matrix.
	private final float[] mProjM = new float[16];
	// View rectangles for near and far clipping planes.
	private final RectF mRectNear = new RectF(), mRectFar = new RectF();
	// Last rendering time.
	private long mRenderTime;
	// Shader for rendering points clouds consist of.
	private final FlierShader mShaderPoint = new FlierShader();
	// X -offset for handling scrolling and multiplier for adjusting its amount.
	private float mXOffset, mXOffsetMultiplier = 4f;

	/**
	 * Default constructor.
	 * 
	 * @param cloudCount
	 *            Number of clouds.
	 * @param pointsPerCloud
	 *            Points per cloud.
	 */
	public FlierClouds(int cloudCount, int pointsPerCloud) {
		mPointsPerCloud = pointsPerCloud;

		ByteBuffer bBuffer = ByteBuffer.allocateDirect(cloudCount
				* pointsPerCloud * SIZE_POINT * SIZE_FLOAT);
		mBufferPoints = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

		for (int i = 0; i < cloudCount; ++i) {
			Cloud cloud = new Cloud();
			cloud.mStartIndex = i * pointsPerCloud;
			mClouds.add(cloud);
		}
	}

	/**
	 * Generates/initializes cloud with random values.
	 * 
	 * @param cloud
	 *            Cloud to modify.
	 */
	private void genRandCloud(Cloud cloud) {
		RectF rect = cloud.mViewRect;

		cloud.mZValue = rand(-ZFAR, -ZNEAR);
		float t = (-cloud.mZValue - ZNEAR) / (ZFAR - ZNEAR);
		rect.left = mRectNear.left + t * (mRectFar.left - mRectNear.left);
		rect.right = mRectNear.right + t * (mRectFar.right - mRectNear.right);
		rect.top = mRectNear.top + t * (mRectFar.top - mRectNear.top);
		rect.bottom = rect.top * 0.25f;

		cloud.mWidth = rect.width() * 0.2f;
		cloud.mHeight = rect.height() * 0.2f;
		cloud.mSpeed = rand(.3f, .6f);

		float y = rand(rect.bottom, rect.top - cloud.mHeight);
		float pointSz = mMaxPointSizeNear + t
				* (mMaxPointSizeFar - mMaxPointSizeNear);

		mBufferPoints.position(cloud.mStartIndex * SIZE_POINT);
		for (int i = 0; i < mPointsPerCloud; ++i) {
			mBufferPoints.put(rand(0, cloud.mWidth))
					.put(rand(0, cloud.mHeight) + y).put(cloud.mZValue);
			mBufferPoints.put(rand(pointSz / 2, pointSz));
		}
	}

	/**
	 * Called from renderer for rendering clouds into scene.
	 */
	public void onDrawFrame() {
		boolean needsSorting = false;
		long renderTime = SystemClock.uptimeMillis();
		float t = (float) (renderTime - mRenderTime) / 1000;
		mRenderTime = renderTime;
		for (Cloud cloud : mClouds) {
			cloud.mXOffset -= t * cloud.mSpeed;
			if (cloud.mXOffset + cloud.mWidth * 3 < cloud.mViewRect.left) {
				genRandCloud(cloud);
				cloud.mXOffset = cloud.mViewRect.right + cloud.mWidth;
				needsSorting = true;
			}
		}
		if (needsSorting) {
			sortClouds();
		}

		mShaderPoint.useProgram();
		int uProjM = mShaderPoint.getHandle("uProjM");
		int uXOffset = mShaderPoint.getHandle("uXOffset");
		int uPointSizeOffset = mShaderPoint.getHandle("uPointSizeOffset");
		int uColor = mShaderPoint.getHandle("uColor");
		int aPosition = mShaderPoint.getHandle("aPosition");
		int aPointSize = mShaderPoint.getHandle("aPointSize");

		GLES20.glUniformMatrix4fv(uProjM, 1, false, mProjM, 0);

		mBufferPoints.position(0);
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false,
				SIZE_POINT * SIZE_FLOAT, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aPosition);
		mBufferPoints.position(3);
		GLES20.glVertexAttribPointer(aPointSize, 1, GLES20.GL_FLOAT, false,
				SIZE_POINT * SIZE_FLOAT, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aPointSize);

		GLES20.glEnable(GLES20.GL_STENCIL_TEST);
		GLES20.glStencilFunc(GLES20.GL_EQUAL, 0x00, 0xFFFFFFFF);
		GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_INCR, GLES20.GL_INCR);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		for (Cloud cloud : mClouds) {
			GLES20.glUniform1f(uXOffset, cloud.mXOffset + mXOffset);
			GLES20.glUniform1f(uPointSizeOffset, 0f);
			GLES20.glUniform1f(uColor, 1f);
			GLES20.glDrawArrays(GLES20.GL_POINTS, cloud.mStartIndex,
					mPointsPerCloud);
			GLES20.glUniform1f(uPointSizeOffset, 4f);
			GLES20.glUniform1f(uColor, .5f);
			GLES20.glDrawArrays(GLES20.GL_POINTS, cloud.mStartIndex,
					mPointsPerCloud);
		}

		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
	}

	/**
	 * Called from renderer once surface has changed.
	 * 
	 * @param width
	 *            Width in pixels.
	 * @param height
	 *            Height in pixels.
	 */
	public void onSurfaceChanged(int width, int height) {
		float aspectRatio = (float) height / width;
		Matrix.frustumM(mProjM, 0, -1f, 1f, -aspectRatio, aspectRatio, ZNEAR,
				ZFAR);

		final float projInvM[] = new float[16];
		Matrix.invertM(projInvM, 0, mProjM, 0);
		unproject(projInvM, mRectNear, -1);
		unproject(projInvM, mRectFar, 1);

		mMaxPointSizeNear = Math.min(width, height) / 5;
		mMaxPointSizeFar = Math.min(width, height) / 10;

		mXOffsetMultiplier = mRectNear.width();
		mRectNear.right += mXOffsetMultiplier;
		mRectFar.right += mXOffsetMultiplier;

		for (Cloud cloud : mClouds) {
			genRandCloud(cloud);
			cloud.mXOffset = rand(cloud.mViewRect.left, cloud.mViewRect.right);
		}
		sortClouds();
		mRenderTime = SystemClock.uptimeMillis();
	}

	/**
	 * Called once surface has been created.
	 * 
	 * @param ctx
	 *            Context for reading shaders from.
	 */
	public void onSurfaceCreated(Context ctx) {
		mShaderPoint.setProgram(ctx.getString(R.string.shader_cloud_vs),
				ctx.getString(R.string.shader_cloud_fs));
	}

	/**
	 * Generates random value between [min, max).
	 * 
	 * @param min
	 *            Minimum value.
	 * @param max
	 *            Maximum value.
	 * @return Random value between [min, max).
	 */
	private float rand(float min, float max) {
		return min + (float) Math.random() * (max - min);
	}

	/**
	 * Sets x offset for clouds. Offset is expected to be a value between [0,
	 * 1].
	 * 
	 * @param xOffset
	 *            New x offset value.
	 */
	public void setXOffset(float xOffset) {
		mXOffset = xOffset * mXOffsetMultiplier;
	}

	/**
	 * Sorts clouds based on their z value.
	 */
	public void sortClouds() {
		final Comparator<Cloud> comparator = new Comparator<Cloud>() {
			@Override
			public int compare(Cloud arg0, Cloud arg1) {
				float z0 = arg0.mZValue;
				float z1 = arg1.mZValue;
				return z0 == z1 ? 0 : z0 < z1 ? 1 : -1;
			}
		};
		Collections.sort(mClouds, comparator);
	}

	/**
	 * Calculates unprojected rectangle at given z value in screen space.
	 * 
	 * @param projInv
	 *            Inverse of projection matrix.
	 * @param rect
	 *            Rectangle to contain result.
	 * @param z
	 *            Z value.
	 */
	private void unproject(float[] projInv, RectF rect, float z) {
		final float result[] = new float[4];
		Matrix.multiplyMV(result, 0, projInv, 0, new float[] { -1, 1, z, 1 }, 0);
		rect.left = result[0] / result[3];
		rect.top = result[1] / result[3];
		Matrix.multiplyMV(result, 0, projInv, 0, new float[] { 1, -1, z, 1 }, 0);
		rect.right = result[0] / result[3];
		rect.bottom = result[1] / result[3];
	}

	/**
	 * Private class for storing cloud information.
	 */
	private final class Cloud {
		public float mSpeed, mXOffset;
		public int mStartIndex;
		public final RectF mViewRect = new RectF();
		public float mWidth, mHeight, mZValue;
	}

}
