package no.agens.depth.lib.tween.interpolators;

import android.animation.TimeInterpolator;

/**
 * @author yongchen
 */
public class BackInOut implements TimeInterpolator {
  protected float param_s = 1.70158f;

  @Override public float getInterpolation(float t) {
    float s = param_s;
    if ((t *= 2) < 1) return 0.5f * (t * t * (((s *= (1.525f)) + 1) * t - s));
    return 0.5f * ((t -= 2) * t * (((s *= (1.525f)) + 1) * t + s) + 2);
  }

  public BackInOut amount(float s) {
    param_s = s;
    return this;
  }
}