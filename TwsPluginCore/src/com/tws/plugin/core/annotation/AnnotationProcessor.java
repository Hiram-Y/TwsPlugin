package com.tws.plugin.core.annotation;

public class AnnotationProcessor {

    public static PluginContainer getPluginContainer(Class clazz) {
        PluginContainer container = (PluginContainer)clazz.getAnnotation(PluginContainer.class);
        return container;
    }

}
