/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.ospf;

import com.google.common.collect.Sets;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.frinx.unitopo.registry.api.TranslationUnitCollector;
import io.frinx.unitopo.registry.spi.TranslateUnit;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import java.util.Set;
import javax.annotation.Nonnull;

import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceReader;
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaReader;
import io.frinx.unitopo.unit.xr6.ospf.handler.Ospfv2GlobalReader;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.Interfaces;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.Areas;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unit implements TranslateUnit {

    private static final Logger LOG = LoggerFactory.getLogger(Unit.class);
    public static final InstanceIdentifier<Protocol> BASE_IID = InstanceIdentifier.create(NetworkInstances.class).child(NetworkInstance.class)
            .child(Protocols.class).child(Protocol.class);

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
        return Sets.newHashSet(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospf.types.rev170228.$YangModuleInfoImpl.getInstance());
    }

    @Override
    public Set<YangModuleInfo> getUnderlayYangSchemas() {
        return Sets.newHashSet(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.$YangModuleInfoImpl.getInstance());
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
        rRegistry.addStructuralReader(BASE_IID.child(Ospfv2.class), Ospfv2Builder.class);
        rRegistry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Global.class).child(Config.class), InstanceIdentifier.create(Global.class).child(State.class)),
                new GenericReader<>(BASE_IID.child(Ospfv2.class).child(Global.class), new Ospfv2GlobalReader(access)));
        rRegistry.addStructuralReader(BASE_IID.child(Ospfv2.class).child(Areas.class), AreasBuilder.class);
        rRegistry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Area.class).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.Config.class),
                InstanceIdentifier.create(Area.class).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.State.class)),
                new GenericListReader<>(BASE_IID.child(Ospfv2.class).child(Areas.class).child(Area.class), new AreaReader(access)));
        rRegistry.addStructuralReader(BASE_IID.child(Ospfv2.class).child(Areas.class).child(Area.class).child(Interfaces.class), InterfacesBuilder.class);
        rRegistry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Interface.class).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config.class),
                InstanceIdentifier.create(Interface.class).child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.State.class)),
                new GenericListReader<>(BASE_IID.child(Ospfv2.class).child(Areas.class).child(Area.class).child(Interfaces.class).child(Interface.class), new AreaInterfaceReader(access)));
    }

    @Override
    public String toString() {
        return "xr6-ospf-unit";
    }

}
