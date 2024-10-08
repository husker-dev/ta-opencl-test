package sma

import com.huskerdev.gpkt.GPSyncDevice
import com.huskerdev.gpkt.GPType
import org.openjdk.jmh.annotations.*


@State(Scope.Benchmark)
open class SMA_INTERPRETER {
    private lateinit var gp: GP

    @Setup
    open fun prepare() {
        gp = GP(GPSyncDevice.create(requestedType = arrayOf(GPType.Interpreter))!!)
    }

    @Benchmark
    open fun exec() =
        gp.execute()

    @TearDown
    open fun cleanup() =
        gp.cleanup()
}