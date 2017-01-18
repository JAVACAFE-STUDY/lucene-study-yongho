import org.apache.lucene.analysis.CannedTokenStream;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LuceneTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class TestCheckIndex extends LuceneTestCase {

    public void testDeletedDocs() throws IOException {
        Directory dir = newDirectory(); // RAMDirectory 생성
        IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(new MockAnalyzer(random()))
                .setMaxBufferedDocs(2));
        for (int i = 0; i < 19; i++) {
            Document doc = new Document();
            FieldType customType = new FieldType(TextField.TYPE_STORED);
            customType.setStoreTermVectors(true);
            customType.setStoreTermVectorPositions(true);
            customType.setStoreTermVectorOffsets(true);
            doc.add(newField("field", "aaa" + i, customType));
            writer.addDocument(doc);
        }
        writer.forceMerge(1);   // 인자로 전달된 maxNumSegments 이하의 개수로 segment를 병합.
        writer.commit();    // commit을 수행하면 segment 병합이 적용됨.
        writer.deleteDocuments(new Term("field", "aaa5"));  // aaa5 Document 제거
        writer.close(); // close 시 제거한 Document가 적용됨

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        CheckIndex checker = new CheckIndex(dir);
        checker.setInfoStream(new PrintStream(bos, false, IOUtils.UTF_8));
        if (VERBOSE) checker.setInfoStream(System.out);
        CheckIndex.Status indexStatus = checker.checkIndex();
        if (indexStatus.clean == false) {
            System.out.println("CheckIndex failed");
            System.out.println(bos.toString(IOUtils.UTF_8));
            fail();
        }

        final CheckIndex.Status.SegmentInfoStatus seg = indexStatus.segmentInfos.get(0);
        assertTrue(seg.openReaderPassed);

        assertNotNull(seg.diagnostics);

        assertNotNull(seg.fieldNormStatus);
        assertNull(seg.fieldNormStatus.error);
        assertEquals(1, seg.fieldNormStatus.totFields);

        assertNotNull(seg.termIndexStatus);
        assertNull(seg.termIndexStatus.error);
        assertEquals(18, seg.termIndexStatus.termCount);
        assertEquals(18, seg.termIndexStatus.totFreq);
        assertEquals(18, seg.termIndexStatus.totPos);

        assertNotNull(seg.storedFieldStatus);
        assertNull(seg.storedFieldStatus.error);
        assertEquals(18, seg.storedFieldStatus.docCount);
        assertEquals(18, seg.storedFieldStatus.totFields);

        assertNotNull(seg.termVectorStatus);
        assertNull(seg.termVectorStatus.error);
        assertEquals(18, seg.termVectorStatus.docCount);
        assertEquals(18, seg.termVectorStatus.totVectors);

        assertTrue(seg.diagnostics.size() > 0);
        final List<String> onlySegments = new ArrayList<>();
        onlySegments.add("_0");

        assertTrue(checker.checkIndex(onlySegments).clean == true);
        dir.close();
    }

    // LUCENE-4221: we have to let these thru, for now
    public void testBogusTermVectors() throws IOException {
        Directory dir = newDirectory();
        IndexWriter iw = new IndexWriter(dir, newIndexWriterConfig(null));
        Document doc = new Document();
        FieldType ft = new FieldType(TextField.TYPE_NOT_STORED);
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorOffsets(true);
        Field field = new Field("foo", "", ft);
        field.setTokenStream(new CannedTokenStream(
                new Token("bar", 5, 10), new Token("bar", 1, 4)
        ));
        doc.add(field);
        iw.addDocument(doc);
        iw.close();
        dir.close(); // checkindex
    }
}
