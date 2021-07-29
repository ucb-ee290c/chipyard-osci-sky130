DIR=build
VTOP=Digital
YML="digital.sky130.yml sky130-files/sky130.yml"

make CONFIG=EE290CBLEConfig VLSI_TOP=$VTOP VLSI_OBJ_DIR=$DIR syn
make CONFIG=EE290CBLEConfig VLSI_TOP=$VTOP VLSI_OBJ_DIR=$DIR par
make CONFIG=EE290CBLEConfig VLSI_TOP=$VTOP VLSI_OBJ_DIR=$DIR drc
make CONFIG=EE290CBLEConfig VLSI_TOP=$VTOP VLSI_OBJ_DIR=$DIR lvs

# DRC
# /tools/mentor/calibre/aoi_cal_2020.3_16.11/bin/calibre -64 -drc -hier -turbo 12 /tools/B/nayiri/sky130/chipyard-osci-sky130/vlsi/$DIR/chipyard.TestHarness.EE290CBLEConfig-$VTOP/drc-rundir/drc_run_file

# V2LVS
# /tools/mentor/calibre/aoi_cal_2016.4_15.11/bin/v2lvs -v /tools/B/nayiri/sky130/chipyard-osci-sky130/vlsi/$DIR/chipyard.TestHarness.EE290CBLEConfig-$VTOP/par-rundir/$VTOP.lvs.v -o /tools/B/nayiri/sky130/chipyard-osci-sky130/vlsi/$DIR/chipyard.TestHarness.EE290CBLEConfig-$VTOP/lvs-rundir/$VTOP.lvs.sp -w 2

# # LVS
# /tools/mentor/calibre/aoi_cal_2016.4_15.11/bin/calibre -64 -lvs -hier -turbo 12 -hyper /tools/B/nayiri/sky130/chipyard-osci-sky130/vlsi/$DIR/chipyard.TestHarness.EE290CBLEConfig-$VTOP/lvs-rundir/lvs_run_file