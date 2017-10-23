package io.frinx.unitopo.unit.xr6.lr.common

import io.frinx.cli.registry.common.TypedReader
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev160512.STATIC
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject

interface LrReader<O : DataObject, B : Builder<O>> : TypedReader<O, B> {

    override fun getKey(): ProtocolKey {
        return ProtocolKey(TYPE, null)
    }

    companion object {
        val TYPE: Class<STATIC> = STATIC::class.java
    }
}
