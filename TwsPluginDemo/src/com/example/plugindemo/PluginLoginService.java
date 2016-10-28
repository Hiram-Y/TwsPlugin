package com.example.plugindemo;

import com.example.pluginbase.ILoginService;
import com.example.pluginbase.LoginVO;


/**
 * @author yongchen
 */
public class PluginLoginService implements ILoginService {

    @Override
    public LoginVO login(String username, String password) {
        if ("admin".equals(username) && "123456".equals(password)) {
            LoginVO loginVO = new LoginVO();
            loginVO.setUsername(username);
            loginVO.setPassword(password);
            return loginVO;
        }
        return null;
    }

    @Override
    public boolean logout(String username) {
        if ("admin".equals(username)) {
            return true;
        }
        return false;
    }
}
