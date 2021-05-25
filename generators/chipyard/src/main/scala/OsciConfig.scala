package chipyard

import freechips.rocketchip.config.{Config}
import freechips.rocketchip.config._

import Chisel._
import chisel3._

import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy.{LazyModule, AddressSet}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.util._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.subsystem._
import freechips.rocketchip.config.{Field, Config}
import freechips.rocketchip.tilelink.{TLRAM}
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.uart._

// EE290C
//import dma._
// import aes._
// import baseband._

case class SBusScratchpadParams(
  base: BigInt,
  mask: BigInt)

case object SBusScratchpadKey extends Field[Option[SBusScratchpadParams]](None)

/**
 * Trait to add a scratchpad on the sbus
 */
trait CanHaveSBusScratchpad { this: BaseSubsystem =>
  private val portName = "Backing-Scratchpad"

  val spadOpt = p(SBusScratchpadKey).map { param =>
    val spad = LazyModule(new TLRAM(address=AddressSet(param.base, param.mask), beatBytes=sbus.beatBytes, cacheable=true, executable=true))
    sbus.toVariableWidthSlave(Some(portName)) { spad.node }
    spad
  }
}

class WithSBusScratchpad(base: BigInt = 0x80000000L, mask: BigInt = ((32 << 10) - 1)) extends Config((site, here, up) => {
  case SBusScratchpadKey => Some(SBusScratchpadParams(base, mask))
})


class WithEE290CBootROM extends Config((site, here, up) => {
  case BootROMLocated(x) => up(BootROMLocated(x), site).map(_.copy(contentFileName = s"./generators/chipyard/src/main/scala/ee290c/bootrom/bootrom.rv${site(XLen)}.img"))
})

class WithNGPIOs(width: Int = 2) extends Config((site, here, up) => {
  case PeripheryGPIOKey => Seq(
    GPIOParams(address = 0x10012000, width = width, includeIOF = false))
})

class WithNEntryUART(nTxEntries: Int = 32, nRxEntries: Int = 32, baudrate: BigInt = 115200) extends Config((site, here, up) => {
  case PeripheryUARTKey => Seq(
    UARTParams(address = 0x54000000L, nTxEntries = nTxEntries, nRxEntries = nRxEntries, initBaudRate = baudrate))
})

class OsciCore extends Config((site, here, up) => {
  case XLen => 32
  case RocketTilesKey => List(RocketTileParams(
      core = RocketCoreParams(
        useVM = false,
        fpu = Some(FPUParams(minFLen = 32, fLen = 32, divSqrt = false, sfmaLatency = 4)),
        mulDiv = Some(MulDivParams(
          mulUnroll = 8,
          mulEarlyOut = true,
          divEarlyOut = true))),
      btb = None,
      dcache = Some(DCacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = ((8 << 10) >> 6), // 8k
        nWays = 1,
        nTLBSets = 1,
        nTLBWays = 4,
        nMSHRs = 0,
        blockBytes = 64,
        scratch = Some(0x80000000L))),
      icache = Some(ICacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = ((8 << 10) >> 6), // 8k
        nWays = 1,
        nTLBSets = 1,
        nTLBWays = 4,
        blockBytes = 64))))
})

/// Osci-Bear Adapted Configuration 
/// FIXME: all commented sections to be reinstated 
class OsciConfig extends Config(
  // new baseband.WithBLEBasebandModem ++
  // new chipyard.iobinders.WithBLEBasebandModemPunchthrough() ++
  // new chipyard.harness.WithBLEBasebandModemTiedOff ++
  // new aes.WithAESAccel ++
  // new WithBSel ++

  new WithNGPIOs(3) ++                                         // 2 GPIO pins
  new chipyard.config.WithSPIFlash ++
  new chipyard.config.WithTLSerialLocation(
    freechips.rocketchip.subsystem.FBUS,
    freechips.rocketchip.subsystem.PBUS) ++                    // attach TL serial adapter to f/p busses
  new chipyard.WithMulticlockIncoherentBusTopology ++          // use incoherent bus topology
  new freechips.rocketchip.subsystem.WithNoMemPort ++          // remove backing memory
  new OsciCore ++                                             // single tiny rocket-core

  new chipyard.config.WithTileFrequency(20.0) ++
  new chipyard.config.WithPeripheryBusFrequency(20.0)++
  new chipyard.config.WithMemoryBusFrequency(20.0) ++
  new chipyard.config.WithSystemBusFrequency(20.0) ++
  new chipyard.config.WithFrontBusFrequency(20.0) ++
  new chipyard.config.WithControlBusFrequency(20.0) ++

  // From the AbstractConfig:
  // The HarnessBinders control generation of hardware in the TestHarness
  new chipyard.harness.WithSimSPIFlashModel(false) ++          // add the SPI flash model in the harness (writeable)
  new chipyard.harness.WithUARTAdapter ++                      // add UART adapter to display UART on stdout, if uart is present
  new chipyard.harness.WithSimSerial ++                        // add external serial-adapter and RAM
  new chipyard.harness.WithSimDebug ++                         // add SimJTAG or SimDTM adapters if debug module is enabled
  new chipyard.harness.WithGPIOTiedOff ++                      // tie-off chiptop GPIOs, if GPIOs are present
  new chipyard.harness.WithSimSPIFlashModel ++                 // add simulated SPI flash memory, if SPI is enabled
  new chipyard.harness.WithTieOffInterrupts ++                 // tie-off interrupt ports, if present
  
  // new chipyard.harness.WithPlusArgBSel ++

  // The IOBinders instantiate ChipTop IOs to match desired digital IOs
  // IOCells are generated for "Chip-like" IOs, while simulation-only IOs are directly punched through
  new chipyard.iobinders.WithBlockDeviceIOPunchthrough ++
  new chipyard.iobinders.WithNICIOPunchthrough ++
  new chipyard.iobinders.WithSerialTLIOCells ++
  new chipyard.iobinders.WithDebugIOCells ++
  new chipyard.iobinders.WithGPIOCells ++
  new chipyard.iobinders.WithUARTIOCells ++
  new chipyard.iobinders.WithSPIIOCells ++
  // new chipyard.iobinders.WithBSelIOCells ++
  
  new chipyard.iobinders.WithTraceIOPunchthrough ++
  new chipyard.iobinders.WithExtInterruptIOCells ++

  new testchipip.WithSerialTLWidth(1) ++
  new testchipip.WithDefaultSerialTL ++                          // use serialized tilelink port to external serialadapter/harnessRAM0

  // new WithEE290CBootROM ++                                       // use our bootrom

  new WithNEntryUART(32, 32) ++                                // add a UART
  new chipyard.config.WithNoSubsystemDrivenClocks ++             // drive the subsystem diplomatic clocks from ChipTop instead of using implicit clocks
  new chipyard.config.WithInheritBusFrequencyAssignments ++      // Unspecified clocks within a bus will receive the bus frequency if set
  new chipyard.config.WithPeripheryBusFrequencyAsDefault ++      // Unspecified frequencies with match the pbus frequency (which is always set)
  new freechips.rocketchip.subsystem.WithJtagDTM ++              // set the debug module to expose a JTAG port
  new freechips.rocketchip.subsystem.WithNoMMIOPort ++           // no top-level MMIO master port (overrides default set in rocketchip)
  new freechips.rocketchip.subsystem.WithNoSlavePort ++          // no top-level MMIO slave port (overrides default set in rocketchip)
  new freechips.rocketchip.subsystem.WithNExtTopInterrupts(0) ++ // no external interrupts
  new freechips.rocketchip.system.BaseConfig                     // "base" rocketchip system
)

