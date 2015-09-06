# EC2 Snapshotter

This program allows for a simple mechanism to snapshot all volumes in your EC2 environment. 
This can run on any server, but will backup any volumes that have a tag with a key of `Backup` 
and a value of `True`.

The program will also automatically clean up all snapshots it has created that are older than 
one month. It does this by only deleting volumes with the tag key of `Snapshotter` and a value 
of `True` which have a snapshot start time of over one month ago.

All actions performed by this program are logged to a `snapshotter.log` file. This file is 
automatically rolled and a single rolled file is kept at all times.

## System Requirements

You must have Java 8 on your system classpath to run this program.

## Running the program

`java -jar ec2-snapshotter.jar [AWS-Key AWS-Secret]`

The AWS key and secret are optional and when omitted, the program will attempt to use the 
EC2 instance profile's credentials. Make sure you have the appropriate permissions on your 
IAM role to allow listing volumes, creating and deleting snapshots, and tagging resources.

## Scheduling the Backup

On Linux, you can use `cron` to perform scheduled backups. The following crontab entry will 
run every night at midnight server local time.

`0 0 * * * java -jar /path/to/program/ec2-snapshotter.jar`

## License

The EC2 Snapshotter is licensed under the Apache 2.0 License.