package fi.harism.wallpaper.flier;

import android.content.Context;
import android.opengl.Matrix;

public class FlierClouds {
	
	private FlierShader mShaderPoint = new FlierShader();
	private float[] mProjM = new float[16];
	
	public void render() {
		
	}
	
	public void init(Context ctx) {
		mShaderPoint.setProgram(ctx.getString(R.string.shader_point_vs), ctx.getString(R.string.shader_point_fs));
	}
	
	public void init(int width, int height) {
		float aspectRatio = (float)height / width;
		Matrix.frustumM(mProjM, 0, -1f, 1f, -aspectRatio, aspectRatio, 1f, 11f);
	}

}
