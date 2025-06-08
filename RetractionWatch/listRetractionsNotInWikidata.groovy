// Copyright (C) 2023-2025  Egon Willighagen
// License: MIT
// If you use this software, please check the CITATION.cff file 

// Using the output of the CrossRef API, see https://www.crossref.org/blog/news-crossref-and-retraction-watch/
//
// the API call https://api.labs.crossref.org/data/retractionwatch?ginny@crossref.org gives files like this:
//
// Record ID,Title,Subject,Institution,Journal,Publisher,Country,Author,URLS,ArticleType,RetractionDate,RetractionDOI,RetractionPubMedID,OriginalPaperDate,OriginalPaperDOI,OriginalPaperPubMedID,RetractionNature,Reason,Paywalled,Notes
// 50438,Construction of Mental Health Education Model Based on Computer Multimedia Group Psychological Measurement,(B/T) Technology;(SOC) Education;(SOC) Psychology;,"Education Department, Taiyuan Normal University, Taiyuan, Shanxi, China;",Advances in Multimedia,Hindawi,China,Haiyan Zhang,https://retractionwatch.com/2022/09/28/exclusive-hindawi-and-wiley-to-retract-over-500-papers-linked-to-peer-review-rings/;https://retractionwatch.com/2023/04/05/wiley-and-hindawi-to-retract-1200-more-papers-for-compromised-peer-review/,Research Article;,8/16/2023 0:00,10.1155/2023/9804945,0,9/24/2021 0:00,10.1155/2021/6907871,0,Retraction,+Concerns/Issues About Data;+Concerns/Issues About Results;+Concerns/Issues about Referencing/Attributions;+Concerns/Issues with Peer Review;+Investigation by Journal/Publisher;+Investigation by Third Party;+Randomly Generated Content;+Unreliable Results;,No,See also: https://pubpeer.com/publications/7309DC051DD46825091B5F5FC6B0E9
// ...


// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.6-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.6-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.wikidata', version='1.0.6-SNAPSHOT')

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
wikidata = new net.bioclipse.managers.WikidataManager(workspaceRoot);

import java.text.SimpleDateFormat;
import java.util.Date;

String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

class RetractedArticle {
  String id;
  String qid;
  String doi;
  String noticeDoi;
  String noticeQid;
  String urls;
}

format = CSVFormat.RFC4180.builder().setHeader().build()
Iterable<CSVRecord> records = format.parse(
  new FileReader(new File("retractions.csv"))
)
dois = new HashSet<String>()
retractedArticles = new HashMap<String,RetractedArticle>();
for (CSVRecord record : records) {
  if ("Retraction".equals(record.get("RetractionNature"))) {
    retracted = new RetractedArticle();
    doi = record.get("OriginalPaperDOI")
    retracted.id = record.get("Record ID")
    retracted.doi = doi
    retractionDOI = record.get("RetractionDOI")
    retracted.noticeDoi = retractionDOI
    dois.add(doi)
    dois.add(retractionDOI)
    if (record.get("URLS").length() > 0)
      retracted.urls = record.get("URLS")
    retractedArticles.put(doi, retracted)
  }
}

println "# looking up Wikidata items for the DOIs"
wikidataQIDs = wikidata.getEntityIDsForDOIs(dois.asList())

wikidataQIDs.each { doi, qid ->
  retractedArticle = retractedArticles.get(doi)
  if (retractedArticle != null) {
    if (doi == retractedArticle.doi) {
      retractedArticle.qid = qid.substring(31)
    }
  }
}

println "# Listing DOIs without an Wikidata item"
dois.each { doi ->
  retractedArticle = retractedArticles.get(doi)
  if (retractedArticle != null && retractedArticle.qid == null) {
    println doi
  }
}
