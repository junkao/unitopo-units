/*
 * Copyright © 2020 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.frinx.unitopo.unit.xr7.isis.handler.global

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr7.isis.handler.IsisProtocolReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.Instances
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.cfg.rev191031.isis.instances.Instance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisGlobalConfAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisGlobalConfAugBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.isis.extension.rev190311.IsisInternalLevel
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.openconfig.isis.rev181121.isis.global.base.ConfigBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.clns.isis.datatypes.rev190405.IsisInternalLevel as UnderlayIILevel

open class IsisGlobalConfigReader(private val access: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfName = id.firstKeyOf(NetworkInstance::class.java).name

        val data = access.read(IsisProtocolReader.UNDERLAY_ISIS, LogicalDatastoreType.CONFIGURATION)
            .checkedGet()
            .orNull()

        parse(data, protKey, builder, vrfName)
    }

    companion object {

        fun parse(data: Instances?, protKey: ProtocolKey, builder: ConfigBuilder, vrfName: String) {
            data?.let {
                it.instance.orEmpty()
                    .find { it.instanceName.value == protKey.name }
                    ?.let { builder.fromUnderlay(it, vrfName) }
            }
        }
    }
}

@VisibleForTesting
fun ConfigBuilder.fromUnderlay(underlayInstance: Instance?, vrfName: String) {
    underlayInstance?.maxLinkMetrics?.maxLinkMetric?.map {
        it.level.toOpenConfig()
    }?.toList()?.let {
        addAugmentation(IsisGlobalConfAug::class.java,
            IsisGlobalConfAugBuilder().setMaxLinkMetric(it).build())
    }
}

fun UnderlayIILevel.toOpenConfig(): IsisInternalLevel {
    return when (this) {
        UnderlayIILevel.Level1 -> IsisInternalLevel.LEVEL1
        UnderlayIILevel.Level2 -> IsisInternalLevel.LEVEL2
        UnderlayIILevel.NotSet -> IsisInternalLevel.NOTSET
    }
}