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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.RouterStatic;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.AddressFamily;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.address.family.Vrfipv4;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.address.family.Vrfipv6;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.router._static.vrfs.Vrf;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.multicast.VrfMulticast;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.prefix.table.VrfPrefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.prefix.table.vrf.prefixes.VrfPrefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.unicast.VrfUnicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes._static.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.IpPrefix;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv6Prefix;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class StaticRouteReader implements ListReaderCustomizer<Static, StaticKey, StaticBuilder> {

    // TODO: move to a more appropriate place
    private static final String DEFAULT_VRF = "default";

    private UnderlayAccess access;

    public StaticRouteReader(UnderlayAccess access) {
        this.access = access;
    }

    @Override
    public List<StaticKey> getAllIds(InstanceIdentifier<Static> id, ReadContext context) throws ReadFailedException {
        InstanceIdentifier<RouterStatic> rsId = InstanceIdentifier.create(RouterStatic.class);
        List<StaticKey> keys = new ArrayList<>();
        ProtocolKey protKey = id.firstKeyOf(Protocol.class);
        if (!protKey.getIdentifier().equals(StaticProtocolReader.TYPE)) {
            return keys;
        }
        try {
            RouterStatic rs = access.read(rsId).checkedGet().orNull();
            final String vrfName = id.firstKeyOf(NetworkInstance.class).getName();
            if (DEFAULT_VRF.equals(vrfName)) {
             // default vrf
                AddressFamily fam = rs.getDefaultVrf().getAddressFamily();
                if (fam.getVrfipv4() != null  && fam.getVrfipv4().getVrfUnicast() != null && fam.getVrfipv4().getVrfUnicast().getVrfPrefixes() != null) {
                    findKeys(keys, fam.getVrfipv4().getVrfUnicast().getVrfPrefixes());
                }
                if (fam.getVrfipv4() != null  && fam.getVrfipv4().getVrfMulticast() != null && fam.getVrfipv4().getVrfMulticast().getVrfPrefixes() != null) {
                    findKeys(keys, fam.getVrfipv4().getVrfMulticast().getVrfPrefixes());
                }
                if (fam.getVrfipv6() != null  && fam.getVrfipv6().getVrfUnicast() != null && fam.getVrfipv6().getVrfUnicast().getVrfPrefixes() != null) {
                    findKeys(keys, fam.getVrfipv6().getVrfUnicast().getVrfPrefixes());
                }
                if (fam.getVrfipv6() != null  && fam.getVrfipv6().getVrfMulticast() != null && fam.getVrfipv6().getVrfMulticast().getVrfPrefixes() != null) {
                    findKeys(keys, fam.getVrfipv6().getVrfMulticast().getVrfPrefixes());
                }
            } else {
                Stream<AddressFamily> family = rs.getVrfs().getVrf().stream().filter(vrf -> vrf.getVrfName().getValue().equals(vrfName)).map(Vrf::getAddressFamily);

                // ipv4 unicast
                findKeys(keys, family.map(AddressFamily::getVrfipv4)
                    .map(Vrfipv4::getVrfUnicast)
                    .map(VrfUnicast::getVrfPrefixes)
                    .flatMap(p -> p.getVrfPrefix().stream()));

                // ipv4 multicast
                findKeys(keys, family.map(AddressFamily::getVrfipv4)
                    .map(Vrfipv4::getVrfMulticast)
                    .map(VrfMulticast::getVrfPrefixes)
                    .flatMap(p -> p.getVrfPrefix().stream()));

                // ip6 unicast
                findKeys(keys, family.map(AddressFamily::getVrfipv6)
                    .map(Vrfipv6::getVrfUnicast)
                    .map(VrfUnicast::getVrfPrefixes)
                    .flatMap(p -> p.getVrfPrefix().stream()));

                // ipv6 multicast
                findKeys(keys, family.map(AddressFamily::getVrfipv6)
                        .map(Vrfipv6::getVrfMulticast)
                        .map(VrfMulticast::getVrfPrefixes)
                        .flatMap(p -> p.getVrfPrefix().stream()));
            }
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            throw new ReadFailedException(id, e);
        }
        return keys;
    }

    private void findKeys(final List<StaticKey> keys, final VrfPrefixes prefixes) {
        findKeys(keys, prefixes.getVrfPrefix().stream());
    }

    private void findKeys(final List<StaticKey> keys, Stream<VrfPrefix> prefixes) {
        keys.addAll(prefixes.map(this::convertVrfKeyToStaticKey).collect(Collectors.toList()));
    }

    private StaticKey convertVrfKeyToStaticKey(final VrfPrefix prefix) {
        IpPrefix finalPrefix;
        if (prefix.getPrefix().getIpv4AddressNoZone() != null) {
            finalPrefix = new IpPrefix(new Ipv4Prefix(prefix.getPrefix().getIpv4AddressNoZone().getValue() + "/" + prefix.getPrefixLength()));
        } else {
            finalPrefix = new IpPrefix(new Ipv6Prefix(prefix.getPrefix().getIpv6AddressNoZone().getValue() + "/" + prefix.getPrefixLength()));
        }
        return new StaticKey(finalPrefix);
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<Static> readData) {
        ((StaticRoutesBuilder) builder).setStatic(readData);
    }

    @Override
    public StaticBuilder getBuilder(InstanceIdentifier<Static> id) {
        return new StaticBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Static> id, StaticBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        ProtocolKey protKey = id.firstKeyOf(Protocol.class);
        if (!protKey.getIdentifier().equals(StaticProtocolReader.TYPE)) {
            return;
        }
        final IpPrefix prefix = id.firstKeyOf(Static.class).getPrefix();
        builder.setPrefix(prefix);
        builder.setConfig(new ConfigBuilder().setPrefix(prefix).build());
        builder.setState(new StateBuilder().setPrefix(prefix).build());
    }
}
