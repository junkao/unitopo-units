package io.frinx.unitopo.unit.xr6.ospf.common

import io.frinx.cli.registry.common.TypedListReader
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier

interface OspfListReader<O, K, B> : OspfReader<O, B>, TypedListReader<O, K, B>
        where O : DataObject, O : Identifiable<K>, K : Identifier<O>, B : Builder<O>
