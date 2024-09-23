package benchmark

import com.huskerdev.gpkt.GPEngine
import com.huskerdev.gpkt.GPType
import org.openjdk.jmh.annotations.*


@State(Scope.Benchmark)
open class SMA_CL {
    private lateinit var gp: GP

    @Setup
    open fun prepare() {
        gp = GP(GPEngine.create(GPType.OpenCL)!!)
    }

    @Benchmark
    open fun execute() =
        gp.execute()

    @TearDown
    open fun cleanup() =
        gp.cleanup()
}