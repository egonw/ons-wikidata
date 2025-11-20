// Copyright (C) 2021-2024  Egon Willighagen
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
//   Alternatively, use the -l option to point to a file with a list of DOIs, or the -a option
//   for a list with all works by the given author. The output is the
//   the same. The -h option gives additional help about these and other options.
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://quickstatements.toolforge.org/
//
//   If you used this script, please cite this repository and/or doi:10.21105/joss.02558

// Bacting config
// @Grab(group='org.openscience.cdk', module='cdk-silent', version='2.11')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.5')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.5')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.wikidata', version='1.0.5')

import groovy.cli.commons.CliBuilder
import java.util.stream.Collectors
import java.io.IOException

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
wikidata = new net.bioclipse.managers.WikidataManager(workspaceRoot);

import java.text.SimpleDateFormat;
import java.util.Date;

def cli = new CliBuilder(usage: 'quickstatements.groovy')
cli.h(longOpt: 'help', 'print this message')
cli.t(longOpt: 'token', args:1, argName:'token', 'OpenCitations Access Token')
cli.l(longOpt: 'list', args:1, argName:'list', 'name of a file with a list of DOI of the cited/citing articles')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
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

optionCount = 0
if (options.list) optionCount++

if (optionCount > 1) {
  println("Error: -a, -d, -v, -s, -e, and -l cannot be used at the same time")
  System.exit(-1)
} else if (optionCount < 1) {
  println("Error: Either -a, -d, -v, -s, -e, or -l must be given")
  System.exit(-1)
}

doisToProcess = new ArrayList<String>();

if (options.list) {
  doiFile = new File(options.l)
  if (!doiFile.exists()) {
    println("Error: File ${doiFile} does not exist")
    System.exit(-1)
  }
  doisToProcess = (doiFile as List)
}

missingDOIs = new java.util.HashSet()
println "# Processing ${doisToProcess.size()} DOIs"
doisToProcess.each { doiToProcess ->
  citingDOIs = new ArrayList()
  ociURL = new URL("https://opencitations.net/index/api/v1/citations/${doiToProcess}")
  println "# Fetching ${doiToProcess} from ${ociURL} ..."
  try {
    data = new groovy.json.JsonSlurper().parseText(ociURL.text)
    data.each { citation -> citingDOIs.add(citation.citing.toUpperCase()) }
  } catch (IOException exception) {
    println("# HTTP error: ${exception.message}")
  }
  println "${doiToProcess}\t${citingDOIs.size()}"
}
