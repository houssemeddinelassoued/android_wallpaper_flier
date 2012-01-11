package fi.harism.wallpaper.flier;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.opengl.GLSurfaceView;

public final class FlierEGLConfigChooser implements
		GLSurfaceView.EGLConfigChooser {

	private boolean mNeedsDepth;

	public FlierEGLConfigChooser(boolean needsDepth) {
		mNeedsDepth = needsDepth;
	}

	@Override
	public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
		int[] configSpec = { EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4,
				EGL10.EGL_BLUE_SIZE, 4, EGL10.EGL_ALPHA_SIZE, 0,
				EGL10.EGL_DEPTH_SIZE, 0, EGL10.EGL_STENCIL_SIZE, 0,
				EGL10.EGL_NONE };

		int[] temp = new int[1];
		egl.eglChooseConfig(display, configSpec, null, 0, temp);
		int numConfigs = temp[0];
		if (numConfigs <= 0) {
			throw new RuntimeException("No configs found.");
		}
		EGLConfig[] configs = new EGLConfig[numConfigs];
		egl.eglChooseConfig(display, configSpec, configs, numConfigs, temp);

		int highestSum = 0;
		EGLConfig highestConfig = null;
		for (EGLConfig config : configs) {
			int r = getConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE,
					0, temp);
			int g = getConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE,
					0, temp);
			int b = getConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE,
					0, temp);
			int d = getConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE,
					0, temp);
			int a = getConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE,
					0, temp);
			int s = getConfigAttrib(egl, display, config,
					EGL10.EGL_STENCIL_SIZE, 0, temp);
			int sum = r + g + b + (mNeedsDepth ? d : -d) - a - s;
			if (sum > highestSum) {
				highestSum = sum;
				highestConfig = config;
			}
		}
		if (highestConfig == null) {
			throw new RuntimeException(
					"No config chosen, this should never happen.");
		}
		return highestConfig;
	}

	private int getConfigAttrib(EGL10 egl, EGLDisplay display,
			EGLConfig config, int attribute, int defaultValue, int[] temp) {
		if (egl.eglGetConfigAttrib(display, config, attribute, temp)) {
			return temp[0];
		}
		return defaultValue;
	}
}
