package util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.io.StringReader;

public class AnalyzerUtils {
    public static void displayTokens(Analyzer analyzer, String text) throws IOException {
        displayTokens(analyzer.tokenStream("field", new StringReader(text)));
    }

    private static void displayTokens(TokenStream stream) throws IOException {
        CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
        while (stream.incrementToken()) {
            System.out.print("[" + term.buffer() + "] ");
        }
        stream.close();
    }

    public static void displayTokensWithFullDetails(Analyzer analyzer, String text) throws IOException {
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
        CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncr = stream.addAttribute(PositionIncrementAttribute.class);
        OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);
        TypeAttribute type = stream.addAttribute(TypeAttribute.class);

        int position = 0;

        while (stream.incrementToken()) {
            int increment = posIncr.getPositionIncrement();

            if (increment > 0) {
                position = position + increment;
                System.out.println();
                System.out.print(position + ": ");
            }

            System.out.print("[" +
                    term.buffer() + ":" +
                    offset.startOffset() + "->" +
                    offset.endOffset() + ":" +
                    type.type() + "] ");
        }

        System.out.println();
    }

    public static void displayTokensWithPositions(Analyzer analyzer, String text) throws IOException {
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
        CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncr = stream.addAttribute(PositionIncrementAttribute.class);

        int position = 0;
        while(stream.incrementToken()) {
            int increment = posIncr.getPositionIncrement();
            if(increment > 0) {
                position = position + increment;
                System.out.println();
                System.out.print(position + ": ");
            }

            System.out.print("[" + term.buffer() + "] ");
        }
        System.out.println();
    }

    public static void assertAnalyzesTo(Analyzer analyzer, String input, String[] output) throws IOException {
        TokenStream stream = analyzer.tokenStream("field", new StringReader(input));

        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);

        for (String expected : output) {
            assert stream.incrementToken();
            assert expected.equals(termAttr.buffer());
        }

        assert !stream.incrementToken();
        stream.close();
    }
}
