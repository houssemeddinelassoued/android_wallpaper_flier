package fi.harism.wallpaper.flier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

public class FlierPlane {

	private FloatBuffer mBufferVertices;
	private ByteBuffer mBufferLineIndices;
	private FlierShader mShaderPlane = new FlierShader();
	private float[] mProjM = new float[16], mViewM = new float[16];

	public FlierPlane() {
		ByteBuffer bBuffer = ByteBuffer.allocateDirect(6 * 3 * 4);
		mBufferVertices = bBuffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();

		final float WIDTH = 1f, HEIGHT = 0.3f, LENGTH = 1.2f, BEND = 0.3f;
		float[] vertices = { 0f, HEIGHT, -LENGTH, WIDTH, HEIGHT, LENGTH, BEND, HEIGHT, LENGTH, 0f, -HEIGHT,
				LENGTH, -BEND, HEIGHT, LENGTH, -WIDTH, HEIGHT, LENGTH };
		mBufferVertices.put(vertices).position(0);
		
		mBufferLineIndices = ByteBuffer.allocateDirect(9 * 2);
		byte[] indices = { 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 1, 2, 2, 3, 3, 4, 4, 5 };
		mBufferLineIndices.put(indices).position(0);
	}
	
	public void render() {
		mShaderPlane.useProgram();
		
		float[] modelViewProjM = new float[16];
		float rx = (float)Math.sin(Math.PI * (SystemClock.uptimeMillis() % 3000) / 1500) * 10f;
		float rz = (float)Math.cos(Math.PI * (SystemClock.uptimeMillis() % 4000) / 2000) * 10f;
		float ry = (float)(SystemClock.uptimeMillis() % (360 * 30)) / 30;
		Matrix.setRotateM(modelViewProjM, 0, rx, 1f, 0, 0);
		Matrix.rotateM(modelViewProjM, 0, ry, 0, 1f, 0);
		Matrix.rotateM(modelViewProjM, 0, rz, 0, 0, 1f);
		
		Matrix.multiplyMM(modelViewProjM, 0, mViewM, 0, modelViewProjM, 0);
		Matrix.multiplyMM(modelViewProjM, 0, mProjM, 0, modelViewProjM, 0);
		
		int uModelViewProjM = mShaderPlane.getHandle("uModelViewProjM");
		int uColor = mShaderPlane.getHandle("uColor");
		int aPosition = mShaderPlane.getHandle("aPosition");
		GLES20.glUniformMatrix4fv(uModelViewProjM, 1, false, modelViewProjM, 0);
		GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, mBufferVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		
		GLES20.glStencilFunc(GLES20.GL_ALWAYS, 0x01, 0xFFFFFFFF);
		GLES20.glStencilOp(GLES20.GL_REPLACE, GLES20.GL_REPLACE, GLES20.GL_REPLACE);
		
		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glPolygonOffset(1f, 1f);
		GLES20.glUniform4f(uColor, 1f, 1f, 1f, 1f);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);
		GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
		
		GLES20.glLineWidth(1f);
		GLES20.glUniform4f(uColor, 0f, 0f, 0f, 0f);
		GLES20.glDrawElements(GLES20.GL_LINES, 18, GLES20.GL_UNSIGNED_BYTE, mBufferLineIndices);		
	}
	
	public void init(int width, int height) {
		float aspectRatio = (float)height / width;
		Matrix.orthoM(mProjM, 0, -3f, 3f, -aspectRatio * 2f, aspectRatio * 4f, 1f, 11f);
		Matrix.setLookAtM(mViewM, 0, 0, 0, 4f, 0, 0, 0, 0f, 1f, 0f);
	}
	
	public void init(Context ctx) {
		mShaderPlane.setProgram(ctx.getString(R.string.shader_plane_vs), ctx.getString(R.string.shader_plane_fs));
	}

}