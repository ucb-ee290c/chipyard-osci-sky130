
# Build the simulation binary 
make CONFIG=EE290CBLEConfig

# Create an all-zero, 1GB "SPI Flash Image"
# This needs to be *bigger* than some threshold, for some reason 
truncate -s 1G nullbytes

# And run some sim! 
./simv-chipyard-EE290CBLEConfig \
	+permissive \
	+dramsim \
	+dramsim_ini_dir=../../generators/testchipip/src/main/resources/dramsim2_ini \
	+spiflash0=./nullbytes \
	+ee290c_bsel=0 \
	+max-cycles=10000000 \
	+ntb_random_seed_automatic +verbose +permissive-off \
	/tools/B/nayiri/chipyard-tools/toolchains/riscv-tools/riscv-tests/isa/rv32ui-p-add

##$RISCV/riscv64-unknown-elf/share/riscv-tests/isa/rv64ui-p-simple


