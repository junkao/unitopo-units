/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.bgp.common.BgpWriter
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.InstanceAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAsKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.Global
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.GlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`._default.vrf.global.global.afs.GlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.VrfGlobal
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.VrfGlobalAfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.four._byte.`as`.vrfs.vrf.vrf.global.vrf.global.afs.VrfGlobalAfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.SourcedNetworks
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetwork
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetworkBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.sourced.network.table.sourced.networks.SourcedNetworkKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.datatypes.rev150827.BgpAsRange
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.local.routing.rev170515.local.aggregate.top.local.aggregates.aggregate.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress
import java.util.regex.Pattern
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AggregateConfigWriter(private val underlayAccess: UnderlayAccess) : BgpWriter<Config> {

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun deleteCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = iid.firstKeyOf(Protocol::class.java).name.toLong()

        val networkIid = getSourcedNetworkIdentifier(vrfName, bgpProcess, dataBefore.prefix)

        try {
            underlayAccess.delete(networkIid)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(networkIid, e)
        }
    }

    override fun writeCurrentAttributesForType(iid: IID<Config>, dataAfter: Config, wc: WriteContext) {
        val vrfName = iid.firstKeyOf(NetworkInstance::class.java).name
        val bgpProcess = iid.firstKeyOf(Protocol::class.java).name.toLong()
        val networkIid = getSourcedNetworkIdentifier(vrfName, bgpProcess, dataAfter.prefix)
        val network = getNetworkData(dataAfter.prefix)

        try {
            underlayAccess.merge(networkIid, network)
        } catch (e: Exception) {
            throw io.fd.honeycomb.translate.write.WriteFailedException(networkIid, e)
        }
    }

    private fun getNetworkData(prefix: IpPrefix): SourcedNetwork {
        val prefixMatcher = prefixPattern.matcher(prefix.value.toString())
        return SourcedNetworkBuilder()
                .setKey(SourcedNetworkKey(IpAddress(prefixMatcher.group("addr").toCharArray()),
                        prefixMatcher.group("mask").toInt()))
                .build()
    }

    companion object {
        val prefixPattern = Pattern.compile("(?<addr>.*)\\/(?<mask>.*)")

        public fun getSourcedNetworkIdentifier(vrfName: String, bgpProcess: Long, prefix: IpPrefix): IID<SourcedNetwork> {
            val prefixMatcher = prefixPattern.matcher(prefix.value.toString())
            return IID.create(Bgp::class.java)
                    .child(Instance::class.java, InstanceKey(CiscoIosXrString("default")))
                    .child(InstanceAs::class.java, InstanceAsKey(BgpAsRange(0)))
                    .child(FourByteAs::class.java, FourByteAsKey(BgpAsRange(bgpProcess)))
                    .let {
                        if(vrfName.equals("default")) {
                            it.child(DefaultVrf::class.java)
                                    .child(Global::class.java)
                                    .child(GlobalAfs::class.java)
                                    .child(GlobalAf::class.java, GlobalAfKey(BgpAddressFamily.Ipv4Unicast))
                                    .child(SourcedNetworks::class.java)
                                    .child(SourcedNetwork::class.java,
                                            SourcedNetworkKey(IpAddress(prefixMatcher.group("addr").toCharArray()),
                                                    prefixMatcher.group("mask").toInt()))
                        } else {
                            it.child(Vrfs::class.java)
                                    .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName)))
                                    .child(VrfGlobal::class.java)
                                    .child(VrfGlobalAfs::class.java)
                                    .child(VrfGlobalAf::class.java, VrfGlobalAfKey(BgpAddressFamily.Ipv4Unicast))
                                    .child(SourcedNetworks::class.java)
                                    .child(SourcedNetwork::class.java,
                                            SourcedNetworkKey(IpAddress(prefixMatcher.group("addr").toCharArray()),
                                                    prefixMatcher.group("mask").toInt()))
                        }
                    }
        }
    }
}