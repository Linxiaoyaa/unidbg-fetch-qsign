package com.tencent.mobileqq.dt

import com.github.unidbg.linux.android.dvm.DvmObject
import moe.fuqiuluo.unidbg.QSecVM

object Dtn {
    fun initContext(vm: QSecVM, context: DvmObject<*>) {
        // 这里的路径必须和 FEKit.java 第 535 行保持一致
        // 文件夹名称：5463306EE50FE3AA
        val filesDir = "/data/user/0/${vm.envData.packageName}/files/5463306EE50FE3AA"

        val dtnInstance = vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)

        // 修正 1: 方法名改为 initNativeContext
        // 签名通常是 (Landroid/content/Context;Ljava/lang/String;)V
        dtnInstance.callJniMethod(
            vm.emulator,
            "initNativeContext(Landroid/content/Context;Ljava/lang/String;)V",
            context,
            filesDir
        )
    }

    fun initLog(vm: QSecVM,context: DvmObject<*>, logger: DvmObject<*>) {
        // 修正 2: 方法名改为 initNativeLog
        vm.newInstance("com/tencent/mobileqq/dt/Dtn", unique = true)
            .callJniMethod(vm.emulator, "initNativeLog(Landroid/content/Context;Lcom/tencent/mobileqq/fe/IFEKitLog;)V", context, logger)
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