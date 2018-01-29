package io.frinx.unitopo.unit.utils

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NoopWriter<T : DataObject> : WriterCustomizer<T> {

    override fun writeCurrentAttributes(p0: InstanceIdentifier<T>, p1: T, p2: WriteContext) {}

    override fun deleteCurrentAttributes(p0: InstanceIdentifier<T>, p1: T, p2: WriteContext) {}
}