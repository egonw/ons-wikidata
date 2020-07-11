@GrabResolver(name='spring', root='https://repo.spring.io/plugins-release/')
@Grab(group='nu.xom', module='com.springsource.nu.xom', version='1.2.5')

@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.11')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.11')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.11')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

identifier = "P233" // SMILES
type = "smiles"

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?compound ?smiles WHERE {
  ?compound wdt:P233 ?smiles .
}
"""
canMappings = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?compound ?smiles WHERE {
  ?compound wdt:P2017 ?smiles .
}
"""
isoMappings = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

outFilename = "/Wikidata/badWikidataSMILES.txt"
unitFilename = "/Wikidata/badWikidataSMILES.xml"

fileContent = ""
unitContent = ""
unitContent = "<testsuite tests=\"2\">\n"
unitContent += "  <testcase classname=\"SMILESTests\" name=\"Parsable\">\n"
for (i=1; i<=canMappings.rowCount; i++) {
  try {
    wdID = canMappings.get(i, "compound")
    smiles = canMappings.get(i, "smiles")
    mol = cdk.fromSMILES(smiles)
  } catch (Exception exception) {
    fileContent += wdID + " , " + smiles + ": " + exception.message + "\n"
  }
}
if (fileContent.length() > 0) {
  unitContent += "<error message=\"Unparsable SMILES Found\" " +
    "type=\"org.openscience.cdk.exception.InvalidSmilesException\">\n" +
    fileContent + "\n</error>\n"
}
ui.append(outFilename, fileContent); fileContent = ""
ui.append(unitFilename, unitContent); unitContent = ""
unitContent += "  </testcase>\n"
unitContent += "  <testcase classname=\"IsomericSMILESTests\" name=\"Parsable\">\n"
for (i=1; i<=isoMappings.rowCount; i++) {
  try {
    wdID = isoMappings.get(i, "compound")
    smiles = isoMappings.get(i, "smiles")
    mol = cdk.fromSMILES(smiles)
  } catch (Exception exception) {
    fileContent += wdID + " , " + smiles + ": " + exception.message + "\n"
  }
}
if (fileContent.length() > 0) {
  unitContent += "<error message=\"Unparsable SMILES Found\" " +
    "type=\"org.openscience.cdk.exception.InvalidSmilesException\">\n" +
    fileContent + "\n</error>\n"
}
unitContent += "  </testcase>\n"
unitContent += "</testsuite>\n"
ui.append(outFilename, fileContent)
ui.append(unitFilename, unitContent)
// ui.open(outFilename)
