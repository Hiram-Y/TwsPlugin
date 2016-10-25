package com.tencent.tws.assistant.annotation;

public class AnnotationProcessor {

    public static PluginContainer getPluginContainer(Class clazz) {
        PluginContainer container = (PluginContainer)clazz.getAnnotation(PluginContainer.class);
        return container;
    }

}
