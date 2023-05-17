package dev.waterdog.waterdogpe.network.protocol.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleBlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.common.Definition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;

public class FakeDefinitionRegistry<D extends Definition> implements DefinitionRegistry<D> {

    public static FakeDefinitionRegistry<BlockDefinition> createBlockRegistry() {
        return new FakeDefinitionRegistry<>(rid -> new SimpleBlockDefinition("unknown", rid, null));
    }

    public static FakeDefinitionRegistry<ItemDefinition> createItemRegistry() {
        return new FakeDefinitionRegistry<>(rid -> new SimpleItemDefinition("unknown", rid, false));
    }

    private final Int2ObjectMap<D> runtimeMap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectFunction<D> factory;

    public FakeDefinitionRegistry(Int2ObjectFunction<D> factory) {
        this.factory = factory;
    }

    public D getDefinition(String identifier) {
        return null;
    }

    @Override
    public D getDefinition(int runtimeId) {
        D definition = this.runtimeMap.get(runtimeId);
        if (definition == null) {
            this.runtimeMap.put(runtimeId, definition = this.factory.get(runtimeId));
        }
        return definition;
    }

    @Override
    public boolean isRegistered(D definition) {
        return this.runtimeMap.get(definition.getRuntimeId()) == definition;
    }
}
