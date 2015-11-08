# EC2 Snapshotter

This program allows for a simple mechanism to snapshot all volumes in your EC2 environment. 
This can run on any server, but will backup any volumes that have a tag with a key of `Backup` 
and a value of `True`.

The program will also automatically clean up all snapshots it has created that are older than 
one month. It does this by only deleting volumes with the tag key of `Snapshotter` and a value 
of `True` which have a snapshot start time of over one month ago.

Included in this package is an AWS Lambda handler that can be setup to automatically perform 
the backups via a Scheduled Event source.

## System Requirements

You must have Java 8 on your system classpath to run this program.

## Building the program

Currently we only support building through Maven. To package this into a runnable JAR that 
also contains all of the dependencies, simply run the following command from the directory 
containing the `pom.xml` file.

`mvn clean package`

The resulting JAR file will be available in the `target` directory.

## Running the program

`java -jar ec2-snapshotter.jar [AWS-Key AWS-Secret]`

The AWS key and secret are optional and when omitted, the program will attempt to use the 
EC2 instance profile's credentials. Make sure you have the appropriate permissions on your 
IAM role to allow listing volumes, creating and deleting snapshots, and tagging resources.

## Scheduling the Backup using AWS Lambda

You will need to setup an IAM role that has the appropriate AWS Lambda trust relationship 
and the appropriate permissions to log events to CloudWatch, read volumes, create tags, 
read tags, create and delete snapshots. The simplest way to do this is to create a new 
AWS Lambda service role and give it the AmazonEC2FullAccess managed policy. You may 
create a more limiting role if you desire that.

With the role in place, create a new Java 8 runtime lambda function and upload the built 
version of this project. After uploading, you can run a Test to verify it creates all 
expected snapshots instantly. Depending on how many volumes you plan to backup, you might 
need to adjust the Timeout configuration. I'd suggest at least a minute. All other defaults 
should be sufficient. The Handler value is:

`com.marcbaumbach.ec2.snapshotter.LambdaHandler::performBackup`

Finally, under the Event sources, create a new event source and select Scheduled Event. You 
may select any schedule you desire the backup to run on.

## Scheduling the Backup using Cron

On Linux, you can use `cron` to perform scheduled backups. The following crontab entry will 
run every night at midnight server local time.

`0 0 * * * java -jar /path/to/program/ec2-snapshotter.jar`

By default, all logging is sent to standard out, which will be logged in the cron log.

If you want logs to be saved on the server in a different location, you may want to adjust 
the included log4j.xml file to use the `file` appender in place of the `LAMBDA` one. 
Additionally, you may override with your own log4j configuration as part of your script 
call in the crontab.

## License

The EC2 Snapshotter is licensed under the Apache 2.0 License.