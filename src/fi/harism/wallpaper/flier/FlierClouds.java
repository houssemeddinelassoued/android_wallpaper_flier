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

	private static final int SIZE_FLOAT = 4;
	private static final int SIZE_POINT = 5;
	private static final float ZNEAR = 1f, ZFAR = 6f;

	private FloatBuffer mBufferPoints;
	private final Vector<Cloud> mClouds = new Vector<Cloud>();
	private float mMaxPointSizeNear, mMaxPointSizeFar;
	private int mPointsPerCloud;
	private final float[] mProjM = new float[16];
	private final RectF mRectNear = new RectF(), mRectFar = new RectF();
	private long mRenderTime;
	private final FlierShader mShaderPoint = new FlierShader();
	private float mXOffset, mXOffsetMultiplier = 4f;

	public FlierClouds(int cloudCount, int pointsPerCloud) {
		mPointsPerCloud = pointsPerCloud;

		ByteBuffer bBuffer = ByteBuffer.allocateDirect(cloudCount
				* pointsPerCloud * SIZE_POINT * SIZE_FLOAT);
		mBufferPoints = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

		for (int i = 0; i < cloudCount; ++i) {
			Cloud cloud = new Cloud();
			cloud.mIndex = i * pointsPerCloud;
			mClouds.add(cloud);
		}
	}

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

		mBufferPoints.position(cloud.mIndex * SIZE_POINT);
		for (int i = 0; i < mPointsPerCloud; ++i) {
			mBufferPoints.put(rand(0, cloud.mWidth))
					.put(rand(0, cloud.mHeight) + y).put(cloud.mZValue);
			mBufferPoints.put(rand(pointSz / 3, pointSz));
			mBufferPoints.put(1f - ((float) i / (mPointsPerCloud * 7)));
		}
	}

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
		int uColorMultiplier = mShaderPoint.getHandle("uColorMultiplier");
		int aPosition = mShaderPoint.getHandle("aPosition");
		int aPointSize = mShaderPoint.getHandle("aPointSize");
		int aColor = mShaderPoint.getHandle("aColor");

		GLES20.glUniformMatrix4fv(uProjM, 1, false, mProjM, 0);

		mBufferPoints.position(0);
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false,
				SIZE_POINT * SIZE_FLOAT, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aPosition);
		mBufferPoints.position(3);
		GLES20.glVertexAttribPointer(aPointSize, 1, GLES20.GL_FLOAT, false,
				SIZE_POINT * SIZE_FLOAT, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aPointSize);
		mBufferPoints.position(4);
		GLES20.glVertexAttribPointer(aColor, 1, GLES20.GL_FLOAT, false,
				SIZE_POINT * SIZE_FLOAT, mBufferPoints);
		GLES20.glEnableVertexAttribArray(aColor);

		GLES20.glEnable(GLES20.GL_STENCIL_TEST);
		GLES20.glStencilFunc(GLES20.GL_EQUAL, 0x00, 0xFFFFFFFF);
		GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_INCR, GLES20.GL_INCR);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		for (Cloud cloud : mClouds) {
			GLES20.glUniform1f(uXOffset, cloud.mXOffset + mXOffset);
			GLES20.glUniform1f(uPointSizeOffset, 0f);
			GLES20.glUniform1f(uColorMultiplier, 1f);
			GLES20.glDrawArrays(GLES20.GL_POINTS, cloud.mIndex, mPointsPerCloud);
			GLES20.glUniform1f(uPointSizeOffset, 4f);
			GLES20.glUniform1f(uColorMultiplier, 0f);
			GLES20.glDrawArrays(GLES20.GL_POINTS, cloud.mIndex, mPointsPerCloud);
		}

		GLES20.glDisable(GLES20.GL_STENCIL_TEST);
	}

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

	public void onSurfaceCreated(Context ctx) {
		mShaderPoint.setProgram(ctx.getString(R.string.shader_cloud_vs),
				ctx.getString(R.string.shader_cloud_fs));
	}

	private float rand(float min, float max) {
		return min + (float) Math.random() * (max - min);
	}

	public void setXOffset(float xOffset) {
		mXOffset = xOffset * mXOffsetMultiplier;
	}

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

	private void unproject(float[] projInv, RectF rect, float z) {
		final float result[] = new float[4];
		Matrix.multiplyMV(result, 0, projInv, 0, new float[] { -1, 1, z, 1 }, 0);
		rect.left = result[0] / result[3];
		rect.top = result[1] / result[3];
		Matrix.multiplyMV(result, 0, projInv, 0, new float[] { 1, -1, z, 1 }, 0);
		rect.right = result[0] / result[3];
		rect.bottom = result[1] / result[3];
	}

	private final class Cloud {
		public int mIndex;
		public float mSpeed, mXOffset;
		public final RectF mViewRect = new RectF();
		public float mWidth, mHeight, mZValue;
	}

}
