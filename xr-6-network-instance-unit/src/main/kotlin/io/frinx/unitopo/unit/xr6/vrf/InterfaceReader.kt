package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.interfaces.InterfaceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class InterfaceReader(private val underlayAccess: UnderlayAccess) : ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Interface>, builder: InterfaceBuilder, ctx: ReadContext) {
        val ifcName = id.firstKeyOf(Interface::class.java).id
        builder.id = ifcName
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Interface>) {
        (builder as InterfacesBuilder).`interface` = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Interface>): InterfaceBuilder = InterfaceBuilder()

    override fun getAllIds(id: InstanceIdentifier<Interface>, context: ReadContext): List<InterfaceKey> {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        val allIfcs = underlayAccess.read(InterfaceReader.IFC_CFGS)
                .checkedGet()
                .orNull()?.interfaceConfiguration ?: emptyList<InterfaceConfiguration>()

        return allIfcs.filter { it.getVrf() == vrfName }
                .map { InterfaceKey(it.interfaceName.value) }
                .toList()
    }
}

fun InterfaceConfiguration.getVrf(): String {
    getAugmentation(InterfaceConfiguration1::class.java)?.let {
        return it.vrf?.value ?: NetworkInstanceReader.DEFAULT_VRF.name
    }

    return NetworkInstanceReader.DEFAULT_VRF.name
}