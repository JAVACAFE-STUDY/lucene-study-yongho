import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.io.IOException;

public class TestIndexer {
    @Test
    public void testIndexer() throws IOException {
        Directory dir = new RAMDirectory();
        IndexWriterConfig c = new IndexWriterConfig(Version.LATEST, new StandardAnalyzer(Version.LATEST));
        IndexWriter writer = new IndexWriter(dir, c);

        Document doc = new Document();
        doc.add(new Field("field", "This is test message.", TextField.TYPE_STORED));
        writer.addDocument(doc);
        writer.close();

        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        TermQuery query = new TermQuery(new Term("field", "test"));
        ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = searcher.doc(hits[i].doc);
            System.out.println(hitDoc.get("field"));
        }

        reader.close();

        dir.close();
    }
}
