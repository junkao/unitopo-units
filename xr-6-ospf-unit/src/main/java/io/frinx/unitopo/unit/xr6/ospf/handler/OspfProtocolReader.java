/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.ospf.handler;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev160512.OSPF;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class OspfProtocolReader implements ListReaderCustomizer<Protocol, ProtocolKey, ProtocolBuilder> {

    private UnderlayAccess access;

    private static final InstanceIdentifier<Processes> IID = InstanceIdentifier.create(Ospf.class).child(Processes.class);
    private static final Class<OSPF> TYPE = OSPF.class;

    public OspfProtocolReader(UnderlayAccess access) {
        this.access = access;
    }

    @Nonnull
    @Override
    public List<ProtocolKey> getAllIds(@Nonnull InstanceIdentifier<Protocol> id, @Nonnull ReadContext context) throws ReadFailedException {
        List<ProtocolKey> keys = new ArrayList<>();
        try {
            Processes bgp = access.read(IID).checkedGet().orNull();
            bgp.getProcess().stream().forEach(ins -> keys.add(new ProtocolKey(TYPE, ins.getProcessName().getValue())));
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            e.printStackTrace();
        }
        return keys;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Protocol> readData) {
        ((ProtocolsBuilder) builder).setProtocol(readData);
    }

    @Nonnull
    @Override
    public ProtocolBuilder getBuilder(@Nonnull InstanceIdentifier<Protocol> id) {
        return new ProtocolBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Protocol> id, @Nonnull ProtocolBuilder builder, @Nonnull ReadContext ctx) throws ReadFailedException {
        ProtocolKey key = id.firstKeyOf(Protocol.class);
        if (key.getIdentifier().equals(TYPE)) {
            builder.setName(key.getName());
            builder.setIdentifier(key.getIdentifier());
        }
    }
}
