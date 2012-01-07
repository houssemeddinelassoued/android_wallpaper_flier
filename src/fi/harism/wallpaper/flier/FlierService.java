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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

/**
 * Main wallpaper service class.
 */
public final class FlierService extends WallpaperService {

	@Override
	public Engine onCreateEngine() {
		return new WallpaperEngine();
	}

	/**
	 * Private wallpaper engine implementation.
	 */
	private final class WallpaperEngine extends Engine {

		// Slightly modified GLSurfaceView.
		private WallpaperGLSurfaceView mGLSurfaceView;
		private FlierRenderer mRenderer;

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {

			// Uncomment for debugging.
			// android.os.Debug.waitForDebugger();

			super.onCreate(surfaceHolder);
			mRenderer = new FlierRenderer(FlierService.this);
			mGLSurfaceView = new WallpaperGLSurfaceView(FlierService.this);
			mGLSurfaceView.setEGLContextClientVersion(2);
			mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 0, 0, 0);
			mGLSurfaceView.setRenderer(mRenderer);
			mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mGLSurfaceView.onDestroy();
			mGLSurfaceView = null;
			mRenderer = null;
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep,
					xPixelOffset, yPixelOffset);
			mRenderer.setXOffset(xOffset);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);
			if (visible) {
				mGLSurfaceView.onResume();
			} else {
				mGLSurfaceView.onPause();
			}
		}

		/**
		 * Lazy as I am, I din't bother using GLWallpaperService (found on
		 * GitHub) project for wrapping OpenGL functionality into my wallpaper
		 * service. Instead am using GLSurfaceView and trick it into hooking
		 * into Engine provided SurfaceHolder instead of SurfaceView provided
		 * one GLSurfaceView extends.
		 */
		private final class WallpaperGLSurfaceView extends GLSurfaceView {
			public WallpaperGLSurfaceView(Context context) {
				super(context);
			}

			@Override
			public SurfaceHolder getHolder() {
				return WallpaperEngine.this.getSurfaceHolder();
			}

			/**
			 * Should be called once underlying Engine is destroyed. Calling
			 * onDetachedFromWindow() will stop rendering thread which is lost
			 * otherwise.
			 */
			public void onDestroy() {
				super.onDetachedFromWindow();
			}
		}
	}
}
