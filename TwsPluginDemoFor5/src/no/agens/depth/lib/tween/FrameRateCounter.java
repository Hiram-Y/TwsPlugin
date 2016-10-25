package no.agens.depth.lib.tween;

import android.os.SystemClock;

/**
 * @author yongchen
 */

public class FrameRateCounter {
  private static long mLastTime;

  public static float timeStep() {
    final long time = SystemClock.uptimeMillis();
    final long timeDelta = time - mLastTime;
    float timeDeltaSeconds = mLastTime > 0.0f ? timeDelta / 1000.0f : 0.0f;
    mLastTime = time;
    return Math.min(0.021f, timeDeltaSeconds);
  }
}
