/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp;

import com.google.common.collect.Sets;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.frinx.unitopo.registry.api.TranslationUnitCollector;
import io.frinx.unitopo.registry.spi.TranslateUnit;
import io.frinx.unitopo.registry.spi.UnderlayAccess;

import java.util.Set;
import javax.annotation.Nonnull;

import io.frinx.unitopo.unit.xr6.bgp.handler.BgpGlobalConfigReader;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unit implements TranslateUnit {

    private static final Logger LOG = LoggerFactory.getLogger(Unit.class);
    private static final InstanceIdentifier<Protocol> BASE_IID = InstanceIdentifier.create(NetworkInstances.class).child(NetworkInstance.class)
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
        return Sets.newHashSet(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.$YangModuleInfoImpl.getInstance());
    }

    @Override
    public Set<YangModuleInfo> getUnderlayYangSchemas() {
        return Sets.newHashSet(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.$YangModuleInfoImpl.getInstance());
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
        rRegistry.addStructuralReader(BGP_IID, BgpBuilder.class);
        rRegistry.addStructuralReader(BGP_GLOBAL_IID, GlobalBuilder.class);
        rRegistry.add(new GenericReader<>(BGP_GLOBAL_CONFIG_IID, new BgpGlobalConfigReader(access)));
        // TODO: state reader?
    }

    protected static final InstanceIdentifier<Bgp> BGP_IID = BASE_IID.child(Bgp.class);
    protected static final InstanceIdentifier<Global> BGP_GLOBAL_IID = BGP_IID.child(Global.class);
    protected static final InstanceIdentifier<Config> BGP_GLOBAL_CONFIG_IID = BGP_GLOBAL_IID.child(Config.class);


    @Override
    public String toString() {
        return "xr6-bgp-unit";
    }

}
