Feel free to setup the backend components here

# Kafka Setup

## Add Kafka binary files to your $PATH
Setup the `$PATH` environment variable
In order to easily access the Kafka binaries, you can edit your `PATH` variable by adding the following line (edit the content to your system) to your system run commands (for example `~/.zshrc` if you use zshrc):

To make it easier, run `pwd` on the `Backend` directory, copy the output and append `/kafka_2.12-3.6.1/bin` to it.

`export PATH="$PATH:/Users/PATH_TO_DIR/Smart-Thermostat-System/Backend/kafka_2.12-3.6.1/bin"`

This ensures that you can now run the kafka commands without prefixing them.

After reloading your terminal, the following should work from any directory `kafka-topics.sh`

Before starting, make sure you are on the `Backend` directory (`cd Backend`) 
and run the following commands to get started with Kafka

## Run the Zookeeper Server (a service to maintain the main kafka server)
`zookeeper-server-start.sh ./kafka_2.12-3.6.1/config/zookeeper.properties
`

## Start the Kafka server (main kafka server with the queue)
`kafka-server-start.sh ./kafka_2.12-3.6.1/config/server.properties
`

### Optional: Testing that Kafka works
Run the producer (which is basically a message intake to the queue)
``

