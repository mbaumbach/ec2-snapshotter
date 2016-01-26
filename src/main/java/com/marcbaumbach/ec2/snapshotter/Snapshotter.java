package com.marcbaumbach.ec2.snapshotter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;

public class Snapshotter {
	
	private static final Logger logger = LoggerFactory.getLogger(Snapshotter.class);
	
	private static final String SNAPSHOTTER_TAG_KEY = "SnapshotterBackup";
	private static final String FILTER_TAG_BACKUP = "tag:Backup";
	private static final String FILTER_TAG_SNAPSHOTTER = "tag:" + SNAPSHOTTER_TAG_KEY;
	private static final String TRUE_VALUE = "True";
	private static final String TAG_NAME = "Name";
	private static final int NUMBER_OF_MONTHS = 1;
	
	private AmazonEC2 ec2Client;
	
	public Snapshotter(AWSCredentialsProvider credentialsProvider) {
		ec2Client = new AmazonEC2Client(credentialsProvider);
	}
	
	private CreateSnapshotRequest createSnapshotRequest(Volume volume) {
		String description = volume.getTags().stream()
				.filter(t -> t.getKey().equals(TAG_NAME))
				.findFirst()
				.map(t -> t.getValue())
				.orElse(volume.getVolumeId());
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		description += " - " + format.format(new Date());
		logger.info("Creating snapshot {} for volume {}", description, volume.getVolumeId());
		return new CreateSnapshotRequest(volume.getVolumeId(), description);
	}
	
	private DeleteSnapshotRequest deleteSnapshotRequest(Snapshot snapshot) {
		logger.info("Deleting snapshot {} for volume {}", snapshot.getDescription(), snapshot.getVolumeId());
		return new DeleteSnapshotRequest(snapshot.getSnapshotId());
	}
	
	private CreateTagsRequest createTagsRequest(String snapshotId) {
		logger.info("Creating tag for snapshot {}", snapshotId);
		return new CreateTagsRequest()
				.withResources(snapshotId)
				.withTags(new Tag(SNAPSHOTTER_TAG_KEY, TRUE_VALUE));
	}
	
	public Snapshotter createSnapshots() {
		long start = System.nanoTime();
		logger.info("Creating snapshots...");
		DescribeVolumesRequest describeVolumes = new DescribeVolumesRequest()
				.withFilters(new Filter(FILTER_TAG_BACKUP, Arrays.asList(TRUE_VALUE)));
		DescribeVolumesResult volumesResult = ec2Client.describeVolumes(describeVolumes);
		volumesResult.getVolumes().stream()
			.map(this::createSnapshotRequest)
			.map(ec2Client::createSnapshot)
			.map(csr -> csr.getSnapshot().getSnapshotId())
			.map(this::createTagsRequest)
			.forEach(ec2Client::createTags);
		logger.info("Completed snapshots in {}s", (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)));
		return this;
	}
	
	public Snapshotter cleanupSnapshots() {
		long start = System.nanoTime();
		logger.info("Cleaning up snapshots older than {} month(s)...", NUMBER_OF_MONTHS);
		DescribeSnapshotsRequest describeSnapshots = new DescribeSnapshotsRequest()
				.withFilters(new Filter(FILTER_TAG_SNAPSHOTTER, Arrays.asList(TRUE_VALUE)));
		DescribeSnapshotsResult snapshotsResult = ec2Client.describeSnapshots(describeSnapshots);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MONTH, -NUMBER_OF_MONTHS);
		Date cutoff = calendar.getTime();
		snapshotsResult.getSnapshots().stream()
			.filter(s -> s.getStartTime().before(cutoff))
			.map(this::deleteSnapshotRequest)
			.forEach(ec2Client::deleteSnapshot);
		logger.info("Completed snapshot cleanup in {}s", (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)));
		return this;
	}
	
	private static AWSCredentialsProvider getCredentials(String[] args) {
		AWSCredentialsProvider provider = null;
		if (args.length == 2) {
			provider = new StaticCredentialsProvider(new BasicAWSCredentials(args[0], args[1]));
		} else {
			logger.info("No credentials found in arguments, attempting to detect credentials");
			provider = new DefaultAWSCredentialsProviderChain();
		}
		return provider;
	}
	
	public static void main(String[] args) {
		new Snapshotter(getCredentials(args)).createSnapshots().cleanupSnapshots();
	}

}