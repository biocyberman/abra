package abra;

import java.util.HashMap;
import java.util.Map;

import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SAMRecord;

public class SVReadCounter {
	
	private Map<String, Integer> breakpointCounts = new HashMap<String, Integer>();
	
	private static final int MAX_EDIT_DISTANCE = 5;
	
	private SAMLineParser parser;
	
	private Map<String, Integer> counts;

	public Map<String, Integer> countReadsSupportingBreakpoints(SamReader reader, int readLength, SAMFileHeader samHeader) {
		
		parser = new SAMLineParser(new DefaultSAMRecordFactory(),
                ValidationStringency.SILENT, samHeader,
                null, null);
		
		String fullMatch = readLength + "M";
		
		// Require 90% of the read to overlap the breakpoint
		int minStart = (int) (readLength * .10);
		int maxStart = (int) (readLength *.9) + 1;
		
		// TODO: Need way to query mapped reads only
		for (SAMRecord read : reader) {
			if (!read.getReadUnmappedFlag() && read.getCigarString().equals(fullMatch)) {
				if (read.getAlignmentStart() >= minStart && read.getAlignmentStart() <= maxStart) {
					int editDistance = SAMRecordUtils.getIntAttribute(read, "NM");
					
					if (editDistance <= MAX_EDIT_DISTANCE) {
						SAMRecord orig = getOrigRecord(read, samHeader);
						int origEditDistance = SAMRecordUtils.getIntAttribute(orig, "YX");
						if (editDistance < origEditDistance) {
							//TODO: Inspect alternate alignments
							String[] refFields = read.getReferenceName().split("_");
							if (refFields.length >= 6) {
								String breakpointGroupId = refFields[0] + "_" + refFields[1] + "\t" + refFields[2] + ":" + refFields[3] + "\t" +
										refFields[4] + ":" + refFields[5];
								Integer count = breakpointCounts.get(breakpointGroupId);
								if (count == null) {
									breakpointCounts.put(breakpointGroupId, 1);
								} else {
									breakpointCounts.put(breakpointGroupId, count + 1);
								}
							} else {
								System.out.println("Error analyzing breakpoint for: " + read.getSAMString());
							}
						}
					}
				}
			}
		}
		
		this.counts = breakpointCounts;
		
		return breakpointCounts;
	}
	
	private SAMRecord getOrigRecord(SAMRecord read, SAMFileHeader samHeader) {
		String origSamStr = read.getReadName();
		origSamStr = origSamStr.replace(Sam2Fastq.FIELD_DELIMITER, "\t");
		SAMRecord orig;
		try {
			orig = parser.parseLine(origSamStr);
		} catch (RuntimeException e) {
			System.out.println("Error processing: [" + origSamStr + "]");
			System.out.println("Contig read: [" + read.getSAMString() + "]");
			e.printStackTrace();
			throw e;
		}
		orig.setHeader(samHeader);

		return orig;
	}
	
	public Map<String, Integer> getCounts() {
		return counts;
	}
}
