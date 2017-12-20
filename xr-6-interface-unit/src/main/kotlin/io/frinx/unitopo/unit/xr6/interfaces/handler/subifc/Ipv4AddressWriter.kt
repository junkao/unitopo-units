package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv4AddressWriter : WriterCustomizer<Address> {
    override fun deleteCurrentAttributes(p0: InstanceIdentifier<Address>, p1: Address, p2: WriteContext) {
        //noop
    }

    override fun writeCurrentAttributes(p0: InstanceIdentifier<Address>, p1: Address, p2: WriteContext) {
        //noop
    }
}