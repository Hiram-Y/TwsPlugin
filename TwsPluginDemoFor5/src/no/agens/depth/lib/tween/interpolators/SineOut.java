package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class SineOut implements TimeInterpolator {
  @Override public float getInterpolation(float t) {
    return (float) Math.sin(t * (Math.PI / 2));
  }
}