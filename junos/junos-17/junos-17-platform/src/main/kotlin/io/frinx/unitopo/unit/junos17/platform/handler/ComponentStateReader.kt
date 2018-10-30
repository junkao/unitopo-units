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

package io.frinx.unitopo.unit.junos17.platform.handler

import com.google.common.base.Optional
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.OsComponent
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.ComponentBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.components.component.StateBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jrpc.show.version.junos._17._3r1._10.rev170101.GetSoftwareInformationInput
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild
import org.opendaylight.yangtools.yang.data.impl.schema.Builders
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import org.w3c.dom.Element

class ComponentStateReader(private val access: UnderlayAccess) : OperReaderCustomizer<State, StateBuilder> {

    override fun getBuilder(id: InstanceIdentifier<State>): StateBuilder = StateBuilder()

    override fun readCurrentAttributes(id: InstanceIdentifier<State>, StateBuilder: StateBuilder, ctx: ReadContext) {

        val invokeRpc = access.invokeRpc(schema, input)

        val checkedGet = invokeRpc.checkedGet()
        checkedGet.result ?: return

        val choice = (checkedGet.result as ContainerNode)
            .getChild(YangInstanceIdentifier.NodeIdentifier(QName.create(qName, OUTPUT_C).intern()))
        parseFields(StateBuilder, choice)
    }

    override fun merge(parentBuilder: Builder<out DataObject>, data: State) {
        (parentBuilder as ComponentBuilder).state = data
    }

    fun parseFields(
        StateBuilder: StateBuilder,
        choice: Optional<DataContainerChild<out
        YangInstanceIdentifier.PathArgument, *>>
    ) {

        if (choice.isPresent) {
            val xmlNode = (choice.get() as ChoiceNode).getChild(yangIid)
            if (xmlNode.isPresent) {
                val domSource = (xmlNode.get() as AnyXmlNode).value

                val elementsByTagName = (domSource.node as Element).getElementsByTagName(JUNOS_VERSION)
                elementsByTagName?.item(0)?.textContent?.let {
                    StateBuilder.setName(OsComponent.OS_NAME)
                    StateBuilder.setId(JUNOS_ID)
                    StateBuilder.setSoftwareVersion(it) }
            }
        }
    }

    companion object {

        const val OUTPUT_C = "output_c"
        private const val JUNOS_VERSION = "junos-version"
        private const val JUNOS_ID = "JUNOS"
        const val GET_SOFT_INF = "get-software-information"
        const val SOFT_INF = "software-information"

        private val qName = GetSoftwareInformationInput.QNAME

        private val leaf = Builders.leafBuilder<String>()
                .withNodeIdentifier(YangInstanceIdentifier
                        .NodeIdentifier(QName.create(qName, "level")))
                .withValue("detail")
                .build()

        private val input = Builders.containerBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier(qName))
                .withChild(leaf)
                .build()

        private val schema = SchemaPath.create(true, QName.create(qName, GET_SOFT_INF).intern())

        private val yangIid = YangInstanceIdentifier
            .NodeIdentifier(QName.create(qName, SOFT_INF).intern())
    }
}