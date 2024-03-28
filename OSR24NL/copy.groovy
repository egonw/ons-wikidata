@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.5.2')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.5.2')

import groovy.cli.commons.CliBuilder

def cli = new CliBuilder(usage: 'createWDitemsFromSMILES.groovy')
cli.h(longOpt: 'help', 'print this message')
cli.a(longOpt: 'exactMatch', args:1, argName:'exactMatch', 'Property ID (Px) in the Wikibase to match against Wikidata (only when -w is given)')
cli.w(longOpt: 'wikibase', args:1, argName:'wikibase', 'URL of the Wikibase to use, if different from www.wikidata.org')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
  System.exit(0)
}

if (!options.a || !options.w) {
  // both options are obligatory
  println "ERROR: both options -w and -a must be given"
  System.exit(-1)
}

wikibaseServer = options.w
wikibaseName = "Some Wikibase"
exactMatchProperty = options.a

serverIRIprotocol = "https" // the Wikibases have httpS based IRIs
sparqlEP = "https://${wikibaseServer}/query/sparql"

workspaceRoot = ".."

ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

def populateMappings(Set<String> wikidataIDs, HashMap<String,String> prevMappings) {
  wd2wbMappings = (prevMappings != null) ? prevMappings : new HashMap<String,String>();

  wdProperties = ""
  for (prop : wikidataIDs) {
    if (!wd2wbMappings.keySet().contains(prop)) {
      wdProperties += "wd:$prop "
    }
  }
  if (wdProperties.contains("wd:")) {
    // only look up identifiers if we are still missing items
    mappingQuery = """
PREFIX wd:  <http://www.wikidata.org/entity/>
PREFIX wdt: <${serverIRIprotocol}://${wikibaseServer}/prop/direct/>

SELECT ?wdprop ?prop ?propLabel WHERE {
  VALUES ?wdprop { $wdProperties }
  ?prop wdt:${exactMatchProperty} ?wdprop .
  SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en". }
}
"""
    rawResults = bioclipse.sparqlRemote(sparqlEP, mappingQuery)
    results = rdf.processSPARQLXML(rawResults, mappingQuery)

    // update the wd2wbMappings to match the Wikibase
    for (i=1;i<=results.rowCount;i++) {
      rowVals = results.getRow(i)
      wdItem = rowVals[0].substring(31)
      localItem = rowVals[1].substring(16 + wikibaseServer.length())
      wd2wbMappings.put(wdItem, localItem)
    }
    for (prop : wd2wbMappings.keySet()) {
      if (prop == wd2wbMappings.get(prop)) {
        println "#Warning: $prop is not set in the Wikibase"
      }
    }
  }

  return wd2wbMappings;
}

propertiesOfInterest = new HashSet<String>()
propertiesOfInterest.add("P496") // ORCID id
// propertiesOfInterest.add("P248") // stated in

mappings = populateMappings(propertiesOfInterest, null)
println mappings

