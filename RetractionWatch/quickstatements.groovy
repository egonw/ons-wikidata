// Copyright (C) 2023  Egon Willighagen
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
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.5.0-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.5.0-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.wikidata', version='0.5.0-SNAPSHOT')
@Grab(group='org.apache.commons', module='commons-csv', version='1.10.0')

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

wikidataQIDs = wikidata.getEntityIDsForDOIs(dois.asList())

wikidataQIDs.each { doi, qid ->
  retractedArticle = retractedArticles.get(doi)
  if (retractedArticle != null) {
    if (doi == retractedArticle.doi) {
      retractedArticle.qid = qid.substring(31)
    } else if (doi == retractedArticle.noticeDoi) {
      retractedArticle.noticeQid = qid.substring(31)
    }
  }
}

knownRetracted = wikidata.getEntityIDsForType("Q45182324").stream().map(element -> element.substring(31)).toList()
knownNotices = wikidata.getEntityIDsForType("Q7316896").stream().map(element -> element.substring(31)).toList()

retractedArticles.each { doi, retractedArticle ->
  source = "\tS248\tQ17078233\tS813\t+${date}T00:00:00Z/11"
  // extend the sources
  if (retractedArticle.noticeQid == null)
    source += "\tS854\t\"https://doi.org/${retractedArticle.noticeDoi}\""
  if (retractedArticle.urls != null) {
    retractedArticle.urls.split(';').each { url ->
      try {
        new URL(url) // test if it indeed is an URL
        source += "\tS854\t\"${url}\""
      } catch (Exception exception) {} // just skip if not
    }
  }
  // output the QS
  if (retractedArticle.qid != null && !knownRetracted.contains(retractedArticle.qid)) {
    println "\t${retractedArticle.qid}\tP31\tQ45182324${source}"
    if (retractedArticle.noticeQid != null)
      println "\t${retractedArticle.qid}\tP5824\t${retractedArticle.noticeQid}${source}"
  }
  if (retractedArticle.noticeQid != null && !knownNotices.contains(retractedArticle.noticeQid))
    println "\t${retractedArticle.noticeQid}\tP31\tQ7316896${source}"
}
