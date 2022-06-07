dist:
	cd .. && mkdir build
	cd .. && rsync --exclude '.git' --exclude 'build' -L . build/
	cd ../build && symlink
clean:
	cd .. && rm build
