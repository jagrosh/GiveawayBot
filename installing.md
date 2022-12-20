### Getting
#### GiveawayBot
```
git clone https://github.com/jagrosh/GiveawayBot .
rm -r .git*
```

#### Java
```
# Download Java
wget https://download.oracle.com/java/19/latest/jdk-19_linux-x64_bin.tar.gz

# Unpack
gunzip jdk-19_linux-x64_bin.tar.gz
tar xvf jdk-19_linux-x64_bin.tar

# Move
mv jdk-19* /usr/local/jdk

# Register the environment variables
ln -s /usr/local/jdk/bin/* /usr/bin

# Checking Java
java -version
```

#### Maven
```
# Download Maven
wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.zip

# Unpack
unzip apache-maven-3.8.6-bin.zip

# Move
mv apache-maven-3.8.6 /usr/local/mvn

# Register the environment variables
ln -s /usr/local/bin/mvn /usr/bin
ln -s /usr/local/bin/mvnDebug /usr/bin

# Checking Maven
mvn -v
```

### Installing
#### Compile
```
mvn package
```

#### Start Bot
```
java -jar target/GiveawayBot-4.0-jar-with-dependencies.jar
```