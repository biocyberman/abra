/* Copyright 2013 University of North Carolina at Chapel Hill.  All rights reserved. */
package abra;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import htsjdk.samtools.SAMRecord;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CombineChimeraTest {
	
	private CombineChimera3 cc;
	
	@BeforeMethod()
	private void setUp() {
		cc = new CombineChimera3();
		cc.minIndelBuffer = 33;
	}

	@Test (groups = "unit")
	public void testInsert() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(6162064, "129S120M", 249));
		reads.add(newRecord(6161985, "79M170S", 249));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 6161985);
		assertEquals(combined.getCigarString(), "79M50I120M");
	}
	
	@Test (groups = "unit")
	public void testOverlappingInsert() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(6161985, "129M120S", 249));
		reads.add(newRecord(6162064, "129S120M", 249));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 6161985);
		assertEquals(combined.getCigarString(), "79M50I120M");
	}
	
	@Test (groups = "unit")
	public void testDelete() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(16085602, "60S65M", 125));
		reads.add(newRecord(16085517, "60M65S", 125));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 16085517);
		assertEquals(combined.getCigarString(), "60M25D65M");
		assertEquals(combined.getReadLength(), 125);
	}
	
	@Test (groups = "unit")
	public void testOverlappingDelete() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(16085605, "49S62M", 111));
		reads.add(newRecord(16085517, "53M58S", 111));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 16085517);
		assertEquals(combined.getCigarString(), "49M39D62M");
		assertEquals(combined.getReadLength(), 111);
	}
	
	@Test (groups = "unit")
	public void testInsertRegionAligned() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(16085517, "200M300S", 500));
		reads.add(newRecord(16085717, "300S200M", 500));
		reads.add(newRecord(33141553, "199S102M199S", 500));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 16085517);
		assertEquals(combined.getCigarString(), "200M100I200M");
	}
	
	@Test (groups = "unit")
	public void testInsertWithPaddingOnRightDueToSNP() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(30726101, "90M94S", 184));
		reads.add(newRecord(30826191, "90S51M43S", 184));
		reads.add(newRecord(30726192, "141S43M", 184));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 30726101);
		assertEquals(combined.getCigarString(), "90M50I44M");
	}
	
	@Test (groups = "unit")
	public void testDistantReadsNotModified() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		reads.add(newRecord(11032656, "63M31S", 94));
		reads.add(newRecord(214990735, "34S60M", 94));
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 2);
		SAMRecord read1 = outputList.get(0);
		SAMRecord read2 = outputList.get(1);
		assertEquals(read1.getAlignmentStart(), 11032656);
		assertEquals(read2.getAlignmentStart(), 214990735);
	}
	
	@Test (groups = "unit")
	public void testHardClipping() {
		List<SAMRecord> reads = new ArrayList<SAMRecord>();
		SAMRecord read1 = newRecord(6169757, "217M75S", 292);
		SAMRecord read2 = newRecord(6170072, "215H77M", 77);
//		reads.add(newRecord(6169757, "217M75S", 292));
//		reads.add(newRecord(6170072, "215H77M", 77));
		
		reads.add(read1);
		reads.add(read2);
		
		List<SAMRecord> outputList = cc.processRead(reads);
		assertEquals(outputList.size(), 1);
		SAMRecord combined = outputList.get(0);
		assertEquals(combined.getAlignmentStart(), 6169757);
		assertEquals(combined.getCigarString(), "215M100D77M");
		assertEquals(read1.getReadLength(), 292);
		assertEquals(read2.getReadLength(), 292);
	}
	
	private SAMRecord newRecord(int pos, String cigar, int length) {
		SAMRecord record = new SAMRecord(null);
		record.setReferenceName("chr21");
		record.setAlignmentStart(pos);
		record.setCigarString(cigar);
		record.setReadNegativeStrandFlag(false);
		char[] bases = new char[length];
		Arrays.fill(bases, 'A');
		record.setReadString(String.valueOf(bases));
		return record;
	}
}
