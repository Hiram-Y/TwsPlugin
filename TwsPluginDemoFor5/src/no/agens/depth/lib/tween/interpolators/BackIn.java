package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class BackIn implements TimeInterpolator {
  protected float param_s = 1.70158f;

  @Override public float getInterpolation(float t) {
    float s = param_s;
    return t * t * ((s + 1) * t - s);
  }

  public BackIn amount(float s) {
    param_s = s;
    return this;
  }
}