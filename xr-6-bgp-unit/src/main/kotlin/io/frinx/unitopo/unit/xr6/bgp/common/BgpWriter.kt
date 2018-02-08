/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.common

import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.cli.registry.common.TypedWriter
import io.frinx.unitopo.unit.network.instance.common.L3VrfReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.BGP
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.*

interface BgpWriter<O : DataObject> : TypedWriter<O>, WriterCustomizer<O> {

    override fun getKey(): ProtocolKey {
        return ProtocolKey(TYPE, null)
    }

    override fun getParentCheck(id: InstanceIdentifier<O>) =  AbstractMap.SimpleEntry(
                RWUtils.cutId(id, NetworkInstance::class.java).child(Config::class.java),
                L3VrfReader.L3VRF_CHECK)

    companion object {
        val TYPE: Class<BGP> = BGP::class.java
    }
}
