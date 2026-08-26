import classes.WikiDataProperty
import classes.ChemicalFormula
import classes.WikiDataConnection
import classes.Compound


// TESTING WikiData Property
// WDP = new WikiDataProperty()
// println WDP.canonicalSMILES


// TESTING Chemical Formula Class
// CF = new ChemicalFormula('0-1-2-3-4-5-6-7-8-9')
// println CF.modifyChemicalFormula(CF.formula, CF.subscript_map)


// TESTING Compound Class Functionality
// String workspaceRoot = '..'
// String inchikey = 'UHOVQNZJYSORNB-UHFFFAOYSA-N'
// cmpnd = new Compound(workspaceRoot, inchikey)
// cmpnd.updateIsomeresFromInChI()
// cmpnd.displayDetails()
// println cmpnd.bactingDetails()


// TESTING WikiData Connection Class
String workspaceRoot = '..'
WKC = new WikiDataConnection(workspaceRoot)

query = 'PREFIX wdt: <http://www.wikidata.org/prop/direct/> SELECT DISTINCT ?key WHERE { ?compound wdt:P235 ?key . } LIMIT 5'
results = WKC.queryEndPoint(query)

println results
