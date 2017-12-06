package io.frinx.unitopo.unit.xr6.ospf.common

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.frinx.cli.registry.common.TypedReader
import io.frinx.cli.registry.common.TypedWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject

interface OspfWriter<O : DataObject> : TypedWriter<O>, WriterCustomizer<O> {

    override fun getKey(): ProtocolKey {
        return ProtocolKey(TYPE, null)
    }

    companion object {
        val TYPE: Class<OSPF> = OSPF::class.java
    }
}
