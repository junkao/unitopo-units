package io.frinx.unitopo.unit.xr6.lr.common

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.cli.registry.common.TypedReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.STATIC
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject

interface LrReader<O : DataObject, B : Builder<O>> : TypedReader<O, B> {

    override fun getKey(): ProtocolKey {
        return ProtocolKey(TYPE, null)
    }

    companion object {
        val TYPE: Class<STATIC> = STATIC::class.java
    }

    /**
     * Union mixin of Lr reader and Config reader.
     */
    interface LrConfigReader<O : DataObject, B : Builder<O>> : LrReader<O, B>, ConfigReaderCustomizer<O, B>

    interface LrOperReader<O : DataObject, B : Builder<O>> : LrReader<O, B>, OperReaderCustomizer<O, B>
}
