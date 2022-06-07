distctan:
	cd .. && mkdir pax
	cd .. && rsync -rL `pwd`/* --exclude '.git/' --exclude 'build/' --exclude '*.zip' -L pax/
	cd ../pax/source && ant distctan
	rm -rf ../pax/source/build
	rm -rf ../pax/pax
	cd .. && zip -r pax-`date +"%Y%m%d"`.zip pax/
	
clean:
	cd .. && rm -rf pax
