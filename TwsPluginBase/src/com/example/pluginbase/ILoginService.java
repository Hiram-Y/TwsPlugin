package com.example.pluginbase;

import java.io.Serializable;

public interface ILoginService extends Serializable {

    public LoginVO login(String username, String password);

    public boolean logout(String username);

}
