package com.example.server.lib;

import com.example.server.lib.annotations.MessageHandler;
import com.example.server.messages.Message;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class MessageHandlersIntrospector {

    private final MessageDispatcher dispatcher;

    public MessageHandlersIntrospector(MessageDispatcher dispatcher) {

        this.dispatcher = dispatcher;
    }

    public void introspect(Object object, Class<?> type) {
        var declaredMethods = ReflectionUtils.getDeclaredMethods(type);
        Arrays.stream(declaredMethods)
                .filter(this::isMessageHandler)
                .forEach(method -> addHandler(method, object));
    }

    private boolean isMessageHandler(Method method) {
        return method.isAnnotationPresent(MessageHandler.class);
    }

    private <T> void addHandler(Method method, T object) {
        method.setAccessible(true);
        Class<Message> type = getMessageType(method);
        com.example.server.lib.MessageHandler<Message> handler = (message, session) -> {
            try {
                method.invoke(object, message, session);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        };
        dispatcher.addHandler(type, handler);
    }

    private Class<Message> getMessageType(Method method) {
        var parameterTypes = method.getParameterTypes();
        return ( Class<Message>) parameterTypes[0];
    }
}
