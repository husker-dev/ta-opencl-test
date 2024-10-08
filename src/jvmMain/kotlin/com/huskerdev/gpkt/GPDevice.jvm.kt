package com.huskerdev.gpkt

import com.huskerdev.gpkt.engines.cuda.Cuda
import com.huskerdev.gpkt.engines.cuda.CudaAsyncDevice
import com.huskerdev.gpkt.engines.cuda.CudaSyncDevice
import com.huskerdev.gpkt.engines.jdk.ClassCompiler
import com.huskerdev.gpkt.engines.jdk.JavacAsyncDevice
import com.huskerdev.gpkt.engines.jdk.JavacSyncDevice
import com.huskerdev.gpkt.engines.opencl.OpenCLSyncDevice
import com.huskerdev.gpkt.engines.opencl.OpenCL
import com.huskerdev.gpkt.engines.opencl.OpenCLAsyncDevice
import com.huskerdev.gpkt.engines.opengl.OpenGLAsyncDevice
import com.huskerdev.gpkt.engines.opengl.OpenGLSyncDevice

internal actual val defaultExpectedTypes: Array<GPType> =
    System.getenv().getOrDefault("gp.order", "cuda,opencl,javac,interpreter")
        .split(",").map {
            GPType.mapped.getOrElse(it) { throw Exception("Unknown GPType: '$it'") }
        }.toTypedArray()

internal actual val defaultExpectedDeviceId: Int =
    System.getenv().getOrDefault("gp.index", "0").toInt()

internal actual fun createSupportedSyncInstance(
    requestedDeviceId: Int,
    requestedType: Array<out GPType>
): GPSyncDevice? = requestedType.firstNotNullOfOrNull {
    when {
        it == GPType.Javac && ClassCompiler.supported -> return JavacSyncDevice()
        it == GPType.OpenCL && OpenCL.supported -> return OpenCLSyncDevice(requestedDeviceId)
        it == GPType.CUDA && Cuda.supported -> return CudaSyncDevice(requestedDeviceId)
        it == GPType.OpenGL -> return OpenGLSyncDevice()
        else -> null
    }
}

internal actual suspend fun createSupportedAsyncInstance(
    requestedDeviceId: Int,
    requestedType: Array<out GPType>
): GPAsyncDevice? = requestedType.firstNotNullOfOrNull {
    when {
        it == GPType.Javac && ClassCompiler.supported -> return JavacAsyncDevice()
        it == GPType.OpenCL && OpenCL.supported -> return OpenCLAsyncDevice(requestedDeviceId)
        it == GPType.CUDA && Cuda.supported -> return CudaAsyncDevice(requestedDeviceId)
        it == GPType.OpenGL -> return OpenGLAsyncDevice()
        else -> null
    }
}