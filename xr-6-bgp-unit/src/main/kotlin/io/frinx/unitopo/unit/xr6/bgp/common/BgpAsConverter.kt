package io.frinx.unitopo.unit.xr6.bgp.common

class BgpAsConverter {

    companion object {
        public fun longToXxYy(longAs: Long): Pair<Int, Int> {
            if (longAs <= Integer.MAX_VALUE) {
                return Pair(0, longAs as Int)
            } else {
                return Pair((longAs shr 32) as Int, longAs as Int)
            }
        }

        public fun XxYyToLong(xx: Int, yy:Int): Long{
            return ((xx as Long) shl 32) + yy
        }
    }
}