package com.huskerdev.gpkt.engines.jdk

import com.huskerdev.gpkt.FieldNotSetException
import com.huskerdev.gpkt.SimpleCProgram
import com.huskerdev.gpkt.TypesMismatchException
import com.huskerdev.gpkt.ast.FunctionStatement
import com.huskerdev.gpkt.ast.ScopeStatement
import com.huskerdev.gpkt.ast.objects.Field
import com.huskerdev.gpkt.ast.types.Modifiers
import com.huskerdev.gpkt.ast.types.Type
import com.huskerdev.gpkt.engines.cpu.*
import com.huskerdev.gpkt.utils.appendCFunctionHeader
import com.huskerdev.gpkt.utils.splitThreadInvocation
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicLong


class JavacProgram(ast: ScopeStatement): SimpleCProgram(ast) {
    companion object {
        val counter = AtomicLong()
    }
    private val execMethod: Method

    init {
        val className = "GPJavacProgram${counter.getAndIncrement()}"
        val buffer = StringBuilder()
        buffer.append("""
            import static java.lang.Math.*;
            public class $className{ 
                public static void _execute(int fromIndex, int toIndex, ${buffers.joinToString(transform = ::transformKernelArg)}){
                    ${buffers.joinToString("") { "${it.name}=__v_${it.name};" }}
                    for(int i = fromIndex; i < toIndex; i++)
                        __m(i);
                }
        """.trimIndent())
        stringifyScopeStatement(buffer, ast, false)
        buffer.append("}")

        val clazz = ClassCompiler.compileClass(buffer.toString(), className)
        execMethod = clazz.getMethod(
            "_execute",
            Int::class.java, Int::class.java,
            *(buffers.map { it.type.toJavaClass() }.toTypedArray())
        )
    }

    override fun executeRange(indexOffset: Int, instances: Int, vararg mapping: Pair<String, Any>) {
        val map = hashMapOf(*mapping)

        val arrays = buffers.map { field ->
            val value = map.getOrElse(field.name) { throw FieldNotSetException(field.name) }
            if(!areEqualTypes(value, field.type))
                throw TypesMismatchException(field.name)

            when(value){
                is CPUMemoryPointer<*> -> value.array
                is Float, is Double, is Long, is Int, is Byte -> value
                else -> throw UnsupportedOperationException()
            }
        }

        splitThreadInvocation(instances) { from, to ->
            try {
                execMethod.invoke(null,
                    from + indexOffset,
                    to + indexOffset,
                    *arrays.toTypedArray()
                )
            }catch (e: InvocationTargetException){
                throw e.targetException
            }
        }
    }

    override fun dealloc() = Unit

    override fun stringifyFunctionStatement(statement: FunctionStatement, buffer: StringBuilder){
        val function = statement.function
        val name: String
        val args: List<String>
        if(function.name == "main"){
            name = "__m"
            args = listOf("int i")
        }else {
            name = function.name
            args = function.arguments.map(::convertToFuncArg)
        }
        appendCFunctionHeader(
            buffer = buffer,
            modifiers = listOf("private", "static", "final") + function.modifiers.map { it.text },
            type = function.returnType.text,
            name = name,
            args = args
        )
        stringifyScopeStatement(buffer, function.body, true)
    }

    private fun transformKernelArg(field: Field) =
        "${toCType(field.type)} __v_${field.name}"

    private fun Type.toJavaClass() = when(this) {
        Type.VOID -> Unit::class.java
        Type.FLOAT -> Float::class.java
        Type.INT -> Int::class.java
        Type.BYTE -> Byte::class.java
        Type.BOOLEAN -> Boolean::class.java
        Type.FLOAT_ARRAY -> FloatArray::class.java
        Type.INT_ARRAY -> IntArray::class.java
        Type.BYTE_ARRAY -> ByteArray::class.java
        Type.BOOLEAN_ARRAY -> BooleanArray::class.java
    }

    override fun toCType(type: Type) = when (type) {
        Type.VOID -> "void"
        Type.FLOAT -> "float"
        Type.INT -> "int"
        Type.BYTE -> "byte"
        Type.BOOLEAN -> "boolean"
        Type.FLOAT_ARRAY -> "float[]"
        Type.INT_ARRAY -> "int[]"
        Type.BYTE_ARRAY -> "byte[]"
        Type.BOOLEAN_ARRAY -> "boolean[]"
    }

    override fun toCArrayName(name: String) = name

    override fun toCModifier(modifier: Modifiers) = when(modifier){
        Modifiers.CONST -> "private static final"
        Modifiers.EXTERNAL -> "private static"
        Modifiers.READONLY -> ""
    }
}