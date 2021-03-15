import WikiDataProperty
import ChemicalFormula
import SparQL

WDP = new WikiDataProperty()
println WDP.canonicalSMILES


CF = new ChemicalFormula('0-1-2-3-4-5-6-7-8-9')
println CF.modifyChemicalFormula(CF.formula, CF.subscript_map)

query = """
    PREFIX wdt: <http://www.wikidata.org/prop/direct/>
    SELECT ?compound ?formula ?key ?inchi ?smiles ?pubchem WHERE {
      VALUES ?compound { <http://www.wikidata.org/entity/Q4835624> }
      OPTIONAL { ?compound wdt:${WDP.canonicalSMILES} ?smiles }
      OPTIONAL { ?compound wdt:P274 ?formula }
      OPTIONAL { ?compound wdt:P235 ?key }
      OPTIONAL { ?compound wdt:P234 ?inchi }
      OPTIONAL { ?compound wdt:P662 ?pubchem }
    }
  """
workspaceRoot = '..'
spql = new SparQL(workspaceRoot, query)
println spql.queryEndPoint()