// Copyright (C) 2019-2022  Egon Willighagen
// License: MIT

//   Takes a list of InChIs and fetches SMILES for them from PubChem.

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.1.2')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);

inchiFile = "/Wikidata/wp_inchikeys.txt"
idIndex = null
inchikeyIndex = 0

pubchemTimeout = 400

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT DISTINCT ?key WHERE {
  ?compound wdt:P235 ?key .
}
"""

if (bioclipse.isOnline()) {
  results = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
}
// make a map
map = new HashSet()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  map.add(rowVals[0])  
}

new File(bioclipse.fullPath(inchiFile)).eachLine { line ->
  inchikey = null; extid = null; name = ""
  if (line.contains("\t")) {
    fields = line.trim().split("\t")
    inchikey = fields[inchikeyIndex]
    // if (fields.length > 1)    extid = fields[1]
    if (fields.length == 2) name = fields[1]
  } else {
    inchikey = line.trim()
  }
  
  if (map.contains(inchikey)) {
    println "# $inchikey is already in Wikidata"
    return
  }

  sleep(pubchemTimeout) // keep PubChem happy
  try {
    url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${inchikey}/property/IsomericSMILES/TXT"
    smiles = bioclipse.download(url)
    String[] smiLines = smiles.split("\r\n|\r|\n");
    if (smiLines.length > 1) {
      println "# Multiple SMILES returned for $inchikey"
      return
    }
    smiles = smiles.trim()
    mol = cdk.fromSMILES(smiles)

    atomsWithUndefinedStereo = cdk.getAtomsWithUndefinedStereo(mol)
    if (atomsWithUndefinedStereo.size() > 0) {
      println "# Molecule has ${atomsWithUndefinedStereo.size()} undefined stereo atoms: ${smiles}"
      // return
      // okay for now
    } else {
      // println "# Molecule has no undefined stereo atoms"
    }

    inchiObj = inchi.generate(mol)
    inchiShort = inchiObj.value.substring(6)
    key = inchiObj.key
    if (key != inchikey) { println "# Key mismatch: started with $inchikey but SMILES translates to $key"; return }
    output = smiles.trim()
    if (extid != null) {
      output += "\t" + extid
    }
    if (name != null && name.length() != 0) {
      output += "\t" + name
    } else {
      try {
        sleep(pubchemTimeout) // keep PubChem happy
        names = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey/${inchikey}/property/IUPACName/TXT")
        String[] nameLines = names.split("\r\n|\r|\n");
        name = nameLines[0]
        if (!names.contains("Status: 404")) output += "\t" + name
      } catch (Exception e) { e.printStackTrace() }
    }
    println output
  } catch (Exception e) { println "# ${inchikey} exception: " + e.message }
}

