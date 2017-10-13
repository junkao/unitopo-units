/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericListReader
import io.fd.honeycomb.translate.impl.read.GenericReader
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstances
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstancesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.*
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayVRFYangInto
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetInstanceYangInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
            NetInstanceYangInfo.getInstance()
    )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            UnderlayVRFYangInto.getInstance()
    )

    override fun getRpcs(underlayAccess: UnderlayAccess): Set<RpcService<*, *>> = emptySet()

    override fun provideHandlers(rRegistry: ModifiableReaderRegistryBuilder,
                                 wRegistry: ModifiableWriterRegistryBuilder,
                                 underlayAccess: UnderlayAccess) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(NIS_ID, NetworkInstancesBuilder::class.java)
        rRegistry.add(GenericListReader(NI_ID, NetworkInstanceReader(underlayAccess)))
        rRegistry.add(GenericReader<Config, ConfigBuilder>(NIC_ID, NetworkInstanceConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(NIFCS_ID, InterfacesBuilder::class.java)
        rRegistry.add(GenericListReader(NIFC_ID, InterfaceReader(underlayAccess)))

        rRegistry.addStructuralReader(PROTOCOLS_ID, ProtocolsBuilder::class.java)
        rRegistry.add(GenericListReader(PROTOCOL_ID, ProtocolReader(underlayAccess)))
        rRegistry.add(GenericReader(PROTOCOL_CFG_ID, ProtocolConfigReader()))
        rRegistry.add(GenericReader(PROTOCOL_STATE_ID, ProtocolStateReader(underlayAccess)))
    }

    override fun toString(): String = "XR 6 (2015-07-30) network-instance translate unit"

    companion object {
        private val NIS_ID = InstanceIdentifier.create(NetworkInstances::class.java)
        private val NI_ID = NIS_ID.child(NetworkInstance::class.java)
        private val NIC_ID = NI_ID.child(Config::class.java)
        private val NIFCS_ID = NI_ID.child(Interfaces::class.java)
        private val NIFC_ID = NIFCS_ID.child(Interface::class.java)

        private val PROTOCOLS_ID = NI_ID
                .child(Protocols::class.java)
        private val PROTOCOL_ID = PROTOCOLS_ID
                .child(Protocol::class.java)
        private val PROTOCOL_CFG_ID = PROTOCOL_ID
                .child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.Config::class.java)
        private val PROTOCOL_STATE_ID = PROTOCOL_ID
                .child(org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.protocol.State::class.java)

    }
}


