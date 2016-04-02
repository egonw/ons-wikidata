paperQ = null // 
paperQ = "Q22570477"
smiles = "CC1=CC(=NC=C1)C"

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

mol = cdk.fromSMILES(smiles)
ui.open(mol)

inchiObj = inchi.generate(mol)
inchiShort = inchiObj.value.substring(6)
key = inchiObj.key // key = "GDGXJFJBRMKYDL-FYWRMAATSA-N"

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?compound WHERE {
  ?compound wdt:P235 "$key" .
}
"""

if (bioclipse.isOnline()) {
  results = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
  missing = results.rowCount == 0
} else {
  missing = true
}

formula = upgradeChemFormula(cdk.molecularFormula(mol))

// Create the Wikidata QuickStatement, see https://tools.wmflabs.org/wikidata-todo/quick_statements.php

item = "LAST" // set to Qxxxx if you need to append info, e.g. item = "Q22579236"

pubchemLine = ""
if (bioclipse.isOnline()) {
  pcResults = pubchem.search(key)
  if (pcResults.size == 1) {
    cid = pcResults[0]
    pubchemLine = "$item\tP662\t\"$cid\""
  }
}

paperProv = ""
if (paperQ != null) paperProv = "\tS248\t$paperQ"

if (!missing) {
  println "===================="
  println "$formula is already in Wikidata as " + results.get(1,"compound")
  println "===================="
} else {
  statement = """
    CREATE
    
    $item\tP31\tQ11173$paperProv
    $item\tDen\t\"chemical compound\"$paperProv
    $item\tP233\t\"$smiles\"
    $item\tP274\t\"$formula\"
    $item\tP234\t\"$inchiShort\"
    $item\tP235\t\"$key\"
    $pubchemLine
  """

  println "===================="
  println statement
  println "===================="
}
