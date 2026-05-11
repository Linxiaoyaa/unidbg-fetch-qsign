package moe.fuqiuluo.unidbg

import com.github.unidbg.Emulator
import com.github.unidbg.arm.Arm64Hook
import com.github.unidbg.arm.ArmHook
import com.github.unidbg.arm.HookStatus
import com.github.unidbg.arm.context.RegisterContext
import com.github.unidbg.hook.HookListener
import com.github.unidbg.linux.android.dvm.DvmObject
import com.github.unidbg.memory.SvcMemory
import com.tencent.mobileqq.qsec.qsecurity.DeepSleepDetector
import moe.fuqiuluo.comm.EnvData
import moe.fuqiuluo.unidbg.env.FileResolver
import moe.fuqiuluo.unidbg.env.QSecJni
import moe.fuqiuluo.unidbg.vm.AndroidVM
import moe.fuqiuluo.unidbg.vm.GlobalData
import java.io.File
import javax.security.auth.Destroyable
import kotlin.system.exitProcess

class QSecVM(
    val coreLibPath: File,
    val envData: EnvData,
    dynamic: Boolean,
    unicorn: Boolean,
    kvm: Boolean,
    is64Bit: Boolean
) : Destroyable, AndroidVM(envData.packageName, dynamic, unicorn, kvm, is64Bit) {
    private var destroy: Boolean = false
    private var isInit: Boolean = false
    internal val global = GlobalData()

    init {
        runCatching {
            val resolver = FileResolver(23, this@QSecVM)
            memory.setLibraryResolver(resolver)
            memory.addHookListener(object : HookListener {
                override fun hook(svcMemory: SvcMemory, p1: String?, p2: String?, p3: Long): Long {
                    if (p2 == "memcmp") {
                        val hookObj = if (emulator.is64Bit) {
                            object : Arm64Hook() {
                                override fun hook(emulator: Emulator<*>): HookStatus {
                                    val context = emulator.getContext<RegisterContext>()
                                    val arg1 = context.getLongArg(0)
                                    val arg2 = context.getLongArg(1)
                                    return if (arg1 > 0x100000000L || arg2 > 0x100000000L) {
                                        HookStatus.LR(emulator, -1)
                                    } else {
                                        HookStatus.RET(emulator, p3)
                                    }
                                }
                            }
                        } else {
                            object : ArmHook() {
                                override fun hook(emulator: Emulator<*>): HookStatus {
                                    return HookStatus.RET(emulator, p3)
                                }
                            }
                        }
                        return svcMemory.registerSvc(hookObj).peer
                    }
                    return 0
                }
            })

            emulator.syscallHandler.addIOResolver(resolver)
            vm.setJni(QSecJni(envData, this, global))

            if (envData.packageName == "com.tencent.mobileqq") {
                println("Running for Mobile QQ VM")
            } else {
                vm.addNotFoundClass("com/tencent/mobileqq/dt/Dc")
                vm.addNotFoundClass("com/tencent/mobileqq/dt/Dte")
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun init() {
        if (isInit) return
        runCatching {
            coreLibPath.resolve("libpoxy.so").let {
                if (it.exists()) {
                    loadLibrary(it)
                }
            }
            loadLibrary(coreLibPath.resolve("libfekit.so"))
            global["DeepSleepDetector"] = DeepSleepDetector()


            this.isInit = true
        }.onFailure {
            it.printStackTrace()
            exitProcess(1)
        }
    }

    fun newInstance(name: String, value: Any? = null, unique: Boolean = false): DvmObject<*> {
        if (unique && name in global) {
            return global[name] as DvmObject<*>
        }
        val obj = findClass(name).newObject(value)
        if (unique) {
            global[name] = obj
        }
        vm.setVerbose(true)
        return obj
    }

    override fun isDestroyed(): Boolean = destroy

    override fun destroy() {
        if (isDestroyed) return
        this.destroy = true
        this.close()
    }
}
