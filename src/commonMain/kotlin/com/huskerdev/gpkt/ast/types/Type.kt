package com.huskerdev.gpkt.ast.types

const val FLAG_NUMBER = 2
const val FLAG_FLOATING_POINT = 4
const val FLAG_INTEGER = 8
const val FLAG_LOGICAL = 16
const val FLAG_ARRAY = 32

enum class Type(
    val text: String,
    val bytes: Int,
    flags: Int,
) {
    VOID("void", -1, 0),

    FLOAT("float", 4, FLAG_NUMBER or FLAG_FLOATING_POINT),
    INT("int", 4, FLAG_NUMBER or FLAG_INTEGER),
    BYTE("byte", 1, FLAG_NUMBER or FLAG_INTEGER),
    BOOLEAN("boolean", 1, FLAG_LOGICAL),

    FLOAT_ARRAY("float[]", -1, FLAG_ARRAY),
    INT_ARRAY("int[]", -1, FLAG_ARRAY),
    BYTE_ARRAY("byte[]", -1, FLAG_ARRAY),
    BOOLEAN_ARRAY("boolean[]", -1, FLAG_ARRAY)
    ;

    val isNumber = flags and FLAG_NUMBER == FLAG_NUMBER
    val isFloating = flags and FLAG_FLOATING_POINT == FLAG_FLOATING_POINT
    val isInteger = flags and FLAG_INTEGER == FLAG_INTEGER
    val isLogical = flags and FLAG_LOGICAL == FLAG_LOGICAL
    val isArray = flags and FLAG_ARRAY == FLAG_ARRAY

    companion object {
        val map = entries.associateBy { it.text }

        val allowedCastMap = mapOf(
            FLOAT to setOf(FLOAT, INT, BYTE),
            INT to setOf(FLOAT, INT, BYTE),
            BYTE to setOf(FLOAT, INT, BYTE),
        )

        fun toArrayType(type: Type) = when(type){
            FLOAT -> FLOAT_ARRAY
            INT -> INT_ARRAY
            BYTE -> BYTE_ARRAY
            BOOLEAN -> BOOLEAN_ARRAY
            else -> throw UnsupportedOperationException()
        }

        fun toSingleType(type: Type) = when(type){
            FLOAT_ARRAY -> FLOAT
            INT_ARRAY -> INT
            BYTE_ARRAY -> BYTE
            BOOLEAN_ARRAY -> BOOLEAN
            else -> throw UnsupportedOperationException()
        }

        fun canAssignNumbers(to: Type, from: Type) =
            to.isNumber && from.isNumber && to.bytes >= from.bytes

        fun mergeNumberTypes(type1: Type, type2: Type) = when {
            type1 == type2 -> type1
            type1.isFloating && !type2.isFloating -> type1
            !type1.isFloating && type2.isFloating -> type2
            type1.bytes > type1.bytes -> type1
            else -> type2
        }

    }
}