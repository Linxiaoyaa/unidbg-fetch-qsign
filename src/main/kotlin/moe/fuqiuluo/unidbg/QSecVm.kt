package moe.fuqiuluo.unidbg

import com.github.unidbg.Emulator
import com.github.unidbg.arm.Arm64Hook
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
import java.io.FileOutputStream
import java.io.PrintStream
import javax.security.auth.Destroyable
import kotlin.system.exitProcess

class QSecVM(
    val coreLibPath: File,
    val envData: EnvData,
    dynamic: Boolean,
    unicorn: Boolean,
    kvm: Boolean
): Destroyable, AndroidVM(envData.packageName, dynamic, unicorn,kvm) {
    private var destroy: Boolean = false
    private var isInit: Boolean = false
    internal val global = GlobalData()

    init {
        runCatching {
            val resolver = FileResolver(23, this@QSecVM)
            memory.setLibraryResolver(resolver)
            memory.addHookListener(object : HookListener {
                override fun hook(svcMemory: SvcMemory, p1: String?, p2: String?, p3: Long): Long {
                    if(p2 == "memcmp"){
                        return svcMemory.registerSvc(object : Arm64Hook() {
                            override fun hook(emulator: Emulator<*>): HookStatus {
                                val context = emulator.getContext<RegisterContext>()
                                try {
                                    val arg1 = context.getLongArg(0)
                                    val arg2 = context.getLongArg(1)
                                    if(arg1>0x100000000 || arg2>0x100000000){
                                        return HookStatus.LR(emulator, -1)
                                    }
                                } catch (ignored: Exception) {
                                }
                                return HookStatus.RET(emulator, p3)
                            }
                        }).peer
                    }
                    return 0;
                }
            })

            emulator.syscallHandler.addIOResolver(resolver)
            vm.setJni(QSecJni(envData, this, global))

            if (envData.packageName == "com.tencent.mobileqq") {
                println("QSign-Unidbg 白名单模式")
                arrayOf(
                    "android/os/Build\$VERSION",
                    "android/content/pm/ApplicationInfo",
                    "com/tencent/mobileqq/fe/IFEKitLog",
                    "com/tencent/mobileqq/channel/ChannelProxy",
                    "com/tencent/mobileqq/qsec/qsecurity/QSec",
                    "com/tencent/mobileqq/qsec/qsecurity/QSecConfig",
                    "com/tencent/mobileqq/sign/QQSecuritySign\$SignResult",
                    "java/lang/String",
                    "com/tencent/mobileqq/qsec/qsecest/QsecEst",
                    "com/tencent/qqprotect/qsec/QSecFramework",
                    "com/tencent/mobileqq/dt/app/Dtc",
                    "android/provider/Settings\$System",
                    "com/tencent/mobileqq/fe/utils/DeepSleepDetector",
                    "com/tencent/mobileqq/dt/model/FEBound",
                    "java/lang/ClassLoader",
                    "java/lang/Thread",
                    "android/content/Context",
                    "android/content/ContentResolver",
                    "java/io/File",
                    "java/lang/Integer",
                    "java/lang/Object",
                    "java/util/ArrayList",
                    "android/os/Process",
                    "com/tencent/mobileqq/sign/QQSecuritySign",
                    "com/tencent/mobileqq/channel/ChannelManager",
                    "com/tencent/mobileqq/dt/Dtn",
                    "com/tencent/mobileqq/qsec/qsecdandelionsdk/Dandelion",
                    "com/tencent/mobileqq/qsec/qsecprotocol/ByteData",
                    "com/tencent/mobileqq/qsec/qseccodec/SecCipher",
                    "dalvik/system/BaseDexClassLoader",
                    "android/content/pm/IPackageManager\$Stub",
                    "android/os/ServiceManager",
                    "android/os/IBinder"
                ).forEach {
                    vm.addFilterFoundClass(it)
                }
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
        val module = vm.emulator.memory.findModule("libfekit.so")

//// 1. 定义输出文件（绝对不要直接打印到控制台，IDE会卡死）
//        val traceStream = PrintStream(FileOutputStream("fekit_trace.log"))
//
//// 2. 开启追踪（建议只追踪 libfekit.so 的内存范围）
//        val tracer = vm.emulator.traceCode(module.base, module.base + module.size)
//        tracer.setRedirect(traceStream)
        return obj
    }

    override fun isDestroyed(): Boolean = destroy

    override fun destroy() {
        if (isDestroyed) return
        this.destroy = true
        this.close()
    }
}
