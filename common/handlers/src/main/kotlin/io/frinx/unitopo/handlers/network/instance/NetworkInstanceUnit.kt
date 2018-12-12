/*
 * Copyright © 2018 Frinx and others.
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
package io.frinx.unitopo.handlers.network.instance

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.handlers.network.instance.protocol.ProtocolConfigReader
import io.frinx.unitopo.handlers.network.instance.protocol.ProtocolStateReader
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.NetworkInstancesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ProtocolsBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.`$YangModuleInfoImpl` as NetInstanceYangInfo

abstract class NetworkInstanceUnit : TranslateUnit {

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        NetInstanceYangInfo.getInstance()
    )

    override fun getRpcs(underlayAccess: UnderlayAccess): Set<RpcService<*, *>> = emptySet()

    override fun provideHandlers(
        rRegistry: ModifiableReaderRegistryBuilder,
        wRegistry: ModifiableWriterRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry)
        provideSpecificReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry)
        provideSpecificWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: ModifiableWriterRegistryBuilder) {
        // FIXME noop
        wRegistry.add(GenericWriter(IIDs.NE_NETWORKINSTANCE, NoopWriter()))
        wRegistry.add(GenericWriter(IIDs.NE_NE_PR_PROTOCOL, NoopWriter()))
    }

    private fun provideReaders(rRegistry: ModifiableReaderRegistryBuilder) {
        rRegistry.addStructuralReader(IIDs.NETWORKINSTANCES, NetworkInstancesBuilder::class.java)

        rRegistry.addStructuralReader(IIDs.NE_NE_PROTOCOLS, ProtocolsBuilder::class.java)
        rRegistry.add(GenericConfigReader(IIDs.NE_NE_PR_PR_CONFIG, ProtocolConfigReader()))
        rRegistry.add(GenericOperReader(IIDs.NE_NE_PR_PR_STATE, ProtocolStateReader()))

        rRegistry.addStructuralReader(IIDs.NE_NE_INTERFACES, InterfacesBuilder::class.java)
    }

    abstract fun provideSpecificWriters(wRegistry: ModifiableWriterRegistryBuilder, underlayAccess: UnderlayAccess)

    abstract fun provideSpecificReaders(rRegistry: ModifiableReaderRegistryBuilder, underlayAccess: UnderlayAccess)

    override fun toString(): String = "Network-instance translate unit"
}