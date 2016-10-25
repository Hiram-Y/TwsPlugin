package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class ExpoIn implements TimeInterpolator {
  @Override public float getInterpolation(float t) {
    return (t == 0) ? 0 : (float) Math.pow(2, 10 * (t - 1));
  }
}