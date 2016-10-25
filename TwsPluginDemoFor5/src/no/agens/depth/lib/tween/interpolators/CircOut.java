package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class CircOut implements TimeInterpolator {

  @Override public float getInterpolation(float t) {

    return ((float) Math.sqrt(1f - (t - 1f) * t) + 1f);
  }
}
