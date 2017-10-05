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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.AreaAddresses;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospf.types.rev170228.OspfAreaIdentifier;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.structure.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.AreasBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.AreaKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class AreaReader implements ListReaderCustomizer<Area, AreaKey, AreaBuilder> {

    private UnderlayAccess access;

    public AreaReader(UnderlayAccess access) {
        this.access = access;
    }

    @Nonnull
    @Override
    public List<AreaKey> getAllIds(@Nonnull InstanceIdentifier<Area> id, @Nonnull ReadContext context) throws ReadFailedException {
        AreaAddresses adr = getAreas(id, access);
        List<AreaKey> keys = new ArrayList<>();
        adr.getAreaAreaId().stream().forEach(a -> keys.add(new AreaKey(new OspfAreaIdentifier(Long.valueOf(a.getAreaId())))));
        return keys;
    }

    public static AreaAddresses getAreas(InstanceIdentifier<?> id, UnderlayAccess access) {
        NetworkInstanceKey vrfName = id.firstKeyOf(NetworkInstance.class);
        Process p = Ospfv2GlobalReader.getProcess(id, access);
        AreaAddresses adr = null;
        if (Ospfv2GlobalReader.DEFAULT_VRF.equals(vrfName.getName())) {
            adr = p.getDefaultVrf().getAreaAddresses();
        } else {
            adr = p.getVrfs().getVrf().stream().filter(n -> vrfName.getName().equals(n.getVrfName().getValue())).findFirst().get().getAreaAddresses();
        }
        return adr;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Area> readData) {
        ((AreasBuilder) builder).setArea(readData);
    }

    @Nonnull
    @Override
    public AreaBuilder getBuilder(@Nonnull InstanceIdentifier<Area> id) {
        return new AreaBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Area> id, @Nonnull AreaBuilder builder, @Nonnull ReadContext ctx) throws ReadFailedException {
        AreaKey key = id.firstKeyOf(Area.class);
        builder.setIdentifier(key.getIdentifier());
        builder.setConfig(new ConfigBuilder().setIdentifier(key.getIdentifier()).build());
        builder.setState(new StateBuilder().setIdentifier(key.getIdentifier()).build());
    }
}
