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

public final class FlierClouds {

	private static final float ZNEAR = 1f, ZFAR = 6f;

	private FloatBuffer mBufferPoints;
	private final Vector<Cloud> mClouds = new Vector<Cloud>();
	private float mMaxPointSizeNear, mMaxPointSizeFar;
	private final float[] mProjM = new float[16];
	private final RectF mRectNear = new RectF(), mRectFar = new RectF();
	private final FlierShader mShaderPoint = new FlierShader();
	private float mXOffset, mXOffsetMultiplier = 4f;

	public FlierClouds(int cloudCount, int pointsPerCloud) {
		ByteBuffer bBuffer = ByteBuffer.allocateDirect(cloudCount
				* pointsPerCloud * 4 * 4);
		mBufferPoints = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

		for (int i = 0; i < cloudCount; ++i) {
			Cloud cloud = new Cloud(i * pointsPerCloud, pointsPerCloud);
			mClouds.add(cloud);
		}
	}

	public void init(Context ctx) {
		mShaderPoint.setProgram(ctx.getString(R.string.shader_cloud_vs),
				ctx.getString(R.string.shader_cloud_fs));
	}

	public void init(int width, int height) {
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
			cloud.init();
		}
	}

	private float rand(float min, float max) {
		return min + (float) Math.random() * (max - min);
	}

	public void render() {
		boolean needsSorting = false;
		for (Cloud cloud : mClouds) {
			needsSorting |= cloud.animate();
		}
		if (needsSorting) {
			final Comparator<Cloud> comparator = new Comparator<Cloud>() {
				@Override
				public int compare(Cloud arg0, Cloud arg1) {
					return arg0.getZ() >= arg1.getZ() ? -1 : 1;
				}
			};
			Collections.sort(mClouds, comparator);
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
				4 * 4, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aPosition);
		mBufferPoints.position(3);
		GLES20.glVertexAttribPointer(aPointSize, 1, GLES20.GL_FLOAT, false,
				4 * 4, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aPointSize);

		GLES20.glEnable(GLES20.GL_STENCIL_TEST);
		GLES20.glStencilFunc(GLES20.GL_EQUAL, 0x00, 0xFFFFFFFF);
		GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_INCR, GLES20.GL_INCR);

		for (Cloud cloud : mClouds) {
			GLES20.glUniform1f(uXOffset, cloud.getXOffset() + mXOffset);
			GLES20.glUniform1f(uPointSizeOffset, 0f);
			GLES20.glUniform4f(uColor, 1f, 1f, 1f, 1f);
			GLES20.glDrawArrays(GLES20.GL_POINTS, cloud.getIndex(),
					cloud.getPointCount());
			GLES20.glUniform1f(uPointSizeOffset, 4f);
			GLES20.glUniform4f(uColor, 0f, 0f, 0f, 1f);
			GLES20.glDrawArrays(GLES20.GL_POINTS, cloud.getIndex(),
					cloud.getPointCount());
		}

		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
	}

	public void setXOffset(float xOffset) {
		mXOffset = xOffset * mXOffsetMultiplier;
	}

	private void unproject(float[] projInv, RectF rect, float z) {
		final float result[] = new float[4];
		Matrix.multiplyMV(result, 0, projInv, 0, new float[] { -1, 1, z, 1 }, 0);
		rect.left = result[0] / result[3];
		rect.top = result[1] / result[3];
		Matrix.multiplyMV(result, 0, projInv, 0, new float[] { 1, -1, z, 1 }, 0);
		rect.right = result[0] / result[3];
		rect.bottom = result[1] / result[3];
	}

	private class Cloud {
		private int mIndex, mPointCount;
		private long mInitTime;
		private float mSpeed, mXOffset;
		private RectF mViewRect = new RectF(), mContentRect = new RectF();

		public Cloud(int index, int pointCount) {
			mIndex = index;
			mPointCount = pointCount;
		}

		public boolean animate() {
			float t = (float) (SystemClock.uptimeMillis() - mInitTime) / 30000;
			mXOffset -= mSpeed * t;
			if (mXOffset + mContentRect.width() * 3f < mViewRect.left) {
				genRandCloud();
				mXOffset = mViewRect.right + mContentRect.width();
				mSpeed = rand(.1f, .2f);
				mInitTime = SystemClock.uptimeMillis();
				return true;
			}
			return false;
		}

		private void genRandCloud() {
			float z = rand(ZFAR, ZNEAR);
			float t = (z - ZNEAR) / (ZFAR - ZNEAR);
			mViewRect.left = mRectNear.left + t
					* (mRectFar.left - mRectNear.left);
			mViewRect.right = mRectNear.right + t
					* (mRectFar.right - mRectNear.right);
			mViewRect.top = mRectNear.top + t * (mRectFar.top - mRectNear.top);
			mViewRect.bottom = mViewRect.top * 0.25f;

			mContentRect.right = mViewRect.width() * 0.2f;
			mContentRect.top = mViewRect.height() * 0.2f;

			float y = rand(mViewRect.bottom,
					mViewRect.top - mContentRect.height());
			float ptSz = mMaxPointSizeNear + t
					* (mMaxPointSizeFar - mMaxPointSizeNear);

			mBufferPoints.position(mIndex * 4);
			for (int i = 0; i < mPointCount; ++i) {
				mBufferPoints.put(rand(0, mContentRect.width()))
						.put(rand(0, mContentRect.height()) + y).put(-z);
				mBufferPoints.put(rand(ptSz / 3, ptSz));
			}
		}

		public int getIndex() {
			return mIndex;
		}

		public int getPointCount() {
			return mPointCount;
		}

		public float getXOffset() {
			return mXOffset;
		}

		public float getZ() {
			return mBufferPoints.get(mIndex * 4 + 2);
		}

		public void init() {
			genRandCloud();
			mXOffset = rand(mViewRect.left - mContentRect.width(),
					mViewRect.right + mContentRect.width());
			mSpeed = rand(.1f, .2f);
			mInitTime = SystemClock.uptimeMillis();
		}
	}

}
