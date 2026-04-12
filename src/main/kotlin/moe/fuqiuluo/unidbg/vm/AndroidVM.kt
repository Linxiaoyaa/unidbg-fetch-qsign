package moe.fuqiuluo.unidbg.vm

import CONFIG
import com.github.unidbg.arm.backend.DynarmicFactory
import com.github.unidbg.arm.backend.KvmFactory
import com.github.unidbg.arm.backend.Unicorn2Factory
import com.github.unidbg.linux.android.AndroidEmulatorBuilder
import com.github.unidbg.linux.android.dvm.DalvikModule
import com.github.unidbg.linux.android.dvm.DvmClass
import com.github.unidbg.virtualmodule.android.AndroidModule
import java.io.Closeable
import java.io.File

open class AndroidVM(packageName: String, dynarmic: Boolean, unicorn: Boolean, kvm: Boolean, is64Bit : Boolean) : Closeable {
    internal val emulator = (if (is64Bit) {
        AndroidEmulatorBuilder.for64Bit()
    } else {
        AndroidEmulatorBuilder.for32Bit()
    })
        .setProcessName(packageName)
        .apply {
            if (kvm) {
                addBackendFactory(KvmFactory(true))
            } else if (dynarmic) {
                addBackendFactory(DynarmicFactory(true))
            } else if (unicorn) {
                addBackendFactory(Unicorn2Factory(true))
            }

        }
        .build()!!
    protected val memory = emulator.memory!!
    internal val vm = emulator.createDalvikVM()!!

    init {
        if (CONFIG.unidbg.debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
        }
        vm.setVerbose(CONFIG.unidbg.debug)
        val syscall = emulator.syscallHandler
        syscall.isVerbose = CONFIG.unidbg.debug
        syscall.setEnableThreadDispatcher(true)
        AndroidModule(emulator, vm).register(memory)
    }

    fun loadLibrary(soFile: File): DalvikModule {
        val dm = vm.loadLibrary(soFile, false)
        dm.callJNI_OnLoad(emulator)
        return dm
    }

    fun findClass(name: String, vararg interfaces: DvmClass): DvmClass {
        return vm.resolveClass(name, *interfaces)
    }

    override fun close() {
        this.emulator.close()
    }
}