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

package io.frinx.unitopo.unit.junos17.configmetadata.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadata
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadataBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jrpc.show.system.junos._17._3r1._10.rev170101.GetCommitRevisionInformationInput
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.data.impl.schema.Builders
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import org.w3c.dom.Element

class ConfigMetadataReader(private val access: UnderlayAccess) :
    OperReaderCustomizer<ConfigurationMetadata, ConfigurationMetadataBuilder> {

    override fun getBuilder(id: InstanceIdentifier<ConfigurationMetadata>): ConfigurationMetadataBuilder =
        ConfigurationMetadataBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<ConfigurationMetadata>,
        configmetadata: ConfigurationMetadataBuilder,
        ctx: ReadContext
    ) {

        val invokeRpc = access.invokeRpc(schema, input)

        val checkedGet = invokeRpc.checkedGet()
        checkedGet.result ?: return
        val choice = (checkedGet.result as ContainerNode)
            .getChild(YangInstanceIdentifier.NodeIdentifier(QName.create(qName, "output_c")))

        if (choice.isPresent) {
            val xmlNode = (choice.get() as ChoiceNode).getChild(yangIid)
            if (xmlNode.isPresent) {
                val domSource = (xmlNode.get() as AnyXmlNode).value

                val elementsByTagName = (domSource.node as Element).getElementsByTagName("date-time")
                elementsByTagName?.item(0)?.textContent?.let {
                    configmetadata.lastConfigurationFingerprint = it
                }
            }
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, data: ConfigurationMetadata) {
        (parentBuilder as ConfigurationMetadataBuilder).lastConfigurationFingerprint =
            data.lastConfigurationFingerprint
    }

    companion object {

        private val qName = GetCommitRevisionInformationInput.QNAME

        private val leaf = Builders.leafBuilder<String>()
        .withNodeIdentifier(YangInstanceIdentifier
        .NodeIdentifier(QName.create(qName, "level")))
        .withValue("detail")
        .build()

        private val input = Builders.containerBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier(qName))
                .withChild(leaf)
                .build()

        private val schema = SchemaPath.create(true, QName.create(qName, "get-commit-revision-information"))

        private val yangIid = YangInstanceIdentifier.NodeIdentifier(QName.create(qName, "commit-revision-information"))
    }
}