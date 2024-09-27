package com.huskerdev.gpkt.engines.cpu

import com.huskerdev.gpkt.BasicProgram
import com.huskerdev.gpkt.FieldNotSetException
import com.huskerdev.gpkt.TypesMismatchException
import com.huskerdev.gpkt.ast.objects.Scope
import com.huskerdev.gpkt.ast.types.Type
import com.huskerdev.gpkt.engines.cpu.objects.ExField
import com.huskerdev.gpkt.engines.cpu.objects.ExScope
import com.huskerdev.gpkt.engines.cpu.objects.ExValue
import com.huskerdev.gpkt.utils.splitThreadInvocation


class CPUProgram(
    val ast: Scope
): BasicProgram(ast) {
    override fun execute(instances: Int, vararg mapping: Pair<String, Any>) {
        val map = hashMapOf(*mapping)
        val variables = buffers.associate { field ->
            val value = map.getOrElse(field.name) { throw FieldNotSetException(field.name) }
            if(!areEqualTypes(value, field.type))
                throw TypesMismatchException(field.name)

            val desc = when (value) {
                is CPUFloatMemoryPointer -> Type.FLOAT_ARRAY to value.array!!
                is CPUDoubleMemoryPointer -> Type.DOUBLE_ARRAY to value.array!!
                is CPULongMemoryPointer -> Type.LONG_ARRAY to value.array!!
                is CPUIntMemoryPointer -> Type.INT_ARRAY to value.array!!
                is CPUByteMemoryPointer -> Type.BYTE_ARRAY to value.array!!
                is Float -> Type.FLOAT to value
                is Double -> Type.DOUBLE to value
                is Long -> Type.LONG to value
                is Int -> Type.INT to value
                is Byte -> Type.BYTE to value
                else -> throw UnsupportedOperationException()
            }
            field.name to ExField(desc.first, ExValue(desc.second))
        }

        splitThreadInvocation(instances) { from, to ->
            val scope = ExScope(ast)
            val iteration = ExField(Type.INT, ExValue(null))
            val threadLocalVariables = hashMapOf(
                "__i__" to iteration
            )
            threadLocalVariables.putAll(variables)

            for(step in from until to){
                iteration.value!!.set(step)
                scope.execute(threadLocalVariables)
            }
        }
    }

    override fun dealloc() = Unit
}

