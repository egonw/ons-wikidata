// Copyright (C) 2021-2023  Egon Willighagen
// License: MIT
// If you use this software, please check the CITATION.cff file 
//
// OpenCitations Access token: get an access token at https://opencitations.net/accesstoken
//
// Usage:
//
//   Give it a DOI and it will fetch citations to that article from OpenCitations to other articles
//   and match this up with Wikidata for "cites" statements. In the following command, replace
//   'token' with your personal token.
//
//   > groovy quickstatements.groovy -t token -d 10.1021/ACS.JCIM.0C01299 > output.qs
//
//   Alternatively, use the -l option to point to a file with a list of DOIs. The output is the
//   the same. The -h option gives additional help about these and other options.
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/
//
//   If you used this script, please cite this repository and/or doi:10.21105/joss.02558

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.0')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.0')

import groovy.cli.commons.CliBuilder
import java.util.stream.Collectors

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

import java.text.SimpleDateFormat;
import java.util.Date;

def cli = new CliBuilder(usage: 'quickstatements.groovy')
cli.h(longOpt: 'help', 'print this message')
cli.t(longOpt: 'token', args:1, argName:'token', 'OpenCitations Access Token')
cli.d(longOpt: 'doi', args:1, argName:'doi', 'DOI of the cited/citing article')
cli.l(longOpt: 'list', args:1, argName:'list', 'name of a file with a list of DOI of the cited/citing article')
cli.i(longOpt: 'incoming-only', 'Return only citations to the articles with the DOIs')
cli.o(longOpt: 'outgoing-only', 'Return only references in the articles with the DOIs')
cli.r(longOpt: 'report', args:1, argName:'report', 'Report citing or cited DOIs (excluding preprints) not found in Wikidata yet in the given file')
cli.R(longOpt: 'report-preprints-too', args:1, argName:'report', 'Report citing or cited DOIs not found in Wikidata yet in the given file')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
  System.exit(0)
}

if (options.r && options.R) {
  println("Error: -r and -R cannot be used at the same time")
  System.exit(0)
}

httpParams = new HashMap();
if (!options.token) {
  println("Error: An OpenCitations Access Token must be given. See https://opencitations.net/accesstoken")
  System.exit(-1)
} else {
  httpParams.put("authorization", options.d)
}

token = options.t

if (options.doi && options.list) {
  println("Error: -d and -l cannot be used at the same time")
  System.exit(-1)
}

if (!options.doi && !options.list) {
  println("Error: Either -d or -l must be given")
  System.exit(-1)
}

doisToProcess = new ArrayList<String>();

if (options.doi) {
  doisToProcess.add(options.d.toUpperCase())
}

if (options.list) {
  doiFile = new File(options.l)
  if (!doiFile.exists()) {
    println("Error: File ${doiFile} does not exist")
    System.exit(-1)
  }
  doisToProcess = (doiFile as List)
}

println "qid,P2860,S248,s854,s813"

if (options.i && options.o) {
  println("Error: The options -i and -o cannot be combined.")
  System.exit(-1)
}
if (options.o) println "# Only reporting cited articles"
if (options.i) println "# Only reporting citing articles"

if (options.report) println "# Reporting missing DOIs in the file ${options.report}"

missingDOIs = new java.util.HashSet()
doisToProcess.each { doiToProcess ->
  sleep(250)
  doiToProcess = doiToProcess.toUpperCase()
  String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

  citedDOIs = new java.util.HashSet()
  if (!options.i) {
    oci2URL = new URL("https://opencitations.net/index/api/v1/references/${doiToProcess}")
    println "# Fetching ${doiToProcess} from ${oci2URL} ..."
    data2 = new groovy.json.JsonSlurper().parseText(oci2URL.text)
    data2.each { citation -> citedDOIs.add(citation.cited.toUpperCase()) }
    println("# Found cited DOIs for ${doiToProcess}: ${citedDOIs.size()}")

    // cited papers
    if (citedDOIs.size() <= 10000) {
      values = "\"${doiToProcess}\" \n" // we also need a QID for the citing article
      citedDOIs.each { doi ->
        values += "\"${doi.toUpperCase()}\" \n"
      }

      // find QIDs for articles citing the focus article, but not if they already cite it in Wikidata (MINUS clause)
      sparql = "SELECT DISTINCT ?work ?doi WHERE {\n VALUES ?doi {\n ${values} }\n ?work wdt:P356 ?doi . MINUS { ?citingWork wdt:P356 \"${doiToProcess}\" ; wdt:P2860 ?work }\n}"
      if (bioclipse.isOnline()) {
        rawResults = bioclipse.sparqlRemote(
          "https://query.wikidata.org/sparql", sparql
        )
        results = rdf.processSPARQLXML(rawResults, sparql)
      }

      // make a map
      map = new HashMap<String,String>()
      for (i=1;i<=results.rowCount;i++) {
        rowVals = results.getRow(i)
        map.put(rowVals[1], rowVals[0].replace("http://www.wikidata.org/entity/",""))
      }

      citingQID = map.get(doiToProcess)
      println "# cited articles for ${doiToProcess}"
      map.each { citedDOI, qid ->
        safeOciURL = oci2URL.toString().replaceAll("<","%3C").replaceAll(">", "%3E")
        if (citedDOI != doiToProcess) println "${citingQID},${qid},Q107507940,\"\"\"${safeOciURL}\"\"\",+${date}T00:00:00Z/11"
      }

      if (options.report) {
        // report all the DOIs that are not in Wikidata
        sparql = "SELECT DISTINCT ?work ?doi WHERE {\n VALUES ?doi {\n ${values} }\n ?work wdt:P356 ?doi .\n}"
        if (bioclipse.isOnline()) {
          rawResults = bioclipse.sparqlRemote("https://query.wikidata.org/sparql", sparql  )
          results = rdf.processSPARQLXML(rawResults, sparql)
        }
        for (i=1;i<=results.rowCount;i++) {
          rowVals = results.getRow(i)
          citedDOIs.remove(rowVals[1])
        }
        println "# DOIs citing ${doiToProcess} that are not in Wikidata: ${citedDOIs.size()}"
        missingDOIs.addAll(citedDOIs)
      }
    } else {
      println "# Too many cited articles. Skipping"
    }
  }
  
  citingDOIs = new ArrayList()
  if (!options.o) {
    ociURL = new URL("https://opencitations.net/index/api/v1/citations/${doiToProcess}")
    println "# Fetching ${doiToProcess} from ${ociURL} ..."
    data = new groovy.json.JsonSlurper().parseText(ociURL.text)
    data.each { citation -> citingDOIs.add(citation.citing.toUpperCase()) }
    println "# Found citing DOIs for ${doiToProcess}: ${citingDOIs.size()}"

    // citing papers
    if (citingDOIs.size() <= 5000) {
      // okay, that should work
    } else {
      println "# Too many citing articles. Taking 5000 random"
      Collections.shuffle(citingDOIs);
      citingDOIs = citingDOIs.stream().limit(5000).collect(Collectors.toList())
      println "# Found citing DOIs for ${doiToProcess}: ${citingDOIs.size()}"
    }

    {
      // find QIDs for articles citing the focus article, but not if they already cite it in Wikidata (MINUS clause)
      values = "\"${doiToProcess}\" \n" // we also need a QID for the cited article
      citingDOIs.each { doi ->
        values += "\"${doi.toUpperCase()}\" \n"
      }
      sparql = "SELECT DISTINCT ?work ?doi WHERE {\n VALUES ?doi {\n ${values} }\n ?work wdt:P356 ?doi . MINUS { ?work wdt:P2860/wdt:P356 \"${doiToProcess}\" }\n}"
      if (bioclipse.isOnline()) {
        rawResults = bioclipse.sparqlRemote("https://query.wikidata.org/sparql", sparql  )
        results = rdf.processSPARQLXML(rawResults, sparql)
      }
      // make a map
      map = new HashMap<String,String>()
      for (i=1;i<=results.rowCount;i++) {
        rowVals = results.getRow(i)
        map.put(rowVals[1], rowVals[0].replace("http://www.wikidata.org/entity/",""))
      }

      citedQID = map.get(doiToProcess)
      println "# citing articles for ${doiToProcess}"
      map.each { citingDOI, qid ->
        safeOciURL = ociURL.toString().replaceAll("<","%3C").replaceAll(">", "%3E")
        if (citingDOI != doiToProcess && citedQID != null && qid != null) println "${qid},${citedQID},Q107507940,\"\"\"${safeOciURL}\"\"\",+${date}T00:00:00Z/11"
      }

      if (options.report) {
        // report all the DOIs that are not in Wikidata
        sparql = "SELECT DISTINCT ?work ?doi WHERE {\n VALUES ?doi {\n ${values} }\n ?work wdt:P356 ?doi .\n}"
        if (bioclipse.isOnline()) {
          rawResults = bioclipse.sparqlRemote("https://query.wikidata.org/sparql", sparql  )
          results = rdf.processSPARQLXML(rawResults, sparql)
        }
        for (i=1;i<=results.rowCount;i++) {
          rowVals = results.getRow(i)
          citingDOIs.remove(rowVals[1])
        }
        println "# DOIs citing ${doiToProcess} that are not in Wikidata: ${citingDOIs.size()}"
        missingDOIs.addAll(citingDOIs)
      }
    }
  }

}

if (options.r || options.R) {
  // report missing DOIs
  reportFile = options.r ? options.r : options.R
  new File(reportFile).withWriter { out ->
    missingDOIs.each {
      if (options.r) {
        // check if the DOI is from a (recognized) preprint server
        if (!(
          it.startsWith("10.1101") || // bioRxviv
          it.startsWith("10.21203"))) // ResearchSquare
          out.println it
      } else {
        out.println it
      }
    }
  }
}
