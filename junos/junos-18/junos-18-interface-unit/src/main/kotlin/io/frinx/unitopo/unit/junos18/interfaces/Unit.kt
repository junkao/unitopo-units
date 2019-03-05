/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.junos18.interfaces

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.openconfig.openconfig.vlan.IIDs as VlanIIDs
import io.frinx.openconfig.openconfig._if.ip.IIDs as IPIIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVlanConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceAddressConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVrrpGroupConfigWriter
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVlanConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceAddressReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceAddressConfigReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVrrpGroupReader
import io.frinx.unitopo.unit.junos18.interfaces.handler.subinterfaces.SubinterfaceVrrpGroupConfigReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Address1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ip.vrrp.top.VrrpBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4Builder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.AddressesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.SubinterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.Subinterface1Builder as SubinterfaceVlanAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.vlan.logical.top.VlanBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.vlan.rev170714.`$YangModuleInfoImpl` as VlanYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.interfaces.rev180101.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.`$YangModuleInfoImpl` as UnderlayConfRootYangModuleInfo
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.`$YangModuleInfoImpl` as IanaIfTypeYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.`$YangModuleInfoImpl` as IPYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.extention.rev181204.`$YangModuleInfoImpl` as Config1YangModuleInfo

class Unit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
            InterfacesYangInfo.getInstance(),
            VlanYangInfo.getInstance(),
            IanaIfTypeYangInfo.getInstance(),
            IPYangInfo.getInstance(),
            Config1YangModuleInfo.getInstance()
    )
    override fun getUnderlayYangSchemas() = setOf(
            UnderlayConfRootYangModuleInfo.getInstance(),
            UnderlayInterfacesYangInfo.getInstance()
    )

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericListWriter(IIDs.IN_INTERFACE, NoopListWriter()))
        wRegistry.add(GenericWriter(IIDs.IN_IN_CONFIG, InterfaceConfigWriter(underlayAccess)))

        wRegistry.add(GenericWriter(IIDs.IN_IN_SUBINTERFACES, NoopWriter()))
        wRegistry.add(GenericListWriter(IIDs.IN_IN_SU_SUBINTERFACE, NoopListWriter()))

        wRegistry.subtreeAddAfter(
                setOf(RWUtils.cutIdFromStart(IIDs.IN_IN_SU_SU_CO_AUG_CONFIG1,
                        InstanceIdentifier.create(Config::class.java))),
                GenericWriter(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigWriter(underlayAccess)),
                IIDs.IN_IN_CONFIG
            )

        wRegistry.add(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, NoopWriter()))
        wRegistry.add(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VLAN, NoopWriter()))
        wRegistry.addAfter(GenericWriter(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
                SubinterfaceVlanConfigWriter(underlayAccess)), IIDs.IN_IN_SU_SU_CONFIG)
        wRegistry.addAfter(GenericWriter(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4, NoopWriter()),
                IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1)
        wRegistry.addAfter(GenericWriter(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES, NoopWriter()),
                IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4)
        wRegistry.addAfter(GenericListWriter(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS, NoopListWriter()),
                IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES)
        wRegistry.addAfter(GenericWriter(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
                SubinterfaceAddressConfigWriter(underlayAccess)), IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS)
        wRegistry.add(GenericWriter(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS3, NoopWriter()))
        wRegistry.add(GenericWriter(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS3_VRRP, NoopWriter()))
        wRegistry.add(GenericListWriter(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS3_VR_VRRPGROUP, NoopListWriter()))
        wRegistry.add(GenericWriter(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS3_VR_VR_CONFIG,
                SubinterfaceVrrpGroupConfigWriter(underlayAccess)))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_INTERFACE, InterfaceReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IIDs.IN_IN_CONFIG, InterfaceConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IIDs.IN_IN_SUBINTERFACES, SubinterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.IN_IN_SU_SUBINTERFACE, SubinterfaceReader(underlayAccess)))

        rRegistry.subtreeAdd(
            IID_SUBIFC_CONFIG_SUBTREE,
            GenericConfigReader(IIDs.IN_IN_SU_SU_CONFIG, SubinterfaceConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, SubinterfaceVlanAugBuilder::class.java)
        rRegistry.addStructuralReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VLAN, VlanBuilder::class.java)
        rRegistry.add(GenericConfigReader(VlanIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_VL_CONFIG,
                SubinterfaceVlanConfigReader(underlayAccess)))

        rRegistry.addStructuralReader(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1, Subinterface1Builder::class.java)
        rRegistry.addStructuralReader(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IPV4, Ipv4Builder::class.java)
        rRegistry.addStructuralReader(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_ADDRESSES, AddressesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_ADDRESS,
                SubinterfaceAddressReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IPIIDs.IN_IN_SU_SU_AUG_SUBINTERFACE1_IP_AD_AD_CONFIG,
                SubinterfaceAddressConfigReader(underlayAccess)))
        // vrrp
        rRegistry.addStructuralReader(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS1, Address1Builder::class.java)
        rRegistry.addStructuralReader(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS1_VRRP, VrrpBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS1_VR_VRRPGROUP,
                SubinterfaceVrrpGroupReader(underlayAccess)))
        rRegistry.add(GenericConfigReader(IPIIDs.IN_IN_SU_SU_IP_AD_AD_AUG_ADDRESS1_VR_VR_CONFIG,
                SubinterfaceVrrpGroupConfigReader(underlayAccess)))
    }

    companion object {
        private val IID_SUBIFC_CONFIG_SUBTREE = setOf(
            RWUtils.cutIdFromStart(IIDs.IN_IN_SU_SU_CO_AUG_CONFIG1, InstanceIdentifier.create(Config::class.java))
        )
    }

    override fun toString(): String = "Junos 18.2 interface translate unit"

    override fun useAutoCommit() = false
}