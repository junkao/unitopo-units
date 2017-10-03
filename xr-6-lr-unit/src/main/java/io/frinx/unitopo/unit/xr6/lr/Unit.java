/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.lr;

import com.google.common.collect.Sets;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.frinx.unitopo.registry.api.TranslationUnitCollector;
import io.frinx.unitopo.registry.spi.TranslateUnit;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import io.frinx.unitopo.unit.xr6.lr.handler.NextHopReader;
import io.frinx.unitopo.unit.xr6.lr.handler.StaticRouteReader;
import java.util.Set;
import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top.StaticRoutes;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.NextHops;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unit implements TranslateUnit {

    private static final Logger LOG = LoggerFactory.getLogger(Unit.class);
    private static final InstanceIdentifier<Protocol> BASE_IID = InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class).child(Protocols.class).child(Protocol.class);

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
        return Sets.newHashSet(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.$YangModuleInfoImpl.getInstance());
    }

    @Override
    public Set<YangModuleInfo> getUnderlayYangSchemas() {
        return Sets.newHashSet(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.$YangModuleInfoImpl.getInstance());
    }

    @Override
    public Set<RpcService<?, ?>> getRpcs(UnderlayAccess context) {
        return Sets.newHashSet();
    }

    @Override
    public void provideHandlers(ModifiableReaderRegistryBuilder rRegistry, ModifiableWriterRegistryBuilder wRegistry,
            UnderlayAccess access) {
        provideReaders(rRegistry, access);
        provideWriters(wRegistry, access);
    }

    private void provideWriters(ModifiableWriterRegistryBuilder wRegistry, UnderlayAccess access) {
        // no-op
    }

    private void provideReaders(@Nonnull ModifiableReaderRegistryBuilder rRegistry, UnderlayAccess access) {
        rRegistry.addStructuralReader(BASE_IID.child(StaticRoutes.class), StaticRoutesBuilder.class);
        rRegistry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Static.class).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.Config.class),
                InstanceIdentifier.create(Static.class).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.State.class)),
                new GenericListReader<>(BASE_IID.child(StaticRoutes.class).child(Static.class), new StaticRouteReader(access)));
        rRegistry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(NextHop.class).child(Config.class), InstanceIdentifier.create(NextHop.class).child(State.class)),
                new GenericListReader<>(BASE_IID.child(StaticRoutes.class).child(Static.class).child(NextHops.class).child(NextHop.class), new NextHopReader(access)));
    }

    @Override
    public String toString() {
        return "xr6-lr-unit";
    }

}
