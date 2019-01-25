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

package io.frinx.unitopo.unit.xr6.configmetadata

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.CfgHistGl
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.cfg.hist.gl.RecordType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.cfg.hist.gl.RecordTypeBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.cfg.hist.gl.RecordTypeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadata
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadataBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConfigMetadataReader(private val access: UnderlayAccess) :
    OperReaderCustomizer<ConfigurationMetadata, ConfigurationMetadataBuilder> {

    override fun getBuilder(id: InstanceIdentifier<ConfigurationMetadata>): ConfigurationMetadataBuilder =
        ConfigurationMetadataBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<ConfigurationMetadata>,
        configmetadata: ConfigurationMetadataBuilder,
        ctx: ReadContext
    ) {

        val recordType = access.read(InstanceIdentifier.create(CfgHistGl::class.java)
            .child(RecordType::class.java, RecordTypeKey(CiscoIosXrString(RECORD_TYPE_COMMIT))),
            LogicalDatastoreType.OPERATIONAL)
            .checkedGet()
            .or(RecordTypeBuilder().build())

        configmetadata.lastConfigurationFingerprint = parseTimestamp(recordType)
    }

    override fun merge(
        parentBuilder: Builder<out DataObject>,
        data: ConfigurationMetadata
    ) {
        (parentBuilder as ConfigurationMetadataBuilder).lastConfigurationFingerprint =
            data.lastConfigurationFingerprint
    }

    companion object {
        const val RECORD_TYPE_COMMIT = "commit"

        @VisibleForTesting
        fun parseTimestamp(data: RecordType): String {
            val lastTimestamp = data.record.orEmpty()
                .asSequence()
                .map { it.timestamp }
                .filterNotNull()
                .sorted()
                .last()

            return LocalDateTime.ofEpochSecond(lastTimestamp, 0, ZoneOffset.UTC).toString()
        }
    }
}