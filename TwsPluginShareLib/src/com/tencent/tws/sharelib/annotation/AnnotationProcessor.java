package com.tencent.tws.sharelib.annotation;

public class AnnotationProcessor {

    public static PluginContainer getPluginContainer(Class clazz) {
        PluginContainer container = (PluginContainer)clazz.getAnnotation(PluginContainer.class);
        return container;
    }

}
