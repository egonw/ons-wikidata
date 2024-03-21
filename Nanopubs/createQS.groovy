// Copyright (C) 2021-2023  Egon Willighagen
// License: MIT
// If you use this software, please check the CITATION.cff file 
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.5.2')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.5.2')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

String.metaClass.encodeURL = {
   java.net.URLEncoder.encode(delegate, "UTF-8")
}

sparql = """
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix np: <http://www.nanopub.org/nschema#>
prefix npa: <http://purl.org/nanopub/admin/>
prefix npx: <http://purl.org/nanopub/x/>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
prefix dct: <http://purl.org/dc/terms/>

select ?np ?subj ?citationrel ?obj ?date where {
  graph npa:graph {
    ?np npa:hasValidSignatureForPublicKey ?pubkey .
    ?np dct:created ?date .
    ?np np:hasAssertion ?assertion .
    optional { ?np rdfs:label ?label . }
    filter not exists { ?npx npx:invalidates ?np ; npa:hasValidSignatureForPublicKey ?pubkey . }
    filter not exists { ?np npx:hasNanopubType npx:ExampleNanopub . }
  }
  graph ?assertion {
    ?subj ?citationrel ?obj .
    filter(regex(str(?citationrel), "^http://purl.org/spar/cito/.*\$"))
    filter(regex(str(?subj), "doi.org/10"))
    filter(regex(str(?obj), "doi.org/10"))
  }
}
"""

if (bioclipse.isOnline()) {
  rawResults = bioclipse.download(
    "https://query.np.trustyuri.net/repo/type/2c1cce3f3152738c1009d59251409392aaaa3b0324bcb5fdfb4b7b944b8f0c18?query=" + sparql.encodeURL(),
    "application/sparql-results+xml"
  )
  results = rdf.processSPARQLXML(rawResults.getBytes(), sparql)
}

for (row in 1..results.getRowCount()) {
  npid = results.get(row, "np")
  citingDOI = "10." + results.get(row, "subj").split("10\\.")[1]
  intent = results.get(row, "citationrel").replace("http://purl.org/spar/cito/", "")
  citedDOI = "10." + results.get(row, "obj").split("10\\.")[1]
  date = results.get(row, "date")
  println "${citingDOI} ${intent} ${citedDOI}"
}
