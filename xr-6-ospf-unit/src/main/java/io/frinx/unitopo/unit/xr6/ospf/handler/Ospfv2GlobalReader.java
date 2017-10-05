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
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.Ospfv2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

public class Ospfv2GlobalReader implements ReaderCustomizer<Global, GlobalBuilder> {

    // TODO: move to a more appropriate place
    public static final String DEFAULT_VRF = "default";

    private UnderlayAccess access;

    public Ospfv2GlobalReader(UnderlayAccess access) {
        this.access = access;
    }

    @Nonnull
    @Override
    public GlobalBuilder getBuilder(@Nonnull InstanceIdentifier<Global> id) {
        return new GlobalBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Global> id, @Nonnull GlobalBuilder builder, @Nonnull ReadContext ctx) {
        NetworkInstanceKey vrfName = id.firstKeyOf(NetworkInstance.class);
        Process p = getProcess(id, access);
        String routerId = "";
        if (DEFAULT_VRF.equals(vrfName.getName())) {
            routerId = p.getDefaultVrf().getRouterId().getValue();
        } else {
            routerId = p.getVrfs().getVrf().stream().filter(n -> vrfName.getName().equals(n.getVrfName().getValue())).findFirst().get().getRouterId().getValue();
        }
        builder.setConfig(new ConfigBuilder().setRouterId(new DottedQuad(routerId)).build());
        builder.setState(new StateBuilder().setRouterId(new DottedQuad(routerId)).build());
    }

    public static Process getProcess(@Nonnull InstanceIdentifier<?> id, UnderlayAccess access) {
        ProtocolKey name = id.firstKeyOf(Protocol.class);
        try {
            Processes processes = access.read(InstanceIdentifier.create(Ospf.class).child(Processes.class)).checkedGet().orNull();
            Process p = processes.getProcess()
                    .stream()
                    .filter(pn -> pn.getProcessName().getValue().equals(name.getName()))
                    .findFirst()
                    .get();
            return p;
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull Global readValue) {
        ((Ospfv2Builder) parentBuilder).setGlobal(readValue);
    }

    @Override
    public boolean isPresent(InstanceIdentifier<Global> id, Global built, ReadContext ctx) throws ReadFailedException {
        return false;
    }
}
