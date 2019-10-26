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
SELECT ?compound ?smiles ?inchikey WHERE {
  ?compound wdt:P233 ?smiles ;
            wdt:P235 ?inchikey .
}
"""
canMappings = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?compound ?smiles ?inchikey WHERE {
  ?compound wdt:P2017 ?smiles ;
            wdt:P235 ?inchikey .
}
"""
isoMappings = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

outFilename = "/Wikidata/mismatchInChIKey.txt"
unitFilename = "/Wikidata/mismatchInChIKey.xml"

// Test for InChIKey mismatches

// Test 1: the first block should be identical when calculated from the canonical SMILES
fileContent = ""
unitContent = ""
unitContent = "<testsuite tests=\"2\">\n"
unitContent += "  <testcase classname=\"InChITests\" name=\"ConnectivityMismatch\">\n"
for (i=1; i<=canMappings.rowCount; i++) {
  try {
    wdID = canMappings.get(i, "compound")
    smiles = canMappings.get(i, "smiles")
    inchikey = canMappings.get(i, "inchikey")
    mol = cdk.fromSMILES(smiles)
    inchiObj = inchi.generate(mol)
    calculatedKey = inchiObj.key
    if (!calculatedKey.substring(0,14).equals(inchikey.substring(0,14))) {
      fileContent += wdID + " with canonical SMILES '${smiles}' has a calculated InChIKey ${calculatedKey} that does not match the given ${inchikey}\n"
    }
  } catch (InvalidSmilesException exception) {
    // ignore bad SMILES; there is a separate test for that
  }
}
if (fileContent.length() > 0) {
  unitContent += "<error message=\"Connectivity of the canonical SMILES and InChIKey does not match\" " +
    "type=\"io.github.egonw.wikidata.ons.chemistry\">\n" +
    fileContent + "\n</error>\n"
}
ui.append(outFilename, fileContent); fileContent = ""
ui.append(unitFilename, unitContent); unitContent = ""
unitContent += "  </testcase>\n"

// Test 1: the first block should be identical when calculated from the canonical SMILES
fileContent = ""
unitContent = ""
unitContent = "<testsuite tests=\"2\">\n"
unitContent += "  <testcase classname=\"InChITests\" name=\"InChIKeyMismatch\">\n"
for (i=1; i<=isoMappings.rowCount; i++) {
  try {
    wdID = isoMappings.get(i, "compound")
    smiles = isoMappings.get(i, "smiles")
    inchikey = isoMappings.get(i, "inchikey")
    mol = cdk.fromSMILES(smiles)
    inchiObj = inchi.generate(mol)
    calculatedKey = inchiObj.key
    if (!calculatedKey.substring(0,14).equals(inchikey.substring(0,14))) {
      fileContent += wdID + " with isomeric SMILES '${smiles}' has a calculated InChIKey ${calculatedKey} that does not match the given ${inchikey}\n"
    }
  } catch (InvalidSmilesException exception) {
    // ignore bad SMILES; there is a separate test for that
  }
}
if (fileContent.length() > 0) {
  unitContent += "<error message=\"The InChIKey computed from the isomeric SMILES and InChIKey in Wikidata does not match\" " +
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
