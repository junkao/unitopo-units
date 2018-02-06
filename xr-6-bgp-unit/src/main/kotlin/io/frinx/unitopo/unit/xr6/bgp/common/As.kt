package io.frinx.unitopo.unit.xr6.bgp.common

import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber

public const val MAX_AS_2BYTE = 65535L
public const val MAX_AS_4BYTE = 4294967295L

public fun asFromDotNotation(first: Long?, second: Long): AsNumber {
    require(first == null || first <= MAX_AS_2BYTE)
    require(second <= MAX_AS_2BYTE)

    return AsNumber((first ?: 0) * (MAX_AS_2BYTE + 1) + second)
}

public fun asToDotNotation(asN: AsNumber): Pair<Long, Long> {
    require(asN.value <= MAX_AS_4BYTE)
    require(asN.value > 0)

    return if (asN.value > MAX_AS_2BYTE) {
        Pair(asN.value / (MAX_AS_2BYTE + 1), asN.value % (MAX_AS_2BYTE + 1))
    } else {
        Pair(0, asN.value)
    }
}