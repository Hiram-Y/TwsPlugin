package com.tencent.tws.sharelib;

import java.io.Serializable;

/**
 * @author yongchen
 */
public interface ShareService extends Serializable {

    public SharePOJO doSomething(String condition);

}
