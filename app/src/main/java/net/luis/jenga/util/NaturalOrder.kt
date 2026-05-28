package net.luis.jenga.util

/**
 * Compares strings so that embedded numbers sort by value, e.g. "T2" before "T10".
 * Comparison is case-insensitive, with a case-sensitive tiebreak for stable ordering.
 */
val naturalOrder: Comparator<String> = Comparator { a, b ->
    val byChunks = compareNatural(a, b)
    if (byChunks != 0) byChunks else a.compareTo(b)
}

private fun compareNatural(a: String, b: String): Int {
    var ia = 0
    var ib = 0
    while (ia < a.length && ib < b.length) {
        if (a[ia].isDigit() && b[ib].isDigit()) {
            val startA = ia
            while (ia < a.length && a[ia].isDigit()) ia++
            val startB = ib
            while (ib < b.length && b[ib].isDigit()) ib++

            var sa = startA
            while (sa < ia - 1 && a[sa] == '0') sa++
            var sb = startB
            while (sb < ib - 1 && b[sb] == '0') sb++

            val lenA = ia - sa
            val lenB = ib - sb
            if (lenA != lenB) return lenA - lenB
            for (k in 0 until lenA) {
                val d = a[sa + k] - b[sb + k]
                if (d != 0) return d
            }
        } else {
            val d = a[ia].lowercaseChar().compareTo(b[ib].lowercaseChar())
            if (d != 0) return d
            ia++
            ib++
        }
    }
    return (a.length - ia) - (b.length - ib)
}
