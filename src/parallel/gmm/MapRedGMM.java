/*
 * Implementation of the EM algorithm for training GMM on a Hadoop cluster. The input text files 
 * should contain one row vector per line and the number of columns must be equal to DIM in
 * Config.java. This version works with hadoop-2.6.0
 * 
 * Example usuage:
 * 	 $HADOOP_PREFIX/bin/hadoop jar ../lib/parallel.jar parallel.gmm.MapRedGMM /user/mwmak/stats/input /user/mwmak/stats/output;
 * 
 * Author: Man-Wai MAK, Dept. of EIE, The Hong Kong Polytechnic University
 * Version: 1.0
 * Date: March 2015
 * 
 * This file is subject to the terms and conditions defined in
 * file 'license.txt', which is part of this source code package.
*/

package parallel.gmm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

//import sequential.gmm.GMM;


public class MapRedGMM {

	private final static int NUM_MIX = Config.NUM_MIX;
	private final static int DIM = Config.DIM;
	private final static String GMM_FILE = Config.GMM_FILE;
	
	/*
	 * Create GMM object and load the parameters estimated in the previous iteration. 
	 * It is important to load the parameters here instead of the main() method because
	 * Hadoop may load the GMMMapper class before loading the MapRedGMM class.
	 */
	
	///////////////
	private static ArrayList<GMM> GMMs = initialArrayGMMs();
	
	//initial GMM arraylist//////////////
	public static ArrayList<GMM> initialArrayGMMs() 
	{
		ArrayList<GMM> GMMs = new ArrayList<GMM>();
		//load GMM parameters in last iteration of 10 GMMs 
		for (int i=0; i<10; i++)
		{
			GMM gmm = new GMM(DIM, NUM_MIX, i, GMM_FILE);
			GMMs.add(gmm);
		}
		return GMMs;
	}
	
	//private static GMM gmm = new GMM(DIM, NUM_MIX, GMM_FILE);
	/*
	 * Emit one <label,SuffStats object> for each line of input files.
	 */
	public static class GMMMapper extends
			Mapper<LongWritable, Text, IntWritable, SuffStats> {
		

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			//load training data one line by line
			String[] token = value.toString().split("\\s+|,");
			int[] xt = new int[DIM];
			for (int i = 0; i < DIM; i++) {
				xt[i] = Integer.parseInt(token[i]);
			}
			//get the label of this training data sample
			int label= Integer.parseInt(token[784]);
			//get corresponding GMM 
			GMM gmm = GMMs.get(label);
			
			SuffStats suffStats = new SuffStats();
			double[] gamma = gmm.getPosterior(xt);
			suffStats.accumulate(gamma, xt, gmm.getMeans());
			suffStats.setLikelh(gmm.getLogLikelihood(xt));
			
			//set label as the key
			IntWritable keyOut = new IntWritable(label);
			context.write(keyOut, suffStats);
		}
	}

	/*
	 * For each mapper, emit <key,SuffStat object> pair, where the SuffStat
	 * object contains the partial sum of the sufficient statistics of this
	 * mapper and the key is the same as the key received by this combiner.
	 */
	public static class GMMCombiner extends
			Reducer<IntWritable, SuffStats, IntWritable, SuffStats> {

		public void reduce(IntWritable key, Iterable<SuffStats> values,
				Context context) throws IOException, InterruptedException {
			Iterator<SuffStats> iter = values.iterator();
			SuffStats suffStats = new SuffStats();
			while (iter.hasNext()) {
				SuffStats thisSuffStats = iter.next();
				suffStats.accumulate(thisSuffStats);
			}
			context.write(key, suffStats);
		}
	}

	/*
	 * For each key-value pair from the combiner, sum the partial sufficient
	 * stats and update GMM parameters. Note that there is one Reducer only
	 */
	public static class GMMReducer extends
			Reducer<IntWritable, SuffStats, IntWritable, Text> {

		public void reduce(IntWritable key, Iterable<SuffStats> values,
				Context context) throws IOException, InterruptedException {

			Iterator<SuffStats> iter = values.iterator();
			SuffStats suffStats = new SuffStats();
			while (iter.hasNext()) {
				SuffStats thisSuffStats = iter.next();
				suffStats.accumulate(thisSuffStats);
			}
			
			GMM gmm = GMMs.get(Integer.parseInt(key.toString()));
			
			gmm.maximize(suffStats);
			System.out.println(gmm.toString());		// Export to stdout files in logs/ folder for debugging
			//save parameters of GMM in this iteration to corresponding file
			gmm.saveParameters(GMM_FILE + key.toString() + ".txt");
			
			Text valueOut = new Text();
			valueOut.set(gmm.toString() + "\nLogLikelihood=" + suffStats.getLikelh());
			context.write(key, valueOut);
		}
	}

	/*
	 * Load GMM parameters at startup so that each run starts with the GMM parameters of the previous iteration
	 */
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "gmm");
		job.setJarByClass(MapRedGMM.class);

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(SuffStats.class);

		job.setMapperClass(GMMMapper.class);
		job.setCombinerClass(GMMCombiner.class);
		job.setReducerClass(GMMReducer.class);
		job.setNumReduceTasks(1);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.waitForCompletion(true);
	}

}
