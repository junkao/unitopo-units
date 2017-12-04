package io.frinx.unitopo.unit.xr6.vrf

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstancesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NetworkInstanceReader(private val underlayAccess: UnderlayAccess) : ListReaderCustomizer<NetworkInstance, NetworkInstanceKey, NetworkInstanceBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<NetworkInstance>, builder: NetworkInstanceBuilder, ctx: ReadContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name
        builder.name = vrfName
    }

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<NetworkInstance>) {
        (builder as NetworkInstancesBuilder).networkInstance = readData
    }

    override fun getBuilder(id: InstanceIdentifier<NetworkInstance>): NetworkInstanceBuilder = NetworkInstanceBuilder()

    override fun getAllIds(id: InstanceIdentifier<NetworkInstance>, context: ReadContext): List<NetworkInstanceKey> {
        val parseIds = parseIds()
        parseIds.add(DEFAULT_VRF)
        return parseIds
    }

    private fun parseIds(): MutableList<NetworkInstanceKey> {
        return underlayAccess.read(VRFS_ID)
                .checkedGet()
                .orNull()
                ?.let {
                    it.vrf?.map { NetworkInstanceKey(it.vrfName.value) }
                            ?.toCollection(mutableListOf())
                }.orEmpty()
                .toMutableList()
    }

    companion object {
        val VRFS_ID = InstanceIdentifier.create(Vrfs::class.java)

        val DEFAULT_VRF = NetworkInstanceKey("default")
    }

}