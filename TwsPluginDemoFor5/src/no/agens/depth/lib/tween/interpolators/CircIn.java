package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class CircIn implements TimeInterpolator {

  @Override public float getInterpolation(float t) {
    return (float) Math.sqrt(1f - t * t);
  }
}
