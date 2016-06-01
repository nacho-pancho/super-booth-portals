SRC := $(wildcard net/ddns/gongorg/*.java)

all:
	#javac -cp ../spigot-api.jar:../../craftbukkit-1.9.2.jar:. $(SRC)
	javac -cp ../../craftbukkit-1.9.2.jar:. $(SRC)
	jar cf ../../plugins.available/BoothPortals-0.2.jar *.yml *.properties net

