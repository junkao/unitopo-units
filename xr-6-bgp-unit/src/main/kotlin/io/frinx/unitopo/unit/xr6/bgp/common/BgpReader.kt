package io.frinx.unitopo.unit.xr6.bgp.common

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.cli.registry.common.TypedReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject

interface BgpReader<O : DataObject, B : Builder<O>> : TypedReader<O, B> {

    override fun getKey(): ProtocolKey {
        return ProtocolKey(TYPE, null)
    }

    companion object {
        val TYPE: Class<BGP> = BGP::class.java
    }


    /**
     * Union mixin of Bgp reader and Config reader.
     */
    interface BgpConfigReader<O : DataObject, B : Builder<O>> : BgpReader<O, B>, ConfigReaderCustomizer<O, B>

    interface BgpOperReader<O : DataObject, B : Builder<O>> : BgpReader<O, B>, OperReaderCustomizer<O, B>
}
