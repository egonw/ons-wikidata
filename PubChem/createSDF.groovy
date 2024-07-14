// Copyright (C) 2019-2024  Egon Willighagen
// License: MIT

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.0-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.0-SNAPSHOT')
@Grab(group='org.openscience.cdk', module='cdk-smiles', version='2.10-SNAPSHOT')
@Grab(group='org.openscience.cdk', module='cdk-silent', version='2.10-SNAPSHOT')
@Grab(group='org.openscience.cdk', module='cdk-ctab', version='2.10-SNAPSHOT')
@Grab(group='org.openscience.cdk', module='cdk-sdg', version='2.10-SNAPSHOT')

import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.io.*;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import javax.vecmath.Vector2d

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);

builder = SilentChemObjectBuilder.getInstance()
sp = new SmilesParser(builder)

// do it in a few runs, so that errors from PubChem are easier to handle
regIDs = new HashSet<String>();

//batches   = 5
//batchSize = 300000
batches   = 10
batchSize = 150000


def allowableBonds(IAtomContainer container) {
  allowable = true
  for (bond : container.bonds()) {
    if (bond.order == IBond.Order.QUADRUPLE) allowable = false
  }
  return allowable
}

//qLeverHeaders = new HashMap();
//qLeverHeaders.put("Accept", "application/sparql-results+xml");

1.upto(batches) { batchCounter ->
  offset = (batchCounter - 1) * batchSize
  molList = builder.newInstance(IAtomContainerSet.class)

  // the following query gets all chemical with a canonical or isomeric SMILES
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT DISTINCT ?chemical ?smiles WHERE {
    ?chemical wdt:P233 | wdt:P2017 [] .
    OPTIONAL { ?chemical wdt:P233 ?canonical }
    OPTIONAL { ?chemical wdt:P2017 ?isomeric }
    BIND (COALESCE(?isomeric, ?canonical) AS ?smiles)
  } LIMIT ${batchSize} OFFSET ${offset}
  """

  error1 = "/PubChem/wikidata_${batchCounter}_fixes1.qs"
  ui.renewFile(error1)

  if (bioclipse.isOnline()) {
    try {
      rawResults = bioclipse.sparqlRemote(
        // "https://query.wikidata.org/sparql", sparql
        // "https://beta.sparql.swisslipids.org/sparql?format=xml", sparql
        "https://qlever.cs.uni-freiburg.de/api/wikidata", sparql
      )
      results = rdf.processSPARQLXML(rawResults, sparql)
      1.upto(results.rowCount) { chemCounter ->
        wdid   = results.get(chemCounter, "chemical")
        smiles = results.get(chemCounter, "smiles")
        qid = wdid.substring(31)
        if (regIDs.contains(qid)) { // skip
        } else if (smiles.contains(" ")) {
          // ignore entries with spaces in the SMILES
          println("${qid}: SMILES with space: ${smiles}")
        } else if (smiles.contains("[R1]")) {
          // ignore entries with R1 in the SMILES
          println("${qid}: SMILES with R group: ${smiles}")
        } else if (smiles.startsWith("http://www.wikidata.org/.well-known/genid/")) {
          // ignore entries with R1 in the SMILES
          println("${qid}: SMILES is 'unknown'")
        } else {
          regIDs.add(qid)
          try {
            if (smiles.contains("\\\\")) {
              println("${qid}: SMILES with double backslash: ${smiles}")
              ui.append(error1, "-${qid}\tP2017\t\"${smiles}\"\n")
              smiles = smiles.replace("\\\\","\\")
              ui.append(error1, "${qid}\tP2017\t\"${smiles}\"\n")
            }
            mol = sp.parseSmiles(smiles)
            sdg = new StructureDiagramGenerator();
            sdg.setMolecule(mol);
            sdg.generateCoordinates(new Vector2d(0, 1));
            mol = sdg.getMolecule();
            mol.setProperty("PUBCHEM_EXT_DATASOURCE_SMILES", smiles)
            mol.setProperty("PUBCHEM_EXT_DATASOURCE_REGID", qid)
            mol.setProperty("PUBCHEM_EXT_SUBSTANCE_URL", "https://scholia.toolforge.org/" + qid)
            
            if (allowableBonds(mol)) {
              molList.addAtomContainer(mol)
            } else {
              println("${qid}: cannot write bond order to SDF")
            }
          } catch (Exception exception) {
            println("${qid}: ${exception.message}")
          }
        }
      }
    } catch (Exception exception) {
      println exception.message
    }
    writer = new FileWriter(new File("wikidata_${batchCounter}.sdf"))
    SDFWriter sdfWriter = new SDFWriter(writer);
    sdfWriter.write(molList);
    sdfWriter.close();
    writer.close();
  } // end is online
} // and batch loop
