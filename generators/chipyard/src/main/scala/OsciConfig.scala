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

import freechips.rocketchip.tile.OpcodeSet

// EE290C
// import dma.EE290CDMA
// import aes.AESAccel


// EE290C
// import dma.EE290CDMA
// import aes._

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

class OsciCore extends Config((site, here, up) => {
  case XLen => 32
  case RocketTilesKey => List(RocketTileParams(
      core = RocketCoreParams(
        useVM = false,
        fpu = Some(FPUParams(fLen = 32, divSqrt = false)),
        mulDiv = Some(MulDivParams(mulUnroll = 8))),
      btb = None,
      dcache = Some(DCacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = ((32 << 10) >> 6), // 32K (32K / 64 bytes)
        nWays = 1,
        nTLBSets = 1,
        nTLBWays = 4,
        nMSHRs = 0,
        blockBytes = site(CacheBlockBytes),
        scratch = Some(0x80000000L))),
      icache = Some(ICacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = ((32 << 10) >> 6), // 32K (32K / 64 bytes)
        nWays = 1,
        nTLBSets = 1,
        nTLBWays = 4,
        blockBytes = site(CacheBlockBytes),
        ))))
  case RocketCrossingKey => List(RocketCrossingParams(
    crossingType = SynchronousCrossing(),
    master = TileMasterPortParams()
  ))
})


class OsciConfig extends Config(
  // new EE290CDMA ++ // or wrap this an RF interface module??
  // new AESAccel ++ // TODO: set params - opcode, p
  new WithSBusScratchpad ++                                    // add sbus backing scratchpad
  new chipyard.config.WithTLSerialLocation(
    freechips.rocketchip.subsystem.FBUS,
    freechips.rocketchip.subsystem.PBUS) ++                    // attach TL serial adapter to f/p busses
  new chipyard.WithMulticlockIncoherentBusTopology ++          // use incoherent bus topology
  new freechips.rocketchip.subsystem.WithNBanks(0) ++          // remove L2$
  new freechips.rocketchip.subsystem.WithNoMemPort ++          // remove backing memory
  new OsciCore ++                                             // single tiny rocket-core

  new chipyard.config.AbstractConfig)

