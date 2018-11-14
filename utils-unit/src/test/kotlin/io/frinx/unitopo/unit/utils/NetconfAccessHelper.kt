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

package io.frinx.unitopo.unit.utils

import com.google.common.base.Optional
import com.google.common.util.concurrent.CheckedFuture
import com.google.common.util.concurrent.Futures
import io.frinx.translate.unit.commons.handler.spi.GenericTranslateContext
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.config.util.xml.XmlUtil
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext
import org.opendaylight.netconf.api.NetconfMessage
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.NetconfMessageTransformer
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yangtools.yang.binding.util.BindingReflections
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.model.api.SchemaPath
import java.net.URL

open class NetconfAccessHelper() : UnderlayAccess {
    private val mib = createMib()
    private val schemaContext = createSchemaContext(mib)
    private val transformer = NetconfMessageTransformer(schemaContext, true)
    private val codec = GenericTranslateContext.getCodec(mib, schemaContext)

    private var configUrl: URL? = null
    private var configXml: String? = null

    private var operUrl: URL? = null
    private var operXml: String? = null

    override fun <T : DataObject> read(path: InstanceIdentifier<T>): CheckedFuture<Optional<T>, ReadFailedException> {
        return read(path, LogicalDatastoreType.CONFIGURATION)
    }

    override fun <T : DataObject> read(path: InstanceIdentifier<T>, type: LogicalDatastoreType):
        CheckedFuture<Optional<T>, ReadFailedException> {

        val xml = when (type) {
            LogicalDatastoreType.CONFIGURATION -> configXml
            else -> operXml
        }
        val result = parseGetCfgResponse(xml!!, path)
        return Futures.immediateCheckedFuture(Optional.of(result))
    }

    fun compile(): NetconfAccessHelper {
        configXml = getResourceAsString(configUrl!!)
        when {
            operUrl == null -> {
                operUrl = configUrl
                operXml = configXml
            }
            operUrl!!.equals(configUrl) -> operXml = configXml
            else -> operXml = getResourceAsString(operUrl!!)
        }
        return this
    }

    constructor(src: NetconfAccessHelper) : this() {
        this.configUrl = src.configUrl!!
        this.configXml = src.configXml!!
        this.operUrl = src.operUrl
        this.operXml = src.operXml
    }

    constructor(name: String): this() {
        this.configUrl = javaClass.getResource(name)
        compile()
    }

    constructor(clazz: Class<*>, name: String): this() {
        this.configUrl = clazz.getResource(name)
        compile()
    }

    constructor(configUrl: URL): this() {
        this.configUrl = configUrl
        compile()
    }

    private fun <T : DataObject> parseGetCfgResponse(xml: String, type: InstanceIdentifier<T>): T {
        val msg = NetconfMessage(XmlUtil.readXmlToDocument(xml))

        val normalizedRpcResult = transformer.toRpcResult(msg, SchemaPath.create(true,
                NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME)).result
        val normalizedData = (normalizedRpcResult as ContainerNode).getChild(
                NetconfMessageTransformUtil.toId(NetconfMessageTransformUtil.NETCONF_DATA_QNAME)).get()

        val yangIID = codec.toYangInstanceIdentifier(type)
        val findNode = NormalizedNodes.findNode(normalizedData, yangIID)

        val value = codec.fromNormalizedNode(yangIID, findNode.get())!!.value
        return type.targetType.cast(value)
    }

    override fun getCurrentOperationType(): LogicalDatastoreType {
        throw UnsupportedOperationException("not implemented")
    }

    override fun invokeRpc(schemaPath: SchemaPath, normalizedNode: NormalizedNode<*, *>?):
        CheckedFuture<DOMRpcResult, DOMRpcException> {

        throw UnsupportedOperationException("not implemented")
    }

    override fun <T : DataObject?> put(path: InstanceIdentifier<T>?, data: T) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun <T : DataObject?> merge(path: InstanceIdentifier<T>?, data: T) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun close() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun delete(path: InstanceIdentifier<*>?) {
        throw UnsupportedOperationException("not implemented")
    }

    companion object {
        private fun createSchemaContext(mib: ModuleInfoBackedContext): SchemaContext {
            return mib.schemaContext
        }

        private fun createMib(): ModuleInfoBackedContext {
            val moduleInfoBackedContext = ModuleInfoBackedContext.create()
            moduleInfoBackedContext.addModuleInfos(getModels())
            return moduleInfoBackedContext
        }

        private fun getModels(): Collection<YangModuleInfo> {
            return BindingReflections.loadModuleInfos()
        }

        private fun getResource(clazz: Class<*>, name: String) = clazz.getResource(name)

        private fun getResource(name: String) = getResource(javaClass, name)

        private fun getResourceAsString(clazz: Class<*>, name: String) =
            getResource(clazz, name).readText(Charsets.UTF_8)

        private fun getResourceAsString(name: String) = getResourceAsString(javaClass, name)

        private fun getResourceAsString(url: URL) = url.readText(Charsets.UTF_8)
    }
}