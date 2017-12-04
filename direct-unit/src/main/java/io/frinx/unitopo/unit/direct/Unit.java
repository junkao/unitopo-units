/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.direct;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.frinx.cli.registry.common.GenericTranslateContext;
import io.frinx.unitopo.registry.api.TranslationUnitCollector;
import io.frinx.unitopo.registry.spi.TranslateUnit;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.$YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class Unit implements TranslateUnit {

    private static final Set<YangModuleInfo> ALL_OPENCONFIGS = Sets.newHashSet(
            $YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.$YangModuleInfoImpl.getInstance());

    // This is a list of supported top level nodes by the direct unit
    // which requires manual modification when another subtree should be supported
    // TODO extract configuration (list of root nodes that are supported)
    private static final Set<String> SUPPORTED_TOP_LEVEL_NODES = Sets.newHashSet("interfaces", "network-instances");

    private static final SchemaContext SCHEMA_CONTEXT;
    private static final BindingToNormalizedNodeCodec CODEC;
    private static final List<? extends InstanceIdentifier<?>> IDS;

    static {
        ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(ALL_OPENCONFIGS);
        SCHEMA_CONTEXT = moduleInfoBackedContext.getSchemaContext();
        CODEC = GenericTranslateContext.getCodec(moduleInfoBackedContext, SCHEMA_CONTEXT);
        IDS = SCHEMA_CONTEXT.getDataDefinitions().stream()
                // only handle openconfig models
                // TODO extract regex to match namespaces into configuration
                .filter(id -> id.getQName().getNamespace().toString().contains("openconfig"))
                // only handle declared root nodes
                .filter(id -> SUPPORTED_TOP_LEVEL_NODES.contains(id.getQName().getLocalName()))
                .flatMap(Unit::getAllIds)
                .map(Unit::fromNormalized)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all instance identifiers recursively.
     * Handles only complex nodes (containers, lists, augments) to later produce InstanceIds compatible
     * with HC's translate framework.
     */
    private static Stream<YangInstanceIdentifier> getAllIds(DataSchemaNode dataSchemaNode) {
        List<YangInstanceIdentifier> all = new LinkedList<>();

        List<YangInstanceIdentifier.PathArgument> pArg;
        if (dataSchemaNode instanceof AugmentationSchema) {
            pArg = Collections.singletonList(new YangInstanceIdentifier.AugmentationIdentifier(
                    ((AugmentationSchema) dataSchemaNode).getChildNodes().stream()
                            .map(SchemaNode::getQName)
                            .collect(Collectors.toSet())));
        } else if (dataSchemaNode.isAugmenting()) {
            // Skip nodes from augment, they are handled in previous block
            return Stream.empty();
        } else if (dataSchemaNode instanceof ListSchemaNode) {
            YangInstanceIdentifier.NodeIdentifier listId =
                    new YangInstanceIdentifier.NodeIdentifier(dataSchemaNode.getQName());
            pArg = Lists.newArrayList(listId, listId);
        } else if (dataSchemaNode instanceof DataNodeContainer) {
            pArg = Collections.singletonList(new YangInstanceIdentifier.NodeIdentifier(dataSchemaNode.getQName()));
        } else {
            // Only collecting complex nodes
            return Stream.empty();
        }

        YangInstanceIdentifier current = YangInstanceIdentifier.create(pArg);
        all.add(YangInstanceIdentifier.create(pArg));

        ((DataNodeContainer) dataSchemaNode).getChildNodes().stream()
                .flatMap(Unit::getAllIds)
                .map(id -> Iterables.concat(current.getPathArguments(), id.getPathArguments()))
                .map(YangInstanceIdentifier::create)
                .forEach(all::add);

        return all.stream();
    }

    private static InstanceIdentifier<? extends DataObject> fromNormalized(YangInstanceIdentifier id) {
        // TODO fix org.opendaylight.yangtools.binding.data.codec.impl.InstanceIdentifierCodec
        // by removing the check for unkeyed list that returns null
        // InstanceIdentifier can handle unkeyed list just fine
        // upstream the fix and remove this, use just CODEC::fromYangInstanceIdentifier

        BindingCodecTree codecContext = CODEC.getCodecFactory().getCodecContext();
        try {
            Method getCodecContextNodeMeth = codecContext.getClass()
                    .getDeclaredMethod("getCodecContextNode", YangInstanceIdentifier.class, Collection.class);
            getCodecContextNodeMeth.setAccessible(true);
            final List<InstanceIdentifier.PathArgument> builder = new ArrayList<>();
            getCodecContextNodeMeth.invoke(codecContext, id, builder);
            return InstanceIdentifier.create(builder);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to transform instance identifier: " + id, e);
        }
    }

    private final TranslationUnitCollector registry;
    private TranslationUnitCollector.Registration reg;

    public Unit(@Nonnull final TranslationUnitCollector registry) {
        this.registry = registry;
    }

    public void init() {
        reg = registry.registerTranslateUnit(this);
    }

    public void close() {
        if (reg != null) {
            reg.close();
        }
    }

    @Override
    public Set<YangModuleInfo> getYangSchemas() {
        return ALL_OPENCONFIGS;
    }

    @Override
    public Set<YangModuleInfo> getUnderlayYangSchemas() {
        return ALL_OPENCONFIGS;
    }

    @Override
    public Set<RpcService<?, ?>> getRpcs(@Nonnull final UnderlayAccess underlayAccess) {
        return Collections.emptySet();
    }

    @Override
    public void provideHandlers(@Nonnull final ModifiableReaderRegistryBuilder rRegistry,
                                @Nonnull final ModifiableWriterRegistryBuilder wRegistry,
                                @Nonnull final UnderlayAccess underlayAccess) {
        // FIXME doesn't work with top level lists ??
        // FIXME readers cannot be extended !!! or can they ? is it enough to just add schema from separate unit ??? TEST!

        IDS.forEach(id -> {
            // Only registering the root readers, no need to register each node, since readers are stored in a tree
            // structure and the DirectReader is capable of handling its child nodes as well
            if (Iterables.size(id.getPathArguments()) == 1) {
                Set<InstanceIdentifier<?>> subtrees = IDS.stream()
                        .filter(childId -> Iterables.size(childId.getPathArguments()) > 1)
                        .filter(childId -> Iterables.get(childId.getPathArguments(), 0).equals(Iterables.get(id.getPathArguments(), 0)))
                        .collect(Collectors.toSet());
                rRegistry.subtreeAdd(subtrees, new DirectReader(underlayAccess, id));
            }

            // Writers are stored in a flat structure, register all the nodes
            wRegistry.add(new DirectWriter(underlayAccess, id));
        });
    }

    @Override
    public String toString() {
        return "Direct translate unit";
    }

}
