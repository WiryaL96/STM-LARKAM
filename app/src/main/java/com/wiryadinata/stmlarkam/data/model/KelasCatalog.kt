package com.wiryadinata.stmlarkam.data.model

/**
 * Predefined class (kelas) list per angkatan, used to populate the class picker on the
 * add-session screen instead of free-text entry.
 *
 * Structure: every jurusan has three parallel classes (A, B, C). The grade prefix is
 * derived from the angkatan:
 *  - @51 -> XII (kelas 12)
 *  - @52 -> XI  (kelas 11)
 *  - @53 -> X   (kelas 10)
 *
 * To change the whole picker, edit [JURUSAN], [PARALEL], or [GRADE_BY_ANGKATAN] here.
 */
object KelasCatalog {

    private val JURUSAN = listOf("RPL", "IOP", "SIJA", "PSPT", "TEK", "EIND", "TPTU", "TOI", "MEKA")

    private val PARALEL = listOf("A", "B", "C")

    private val GRADE_BY_ANGKATAN = mapOf(
        "@51" to "XII",
        "@52" to "XI",
        "@53" to "X"
    )

    /** All class names for the given angkatan (e.g. "XII RPL A"), or empty if unknown. */
    fun kelasFor(namaAngkatan: String): List<String> {
        val grade = GRADE_BY_ANGKATAN[namaAngkatan.trim()] ?: return emptyList()
        return JURUSAN.flatMap { jurusan -> PARALEL.map { paralel -> "$grade $jurusan $paralel" } }
    }
}
