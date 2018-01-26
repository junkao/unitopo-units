package io.frinx.unitopo.unit.utils

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NoopListWriter<C, K> : ListWriterCustomizer<C, K>
        where C: DataObject, C: Identifiable<K>, K: Identifier<C> {

    override fun writeCurrentAttributes(p0: InstanceIdentifier<C>, p1: C, p2: WriteContext) {}

    override fun deleteCurrentAttributes(p0: InstanceIdentifier<C>, p1: C, p2: WriteContext) {}

}