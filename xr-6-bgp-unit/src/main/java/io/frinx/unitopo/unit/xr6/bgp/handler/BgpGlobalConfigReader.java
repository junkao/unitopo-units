/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.frinx.unitopo.registry.spi.UnderlayAccess;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.as.FourByteAs;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.as.four._byte.as.vrfs.Vrf;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.global.base.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.AsNumber;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.yang.rev170403.DottedQuad;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

public class BgpGlobalConfigReader implements ReaderCustomizer<Config, ConfigBuilder> {

    // TODO: move to a more appropriate place
    public static final String DEFAULT_VRF = "default";
    private static final InstanceIdentifier<Bgp> IID = InstanceIdentifier.create(Bgp.class);

    private UnderlayAccess access;

    public BgpGlobalConfigReader(UnderlayAccess access) {
        this.access = access;
    }

    @Nonnull
    @Override
    public ConfigBuilder getBuilder(@Nonnull InstanceIdentifier<Config> id) {
        return new ConfigBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Config> id, @Nonnull ConfigBuilder builder, @Nonnull ReadContext ctx) throws ReadFailedException {
        ProtocolKey protKey = id.firstKeyOf(Protocol.class);
        if (!protKey.getIdentifier().equals(BgpProtocolReader.TYPE)) {
            return;
        }
        final String vrfName = id.firstKeyOf(NetworkInstance.class).getName();
        try {
            Bgp bgp = access.read(IID).checkedGet().orNull();

            // each instance can only have one AS despite there is a list in cisco yang
            // NPE when instanceAs not present
            InstanceAs ias = bgp.getInstance().stream().filter(ins -> protKey.getName().equals(ins.getInstanceName().getValue())).findFirst().get().getInstanceAs().get(0);
            FourByteAs as4 = ias.getFourByteAs().get(0);

            Long as = as4.getAs().getValue();
            String routerId;
            if (DEFAULT_VRF.equals(vrfName)) {
                routerId = as4.getDefaultVrf().getGlobal().getRouterId().getValue();
            } else {
                Vrf vrf = as4.getVrfs().getVrf().stream().filter(v -> v.getVrfName().getValue().equals(vrfName)).findFirst().get();
                routerId = vrf.getVrfGlobal().getRouterId().getValue();
            }
            builder.setAs(new AsNumber(as));
            builder.setRouterId(new DottedQuad(routerId));
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull Config readValue) {
        ((GlobalBuilder) parentBuilder).setConfig(readValue);
    }
}
