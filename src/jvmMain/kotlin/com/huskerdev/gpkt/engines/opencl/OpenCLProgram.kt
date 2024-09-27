package com.huskerdev.gpkt.engines.opencl

import com.huskerdev.gpkt.FieldNotSetException
import com.huskerdev.gpkt.SimpleCProgram
import com.huskerdev.gpkt.TypesMismatchException
import com.huskerdev.gpkt.ast.objects.Field
import com.huskerdev.gpkt.ast.objects.Function
import com.huskerdev.gpkt.ast.objects.Scope
import com.huskerdev.gpkt.ast.types.Modifiers
import com.huskerdev.gpkt.utils.appendCFunctionHeader
import org.jocl.Pointer
import org.jocl.Sizeof
import org.jocl.cl_kernel
import org.jocl.cl_program

class OpenCLProgram(
    private val cl: OpenCL,
    ast: Scope
): SimpleCProgram(ast) {
    private val program: cl_program
    private val kernel: cl_kernel

    init {
        val buffer = StringBuilder()
        stringifyScope(ast, buffer)

        program = cl.compileProgram(buffer.toString())
        kernel = cl.createKernel(program, "__m")
    }

    override fun execute(instances: Int, vararg mapping: Pair<String, Any>) {
        val map = hashMapOf(*mapping)

        buffers.forEachIndexed { i, field ->
            val value = map.getOrElse(field.name) { throw FieldNotSetException(field.name) }
            if(!areEqualTypes(value, field.type))
                throw TypesMismatchException(field.name)

            if(value !is OpenCLMemoryPointer) {
                val (size, ptr) = when(value){
                    is Float -> Sizeof.cl_float.toLong() to Pointer.to(floatArrayOf(value))
                    is Double -> Sizeof.cl_double.toLong() to Pointer.to(doubleArrayOf(value))
                    is Long -> Sizeof.cl_long.toLong() to Pointer.to(longArrayOf(value))
                    is Int -> Sizeof.cl_int.toLong() to Pointer.to(intArrayOf(value))
                    is Byte -> Sizeof.cl_char.toLong() to Pointer.to(byteArrayOf(value))
                    else -> throw UnsupportedOperationException()
                }
                cl.setArgument(kernel, i, size, ptr)
            }else
                cl.setArgument(kernel, i, value.ptr)
        }
        cl.executeKernel(kernel, instances.toLong())
    }

    override fun dealloc() {
        cl.dealloc(program)
        cl.dealloc(kernel)
    }

    override fun stringifyFunction(function: Function, buffer: StringBuilder){
        if(function.name == "main"){
            appendCFunctionHeader(
                buffer = buffer,
                modifiers = listOf("__kernel"),
                type = function.returnType.text,
                name = "__m",
                args = buffers.map(::transformKernelArg)
            )
            buffers.forEach {
                buffer.append("${it.name}=__v_${it.name};")
            }
            buffer.append("int ${function.arguments[0].name}=get_global_id(0);")
            stringifyScope(function, buffer)
            buffer.append("}")
        } else super.stringifyFunction(function, buffer)
    }

    private fun transformKernelArg(field: Field): String{
        return if(field.type.isArray)
            "__global ${toCType(field.type)}*__v_${field.name}"
        else
            "${toCType(field.type)} __v_${field.name}"
    }

    override fun toCModifier(modifier: Modifiers) = when(modifier){
        Modifiers.EXTERNAL -> "__global"
        Modifiers.CONST -> "__constant"
    }
}