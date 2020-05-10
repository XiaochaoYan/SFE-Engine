package com.sfengine.components.contexts;

import com.sfengine.core.context.ContextDictionary;
import com.sfengine.core.context.ContextFactoryProvider;
import com.sfengine.core.context.PhysicalDeviceContextFactory;

import java.util.Dictionary;

public class BasicPhysicalDeviceContextFactory {

    public static BasicPhysicalDeviceContext createPhysicalDeviceContext(String name, ContextDictionary dict) {
        PhysicalDeviceContextFactory factory =
                ContextFactoryProvider.getFactory(
                        PhysicalDeviceContextFactory.CONTEXT_IDENTIFIER,
                        PhysicalDeviceContextFactory.class);

        BasicPhysicalDeviceContext context =
                new BasicPhysicalDeviceContext(name, PhysicalDeviceContextFactory.CONTEXT_IDENTIFIER);
        factory.putContext(context);
        return context;
    }

}
