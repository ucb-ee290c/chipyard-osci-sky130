package chipyard.harness

import chisel3._
import chisel3.experimental.{Analog, BaseModule}

import freechips.rocketchip.config.{Field, Config, Parameters}
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImpLike}
import freechips.rocketchip.amba.axi4.{AXI4Bundle, AXI4SlaveNode, AXI4MasterNode, AXI4EdgeParameters}
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.system.{SimAXIMem}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.PlusArg

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.spi._

import barstools.iocell.chisel._

import testchipip._

import chipyard.HasHarnessSignalReferences
import chipyard.iobinders.GetSystemParameters

import tracegen.{TraceGenSystemModuleImp}
import icenet.{CanHavePeripheryIceNIC, SimNetwork, NicLoopback, NICKey, NICIOvonly}

import scala.reflect.{ClassTag}

import chipyard._


class WithTiedBSel extends OverrideHarnessBinder({
  (system: HasPeripheryEE290CBLEModuleImp, th: HasHarnessSignalReferences, ports: Seq[Data]) => {
    ports.map {
      case b: Bool =>
        b := true.B
      case d: HasPeripheryEE290CBLEBundle =>
        d.tieoffBoot()
    }
  }
})

class WithPlusArgBSel extends OverrideHarnessBinder({
  val plusarg_bsel = PlusArg("ee290c_bsel", default = 1, "1:SPI 0:TSI.")(0)

  (system: HasPeripheryEE290CBLEModuleImp, th: HasHarnessSignalReferences, ports: Seq[Data]) => {
    ports.map {
      case b: Bool =>
        b := plusarg_bsel
      case d: HasPeripheryEE290CBLEBundle =>
        d.boot := plusarg_bsel
    }
  }
})
