/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.sort

data class SortMode(val sortRef: SortRef, val revert: Boolean = false) {

    companion object {
        fun deserialize(str: String): SortMode {
            val array = str.split(':')
            return if (array.size != 2) SortMode(SortRef.ID) else
                SortMode(
                    SortRef.deserialize(array[0]), array[1] != "0"
                )
        }
    }

    fun serialize(): String =
        "${sortRef.serializedName}:${if (!revert) "0" else "1"}"

}
