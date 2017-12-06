package io.frinx.unitopo.unit.xr6.ospf.common

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.frinx.cli.registry.common.TypedWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier

interface OspfListWriter<O, K> : TypedWriter<O>, ListWriterCustomizer<O, K>
        where O: DataObject, O: Identifiable<K>, K: Identifier<O> {

    override fun getKey(): ProtocolKey {
        return ProtocolKey(TYPE, null)
    }

    companion object {
        val TYPE: Class<OSPF> = OSPF::class.java
    }
}
