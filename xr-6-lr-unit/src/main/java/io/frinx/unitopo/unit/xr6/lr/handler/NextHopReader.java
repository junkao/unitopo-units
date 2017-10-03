/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.frinx.unitopo.registry.spi.UnderlayAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.RouterStatic;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.AddressFamily;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.DefaultVrf;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.Vrfs;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.vrfs.Vrf;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.vrfs.VrfKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.next.hop.VRFNEXTHOPCONTENT;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.prefix.table.vrf.prefixes.VrfPrefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.route.vrf.route.VrfNextHopTable;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.NextHopsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHop;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.NextHopKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.next.hops.next.hop.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NextHopReader implements ListReaderCustomizer<NextHop, NextHopKey, NextHopBuilder> {

    // TODO: move to a more appropriate place
    private static final String DEFAULT_VRF = "default";

    private UnderlayAccess access;

    public NextHopReader(UnderlayAccess access) {
        this.access = access;
    }

    @Override
    public NextHopBuilder getBuilder(InstanceIdentifier<NextHop> id) {
        return new NextHopBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<NextHop> id, NextHopBuilder builder, ReadContext ctx) throws ReadFailedException {
        ProtocolKey protKey = id.firstKeyOf(Protocol.class);
        if (!protKey.getIdentifier().equals(StaticProtocolReader.TYPE)) {
            return;
        }
        NextHopKey key = id.firstKeyOf(NextHop.class);
        builder.setIndex(key.getIndex());

        VrfNextHopTable table = parseNextHopTable(id);

        final ConfigBuilder cBuilder = new ConfigBuilder();
        final StateBuilder sBuilder = new StateBuilder();
        cBuilder.setIndex(key.getIndex());
        sBuilder.setIndex(key.getIndex());

        VRFNEXTHOPCONTENT content = null;
        if (table.getVrfNextHopNextHopAddress() != null) {
            content = table.getVrfNextHopNextHopAddress().stream().filter(f -> createComplexKey(null, f.getNextHopAddress()).equals(key.toString())).findFirst().get();
        }
        if (table.getVrfNextHopInterfaceName() != null) {
           content = table.getVrfNextHopInterfaceName().stream().filter(f -> f.getInterfaceName().getValue().equals(key.toString())).findFirst().get();
        }
        if (table.getVrfNextHopInterfaceNameNextHopAddress() != null) {
            content = table.getVrfNextHopInterfaceNameNextHopAddress().stream().filter(f -> createComplexKey(f.getInterfaceName().getValue(), f.getNextHopAddress()).equals(key.toString())).findFirst().get();
        }
        if (content != null) {
            cBuilder.setMetric(content.getMetric());
            sBuilder.setMetric(content.getMetric());
        }
        builder.setConfig(cBuilder.build());
        builder.setState(sBuilder.build());
    }

    private VrfNextHopTable parseNextHopTable(InstanceIdentifier<NextHop> id) {
        final StaticKey routeKey = id.firstKeyOf(Static.class);
        final NetworkInstanceKey vrfKey = id.firstKeyOf(NetworkInstance.class);
        InstanceIdentifier<AddressFamily> iid;

        List<NextHopKey> keys = new ArrayList<>();
        if (DEFAULT_VRF.equals(vrfKey.getName())) {
            iid = InstanceIdentifier.create(RouterStatic.class).child(DefaultVrf.class).child(AddressFamily.class);
        } else {
            iid = InstanceIdentifier.create(RouterStatic.class).child(Vrfs.class).child(Vrf.class, new VrfKey(new CiscoIosXrString(vrfKey.getName()))).child(AddressFamily.class);
        }
        VrfPrefix prefix = null;
        try {
            AddressFamily af = access.read(iid).checkedGet().orNull();
            if (routeKey.getPrefix().getIpv4Prefix().getValue() != null) {
                // prefix can't be in unicast at the same time as in multicast, so if we didn't find it in unicast, let's check multicast
                Optional<VrfPrefix> maybePref = af.getVrfipv4().getVrfUnicast().getVrfPrefixes().getVrfPrefix().stream()
                        .filter(vrf -> vrf.getPrefix().getIpv4AddressNoZone().getValue().equals(routeKey.getPrefix().getIpv4Prefix().getValue())).findFirst();
                if (!maybePref.isPresent()) {
                    maybePref = af.getVrfipv4().getVrfMulticast().getVrfPrefixes().getVrfPrefix().stream()
                            .filter(vrf -> vrf.getPrefix().getIpv4AddressNoZone().getValue().equals(routeKey.getPrefix().getIpv4Prefix().getValue())).findFirst();
                }
                prefix = maybePref.get();
            } else {
                // prefix can't be in unicast at the same time as in multicast, so if we didn't find it in unicast, let's check multicast
                Optional<VrfPrefix> maybePref = af.getVrfipv6().getVrfUnicast().getVrfPrefixes().getVrfPrefix().stream()
                        .filter(vrf -> vrf.getPrefix().getIpv6AddressNoZone().getValue().equals(routeKey.getPrefix().getIpv6Prefix().getValue())).findFirst();
                if (!maybePref.isPresent()) {
                    maybePref = af.getVrfipv6().getVrfMulticast().getVrfPrefixes().getVrfPrefix().stream()
                            .filter(vrf -> vrf.getPrefix().getIpv6AddressNoZone().getValue().equals(routeKey.getPrefix().getIpv6Prefix().getValue())).findFirst();
                }
                prefix = maybePref.get();
            }
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            e.printStackTrace();
        }
        return prefix.getVrfRoute().getVrfNextHopTable();
    }

    @Override
    public List<NextHopKey> getAllIds(InstanceIdentifier<NextHop> id, ReadContext context) throws ReadFailedException {
        VrfNextHopTable table = parseNextHopTable(id);

        List<NextHopKey> keys = new ArrayList<>();
        ProtocolKey protKey = id.firstKeyOf(Protocol.class);
        if (!protKey.getIdentifier().equals(StaticProtocolReader.TYPE)) {
            return keys;
        }
        // only interface
        if (table.getVrfNextHopInterfaceName() != null) {
            table.getVrfNextHopInterfaceName().stream().forEach(name -> keys.add(new NextHopKey(name.getInterfaceName().getValue())));
        }
        // interface + nexthop
        if (table.getVrfNextHopInterfaceNameNextHopAddress() != null) {
            table.getVrfNextHopInterfaceNameNextHopAddress().stream().forEach(name -> keys.add(createComplexKey(name.getInterfaceName().getValue(), name.getNextHopAddress())));
        }
        // only next hop
        if (table.getVrfNextHopNextHopAddress() != null) {
            table.getVrfNextHopNextHopAddress().stream().forEach(name -> keys.add(createComplexKey(null, name.getNextHopAddress())));
        }
        return keys;
    }

    private static NextHopKey createComplexKey(final String interfaceName, final IpAddressNoZone nextHop) {
        final StringBuilder builder = new StringBuilder();
        builder.append(nextHop.getIpv4AddressNoZone() != null ?
            nextHop.getIpv4AddressNoZone().getValue() : nextHop.getIpv6AddressNoZone().getValue());
        builder.append(" ");
        builder.append(interfaceName != null ? interfaceName : "");
        return new NextHopKey(builder.toString());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<NextHop> readData) {
        ((NextHopsBuilder) builder).setNextHop(readData);
    }
}
