package com.tencent.mobileqq.dt

import com.github.unidbg.linux.android.dvm.DvmObject
import moe.fuqiuluo.unidbg.QSecVM

object Dtn {
    fun initContext(vm: QSecVM, context: DvmObject<*>) {
        val filesDir = "/data/user/0/${vm.envData.packageName}/files/5463306EE50FE3AA"
        val dtnInstance = vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)
        if(vm.envData.packageName == "com.tencent.tim"){
            dtnInstance.callJniMethod(
                vm.emulator,
                "initContext(Landroid/content/Context;Ljava/lang/String;)V",
                context,
                ""
            )
        } else if(vm.envData.packageName == "com.tencent.qqlite"){
            dtnInstance.callJniMethod(
                vm.emulator,
                "initContext(Landroid/content/Context;Ljava/lang/String;)V",
                context,
                ""
            )
        }else {
            dtnInstance.callJniMethod(
                vm.emulator,
                "initNativeContext(Landroid/content/Context;Ljava/lang/String;)V",
                context,
                filesDir
            )
        }
    }

    fun initLog(vm: QSecVM,context: DvmObject<*>, logger: DvmObject<*>) {
        if(vm.envData.packageName == "com.tencent.mobileqq") {
            vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)
                .callJniMethod(
                    vm.emulator,
                    "initNativeLog(Landroid/content/Context;Lcom/tencent/mobileqq/fe/IFEKitLog;)V",
                    context,
                    logger
                )
        }else {
            vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)
                .callJniMethod(
                    vm.emulator,
                    "initLog(Lcom/tencent/mobileqq/fe/IFEKitLog;)V",

                    logger
                )
        }
    }

    fun initUin(vm: QSecVM, uin: String) {
        vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)
            .callJniMethod(vm.emulator, "initUin(Ljava/lang/String;)V", uin)
    }
    fun initFinally(vm: QSecVM) {
        vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)
            .callJniMethod(vm.emulator, "initfinally()V")
    }
}