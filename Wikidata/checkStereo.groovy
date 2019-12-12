@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.11')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.11')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.11')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.11')

import org.openscience.cdk.exception.InvalidSmilesException;

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT DISTINCT ?compound ?smiles ?isosmiles WHERE {
  ?compound wdt:P233 | wdt:P2017 [] .
  OPTIONAL { ?compound wdt:P233 ?smiles }
  OPTIONAL { ?compound wdt:P2017 ?isosmiles }
}
"""
mappings = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

outFilename = "/Wikidata/checkStereo.txt"
unitFilename = "/Wikidata/checkStereo.xml"
ui.renewFile(outFilename)
ui.renewFile(unitFilename)

// Test: the first block should be identical when calculated from the canonical SMILES
fileContent = ""
unitContent = ""
unitContent = "<testsuite tests=\"1\">\n"
unitContent += "  <testcase classname=\"checkStereo\" name=\"missingStereo\">\n"
for (i=1; i<=mappings.rowCount; i++) {
  try {
    wdID = mappings.get(i, "compound")
    smiles = mappings.get(i, "smiles")
    isosmiles = mappings.get(i, "isosmiles")
    if (isosmiles != null) smiles = isosmiles
    mol = cdk.fromSMILES(smiles)

    undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol)
    fullChiralityIsDefined = undefinedCenters.size() == 0

    if (!fullChiralityIsDefined) {
      fileContent += wdID + " with SMILES '${smiles}' has missing stereochemistry for ${undefinedCenters.size()} center(s)\n"
    }
  } catch (Exception exception) {
    // ignore bad SMILES; there is a separate test for that
  }
}
if (fileContent.length() > 0) {
  unitContent += "<error message=\"There \" " +
    "type=\"io.github.egonw.wikidata.ons.chemistry\">\n" +
    fileContent + "\n</error>\n"
}
ui.append(outFilename, fileContent); fileContent = ""
ui.append(unitFilename, unitContent); unitContent = ""
unitContent += "  </testcase>\n"
unitContent += "</testsuite>\n"

ui.append(outFilename, fileContent)
ui.append(unitFilename, unitContent)
// ui.open(outFilename)
