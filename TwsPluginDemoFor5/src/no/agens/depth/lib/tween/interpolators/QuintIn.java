package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class QuintIn implements TimeInterpolator {
  @Override public float getInterpolation(float t) {
    return t * t * t * t * t;
  }
}