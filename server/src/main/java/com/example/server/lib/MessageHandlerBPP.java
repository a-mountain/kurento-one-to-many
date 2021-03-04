package com.example.server.lib;

import com.example.server.lib.annotations.WebsocketHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class MessageHandlerBPP implements BeanPostProcessor {

    private final MessageHandlersIntrospector introspector;

    @Autowired
    public MessageHandlerBPP(MessageDispatcher dispatcher) {
        this.introspector = new MessageHandlersIntrospector(dispatcher);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        var type = bean.getClass();
        if(type.isAnnotationPresent(WebsocketHandler.class)) {
            introspector.introspect(bean, type);
        }
        return bean;
    }
}
