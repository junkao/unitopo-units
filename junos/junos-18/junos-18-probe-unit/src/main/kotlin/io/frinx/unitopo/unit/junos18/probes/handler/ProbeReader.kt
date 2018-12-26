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

package io.frinx.unitopo.unit.junos18.probes.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.openconfig.probes.top.ProbesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.Probe
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.probes.rev170905.probes.top.ProbeKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.root.rev180101.Configuration
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.Configuration1 as ServicesConfigurationAug
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.junos.conf.services.rev180101.services.group.Services
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class ProbeReader(private val underlayAccess: UnderlayAccess) :
    ConfigListReaderCustomizer<Probe, ProbeKey, ProbeBuilder> {

    override fun getAllIds(instanceIdentifier: IID<Probe>, readContext: ReadContext): List<ProbeKey> {
        return getProbeIds(underlayAccess)
    }

    override fun merge(builder: Builder<out DataObject>, list: List<Probe>) {
        (builder as ProbesBuilder).probe = list
    }

    override fun getBuilder(instanceIdentifier: IID<Probe>): ProbeBuilder = ProbeBuilder()

    override fun readCurrentAttributes(
        instanceIdentifier: IID<Probe>,
        probeBuilder: ProbeBuilder,
        readContext: ReadContext
    ) {
        val probeName = instanceIdentifier.firstKeyOf(Probe::class.java).name
        probeBuilder.name = probeName
    }

    companion object {
        private val JUNOS_CFG = IID.create(Configuration::class.java)!!
        private val JUNOS_SERVICES_AUG = JUNOS_CFG.augmentation(ServicesConfigurationAug::class.java)!!
        val JUNOS_SERVICES = JUNOS_SERVICES_AUG.child(Services::class.java)!!

        private fun getProbeIds(underlayAccess: UnderlayAccess): List<ProbeKey> {
            return underlayAccess.read(JUNOS_SERVICES, LogicalDatastoreType.CONFIGURATION)
                .checkedGet()
                .orNull()
                ?.let { services ->
                    services.rpm?.probe
                        ?.map { ProbeKey(it.name) }
                }.orEmpty()
        }
    }
}