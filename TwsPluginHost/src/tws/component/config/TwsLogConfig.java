package tws.component.config;

import tws.component.log.TwsLogBaseConfig;

/**
 * Title: QRomLogConfig Package: qrom.component.log Author: interzhang Date:
 * 14-3-18 下午3:57 Version: v1.0
 */
public class TwsLogConfig extends TwsLogBaseConfig {

	@Override
	public int getLogMode() {
		return TwsLogBaseConfig.LOG_BOTH;
	}

	@Override
	public String getPackageName() {
		return "com.example.pluginhost";
	}

	// @Override
	// public void initTraceModules() {
	// // TODO Auto-generated method stub
	//
	// }

	// @Override
	// public boolean isForceTrace() {
	// // TODO Auto-generated method stub
	// return false;
	// }

}