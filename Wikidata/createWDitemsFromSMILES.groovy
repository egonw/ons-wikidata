// Copyright (C) 2016-2022  Egon Willighagen
// License: MIT

// Usage:
//
//   change the values of the following compoundClassQ and idProperty variables. The first is
//   is an (optional; null by default) for the superclass, and the second is the property of
//   the identifiers of the chemical to check.
//
//   The input file is a TAB separated file with two columns, a isomeric SMILES in the first,
//   and an optional identifiers matching the given property in the second.
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     http://quickstatements.toolforge.org/
//
// Changelog:
//
// 2018-12-02 Added a changelog
// 2018-12-01 Added a feature to set a superclass

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.42')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.42')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.42')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.42')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.42')

import groovy.cli.commons.CliBuilder

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);

smiFile = "/Wikidata/cas.smi"; 

def cli = new CliBuilder(usage: 'createWDitemsFromSMILES.groovy')
cli.h(longOpt: 'help', 'print this message')
cli.s(longOpt: 'full-chirality', 'Only output statements for compounds with full stereochemistry defined')
cli.n(longOpt: 'non-existing-only', 'Only output non-existing chemicals')
cli.i(longOpt: 'identifier', args:1, argName:'identifier', 'Name of the database for which the identifiers are given')
cli.c(longOpt: 'compound-class', args:1, argName:'comp', 'QID of the class of which the compound is an instance')
cli.p(longOpt: 'paper', args:1, argName:'paper', 'QID of the article that backs up that this compound is a chemical')
cli.f(longOpt: 'input-file', args:1, argName:'filename', 'Name of the file containing the SMILES and optionally identifiers and names')
cli.o(longOpt: 'output-file', args:1, argName:'output', 'Name of the file where the quickstatements are stored')
cli.l(longOpt: 'with-labels', 'Take the field after the SMILES as the label of the compound')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
  System.exit(0)
}

if (options.f) {
  smiFile = options.f
}

outputLabel = false
if (options.l) {
  outputLabel = true
}

compoundClassQ = null
if (options.c) {
  compoundClassQ = options.c
}

idProperty = null
if (options.identifier) {
  if (outputLabel) {
    System.out("Cannot take both a label and an indentifier from the input")
    System.exit(-1)
  }
  switch (options.identifier.toLowerCase()) {
    case "hmdb": idProperty = "P2057"; break
    case "comptox": idProperty = "P3117"; break
    case "lipidmaps": idProperty = "P2063"; break
    case "cas": idProperty = "P231"; break
    case "chembl": idProperty = "P592"; break
    case "chebi": idProperty = "P683"; break
    case "kegg": idProperty = "P665"; break
    case "knapsack": idProperty = "P2064"; break
    case "chemspider": idProperty = "P661"; break
    case "pubchem": idProperty = "P662"; break
    case "pdb": idProperty = "P3636"; break
    case "nmr": idProperty = "P9405"; break // nmrshiftdb
    default: println "Unknown identifier database: ${options.identifier}"; System.exit(-1)
  }
  if (idProperty != null) println "ID found: ${idProperty}"
}

qsFile = "/Wikidata/output.quickstatements"
if (options.o) {
  qsFile = options.o
}

// if all SMILES come from the same paper, enter the Wikidata item code
// on the next line, e.g. paperQ = "Q22570477". It will be used as reference
// to some of the information 
paperQ = null
if (options.p) {
  paperQ = options.p
}

// a helper function
def upgradeChemFormula(formula) {
  formula = formula.replace("0","₀");
  formula = formula.replace("1","₁");
  formula = formula.replace("2","₂");
  formula = formula.replace("3","₃");
  formula = formula.replace("4","₄");
  formula = formula.replace("5","₅");
  formula = formula.replace("6","₆");
  formula = formula.replace("7","₇");
  formula = formula.replace("8","₈");
  formula = formula.replace("9","₉");
}

// reset the output (SD file + QuickStatements)
ui.renewFile(qsFile)
mols = cdk.createMoleculeList()

new File(bioclipse.fullPath(smiFile)).eachLine { line ->
  if (line.startsWith("#")) return
  sleep(250) // keep PubChem happy
  smiles = ""; id = ""; inchikey = ""; extid = ""; name = ""
  existingQcode = ""; classInfo = ""
  if (line.contains("\t")) {
    fields = line.split("\t")
    if (fields.length == 2) {
      if (outputLabel) {
        (smiles, name) = fields
      } else {
        (smiles, extid) = fields
      }
    }
    // if (fields.length == 3) (inchikey, extid, smiles) = fields
    if (fields.length == 3) (smiles, extid, name) = fields
    if (fields.length == 1) smiles = fields[0]
  } else {
    smiles = line
    id = "LAST"
  }
  compoundQ = null
  
  mol = cdk.fromSMILES(smiles)
  println "Parsed $smiles into $mol"
  
  smilesProp = "P233"
  if (smiles.contains("@") ||
      smiles.contains("/") ||
      smiles.contains("\\")) smilesProp = "P2017"
  
  inchiObj = inchi.generate(mol)
  inchiShort = inchiObj.value.substring(6)
  key = inchiObj.key

  // check for duplicate based on the InChIKey
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound WHERE {
    ?compound wdt:P235 "$key" .
  }
  """
  if (bioclipse.isOnline()) {
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
    missing = results.rowCount == 0
    if (!missing) {
      existingQcode = results.get(1,"compound")
      println "InChIKey match: $existingQcode"
      missing = false
    }
  } else {
    println "no online access"
    missing = true
  }

  // check for duplicate based on the given identifier (with $idProperty)
  extidFound = false
  sparql = """
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?compound WHERE {
    ?compound wdt:$idProperty "$extid" .
  }
  """
  if (bioclipse.isOnline()) {
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
    idMissing = results.rowCount == 0
    if (!idMissing) {
      existingQcode = results.get(1,"compound")
      println "$idProperty match: $existingQcode"
      extidFound = true
    }
  } else {
    println "no online access"
    extidFound = false
  }
  missing = missing && (!extidFound)

  formula = upgradeChemFormula(cdk.molecularFormula(mol))
  
  // Create the Wikidata QuickStatement, see https://tools.wmflabs.org/wikidata-todo/quick_statements.php
  
  item = "LAST" // set to Qxxxx if you need to append info, e.g. item = "Q22579236"
  
  pubchemLine = ""
  if (bioclipse.isOnline()) {
    try {
      pcResults = pubchem.search(key)
      sleep(250) // keep PubChem happy
      if (pcResults.size == 1) {
        cid = pcResults[0]
        pubchemLine = "$item\tP662\t\"$cid\""
  	  sparql = """
  	  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
    	  SELECT ?compound WHERE {
          ?compound wdt:P662 "$cid" .
  	  }
  	  """

        if (bioclipse.isOnline()) {
          rawResults = bioclipse.sparqlRemote(
            "https://query.wikidata.org/sparql", sparql
          )
          results = rdf.processSPARQLXML(rawResults, sparql)
  	    missing = results.rowCount == 0
  	    if (!missing) {
    	      pcExistingQcode = results.get(1,"compound")
    	      println "PubChem CID match: $pcExistingQcode"
    	      if (existingQcode != "") {
    	        if (existingQcode != pcExistingQcode) {
    	          println "Conflicting Qcodes: $existingQcode and $pcExistingQcode"
    	        } // else: OK, the same
    	      } else {
              existingQcode = pcExistingQcode
    	      }
    	    } else {
    	     if (existingQcode != "") {
    	       missing = false // we already found one using the InChIKey
    	     }
    	    }
  	  } else {
          println "no online access"
  	    missing = true
  	  }
      }
    } catch (Exception exception) {
      println "Error while accessing PubChem: ${exception.message}"
    }
  }

  paperProv = ""
  if (paperQ != null) paperProv = "\tS248\t$paperQ"

  undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol)
  fullChiralityIsDefined = undefinedCenters.size() == 0
  ignoreBecauseStereoMissing =  options.s && !fullChiralityIsDefined

  if (!missing && options.'non-existing-only') {
    println "===================="
    println (new String((char)27) + "[31m" + "$formula is already in Wikidata as " + existingQcode + new String((char)27) + "[37m")
    if (fullChiralityIsDefined) {
      println "Full stereochemistry is defined"
    } else {
      println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    }
  } else if (!missing && ignoreBecauseStereoMissing) {
    println "===================="
    println (new String((char)27) + "[31m" + "$formula is already in Wikidata as " + existingQcode + new String((char)27) + "[37m")
    println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
  } else if (!missing) {
    println "===================="
    println (new String((char)27) + "[31m" + "$formula is already in Wikidata as " + existingQcode + new String((char)27) + "[37m")
    if (fullChiralityIsDefined) {
      println "Full stereochemistry is defined"
    } else {
      println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    }

    item = existingQcode.substring(32)
    pubchemLine = pubchemLine.replace("LAST", "Q" + existingQcode.substring(32))

    if (compoundClassQ != null) classInfo = "Q$item\tP31\t$compoundClassQ"

    typeInfo = "Q$item\tP31\tQ11173"

    statement = """
      $classInfo$paperProv
      $typeInfo$paperProv\n"""

    // check for missing properties
    sparql = """
      PREFIX wdt: <http://www.wikidata.org/prop/direct/>
      SELECT ?compound ?formula ?key ?inchi ?smiles ?pubchem WHERE {
        VALUES ?compound { <${existingQcode}> }
        OPTIONAL { ?compound wdt:$smilesProp ?smiles }
        OPTIONAL { ?compound wdt:P274 ?formula }
        OPTIONAL { ?compound wdt:P235 ?key }
        OPTIONAL { ?compound wdt:P234 ?inchi }
        OPTIONAL { ?compound wdt:P662 ?pubchem }
      }
    """
    if (bioclipse.isOnline()) {
      rawResults = bioclipse.sparqlRemote(
        "https://query.wikidata.org/sparql", sparql
      )
      results = rdf.processSPARQLXML(rawResults, sparql)
      missing = results.rowCount == 0
      if (!missing) {
        if (results.get(1,"smiles") == null || results.get(1,"smiles").trim().length() == 0) {
           if (smiles.length() <= 400) statement += "      Q$item\t$smilesProp\t\"$smiles\"\n"
        }
        if (results.get(1,"formula") == null || results.get(1,"formula").trim().length() == 0) statement += "      Q$item\tP274\t\"$formula\"\n"
        if (results.get(1,"key") == null || results.get(1,"key").trim().length() == 0)         statement += "      Q$item\tP235\t\"$key\"\n"
        if (results.get(1,"inchi") == null || results.get(1,"inchi").trim().length() == 0) {
           if (inchiShort.length() <= 400) statement += "      Q$item\tP234\t\"InChI=$inchiShort\"\n"
        }
        if (results.get(1,"pubchem") == null || results.get(1,"pubchem").trim().length() == 0)     statement += "      $pubchemLine\n"
      }
    }
  
    if (idProperty != null && idProperty != "" && idProperty != "P662" && !extidFound) {
      statement += "      Q$item\t$idProperty\t\"$extid\"$paperProv\n"
    }

    ui.append(qsFile, statement + "\n")
    
    // Recon stuff
    
    //statement = """
    //  Q$item\tP703\tQ15978631\tS248\tQ28601559"
    //"""
    //ui.append(qsFile, statement + "\n")
    //ui.append(reconFile, "$id Q$item\n")
      
    println "===================="
  } else if (ignoreBecauseStereoMissing) {
    println "===================="
    println (new String((char)27) + "[32m" + "$formula is not yet in Wikidata" + new String((char)27) + "[37m")
    println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    println "===================="
  } else if (!ignoreBecauseStereoMissing) {
    println "===================="
    println (new String((char)27) + "[32m" + "$formula is not yet in Wikidata" + new String((char)27) + "[37m")
    if (fullChiralityIsDefined) {
      println "Full stereochemistry is defined"
    } else {
      println "Compound has missing stereo on # of centers: " + undefinedCenters.size()
    }

    if (item == "LAST") {
      statement = """
      CREATE
      """
    } else statement = ""
   
    if (compoundClassQ != null) statement += "$item\tP31\t$compoundClassQ$paperProv\n"
   
    statement += """
      $item\tP31\tQ11173$paperProv
      $item\tDen\t\"chemical compound\"$paperProv
      $item\t$smilesProp\t\"$smiles\"
      $item\tP274\t\"$formula\"
    """
    if (name.length() > 0) statement += "  $item\tLen\t\"${name}\"\n    "
    if (inchiShort.length() <= 400) statement += "  $item\tP234\t\"InChI=$inchiShort\""
    statement += """
      $item\tP235\t\"$key\"
      $pubchemLine
    """

    if (idProperty != null && idProperty != "") {
      if (idProperty == "P662" && pubchemLine.contains("P662")) {} else
      statement += "  $item\t$idProperty\t\"$extid\"$paperProv"
    }

    ui.append(qsFile, statement + "\n")
    println "===================="
  }

  mols.add(mol)
  return
}

// ui.open(mols)
// ui.open(qsFile)
