@Grab('com.xlson.groovycsv:groovycsv:1.1')
@Grab('org.openscience.cdk:cdk-ctab:2.3')
@Grab('org.openscience.cdk:cdk-silent:2.3') 

import static com.xlson.groovycsv.CsvParser.parseCsv
import org.openscience.cdk.io.iterator.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;

def duplicates = new Hashtable<String,String>();
def issues     = new Hashtable<String,String>();

// N,tid,Message,Record,Line,Phase
for (line in parseCsv(new FileReader('pc-report-102411_duplicates.csv'))) {
  duplicates.put(line.Record, line.Message)
}

// N,tid,Message,Record,Line,Phase
for (line in parseCsv(new FileReader('pc-report-102411_standardizationIssues.csv'))) {
  issues.put(line.Record, line.Message)
}

print """
{| class="wikitable"
|-
! Wikidata !! Scholia !! Error Message !! SMILES
"""

record = 0
iterator = new IteratingSDFReader(
  new File("wikidata.csv.sdf").newReader(),
  SilentChemObjectBuilder.getInstance()
)
while (iterator.hasNext()) {
  IAtomContainer mol = iterator.next()
  record++

  key = "" + record
  qid = mol.getProperty("PUBCHEM_EXT_DATASOURCE_REGID")
  smi = mol.getProperty("PUBCHEM_EXT_DATASOURCE_SMILES")
  qidTempl = "{{Q|${qid.substring(1)}}}"

  if (duplicates.containsKey(key)) {
    println """|-
| ${qidTempl} || [https://tools.wmflabs.org/scholia/chemical/$qid $qid] || ${duplicates.get(key)} || ${smi}"""
  }
  if (issues.containsKey(key)) {
    println """|-
| ${qidTempl} || [https://tools.wmflabs.org/scholia/chemical/$qid $qid] || ${issues.get(key)} || ${smi}"""
  }
}

print """|}
"""
